package ru.ifmo.team.filenetwork.client;

import ru.ifmo.team.filenetwork.Message;
import ru.ifmo.team.filenetwork.MessagingException;
import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.actions.*;
import ru.ifmo.team.fileprotocol.FileProtocolType;
import ru.ifmo.team.util.ExceptionExpander;
import ru.ifmo.team.util.KeyGen;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;
import ru.ifmo.team.util.tcp.client.ClientException;
import ru.ifmo.team.util.tcp.client.IClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * User: Daniel Penkin
 * Date: May 9, 2009
 * Version: 1.0
 */
public class FileClient implements IFileClient {

    private static final int DEF_PART_SIZE = 10240;
    private static final int TRIES = 10;

    private final Map<SharedFile, File> sharedLocalFiles = new LinkedHashMap<SharedFile, File>();
    private final Set<File> lockedFiles = new HashSet<File>();
    private final Map<String, SharedFile> sharedForeignFiles = new LinkedHashMap<String, SharedFile>();
    private final Set<IFileWatcher> fileWatchers = new HashSet<IFileWatcher>();
    private final Map<String, Download> downloads = new HashMap<String, Download>();
    private final PrefixLogger logger;

    private final int partSize;
    private final String clientID;
    private final String host;
    private final int port;
    private final IClient tcpClient;

    private boolean shutdown = true;

    public FileClient(IClient tcpClient, String host, int port, int partSize, Logger logger) {
        this.tcpClient = tcpClient;
        this.host = host;
        this.port = port;
        if (partSize <= 100) {
            this.partSize = DEF_PART_SIZE;
        } else {
            this.partSize = partSize;
        }
        this.logger = new PrefixLogger("CL", logger);
        this.clientID = KeyGen.generate(Message.SID_LENGTH / 2);
    }

    public void start() {
        if (!reconnect(TRIES)) {
            fireConnectionClosed();
        } else {
            try {
                sendFirstMessage();
            } catch (ClientException ignored) {
            }
        }
    }


    public void registerFileListener(IFileWatcher watcher) {
        fileWatchers.add(watcher);
    }

    public boolean addLocalFiles(Map<File, String> files) {
        Map<SharedFile, File> added = new HashMap<SharedFile, File>();
        for (Map.Entry<File, String> fileDesc : files.entrySet()) {
            File file = fileDesc.getKey();
            if (!lockedFiles.contains(file)) {
                if (file.length() > Integer.MAX_VALUE) {
                    logger.log("Unable to add file: file too big");
                    return false;
                }
                long checksum;
                try {
                    checksum = calculateChecksum(file);
                } catch (IOException e) {
                    logger.log("Unable to calculate checksum: " + ExceptionExpander.expandException(e));
                    return false;
                }
                //TODO lock file
                SharedFile f;
                if ((f = sharedForeignFiles.get(checksum + "")) != null) {
                    logger.log("File " + file.getName() + " is already shared, adding to local list");
                    added.put(f, file);
                } else {
                    long size = file.length();
                    added.put(new SharedFile(fileDesc.getKey().getName(),
                            fileDesc.getValue(), size, checksum + ""), file);
                }
            } else {
                logger.log("File " + file.getName() + " is already locally shared");
            }
        }
        if (added.size() > 0) {
            sharedLocalFiles.putAll(added);
            fireListUpdated();
            AddAction action = new AddAction(added.keySet());
            Message request = new Message(action, FileProtocolType.Direction.CS_RQ, clientID, null);
            try {
                tcpClient.sendMessage(request.encodeMessage());
                return true;
            } catch (ClientException e) {
                logger.log("Unable to add files: " + ExceptionExpander.expandException(e));
                return false;
            }
        }
        return false;
    }

    private long calculateChecksum(File file) throws IOException {
        logger.log("Calculating checksum for file " + file.getName() + " ...");
        CheckedInputStream cis = new CheckedInputStream(
                new FileInputStream(file), new CRC32());
        byte[] buf = new byte[128];
        while (cis.read(buf) >= 0) {
        }
        return cis.getChecksum().getValue();
    }

    public boolean removeLocalFiles(Set<SharedFile> files) {
        Set<SharedFile> removed = new HashSet<SharedFile>();
        for (SharedFile file : files) {
            synchronized (sharedLocalFiles) {
                File lockedFile = sharedLocalFiles.remove(file);
                if (lockedFile != null) {
                    removed.add(file);
                    lockedFiles.remove(lockedFile);
                }
            }
            //TODO unlock file
        }
        logger.log("Removing " + removed.size() + " files " + removed.toString());
        RemoveAction action = new RemoveAction(removed);
        Message request = new Message(action, FileProtocolType.Direction.CS_RQ, clientID, null);
        try {
            tcpClient.sendMessage(request.encodeMessage());
            return true;
        } catch (ClientException e) {
            logger.log("Unable to remove files: " + ExceptionExpander.expandException(e));
            return false;
        }
    }

    public Set<SharedFile> getSharedFilesSet() {
        return new LinkedHashSet<SharedFile>(sharedForeignFiles.values());
    }

    public Set<SharedFile> getLocalSharedFileSet() {
        return Collections.unmodifiableSet(sharedLocalFiles.keySet());
    }

    public void fireListUpdated() {
        for (IFileWatcher watcher : fileWatchers) {
            watcher.fileListUpdated(getLocalSharedFileSet(), getSharedFilesSet());
        }
    }

    public String downloadFile(SharedFile sharedFile, File file) {
        if (sharedLocalFiles.containsKey(sharedFile)) {
            return null;
        }
        String sid = KeyGen.generate(Message.SID_LENGTH);
        Download download = new Download(file, sid, this, logger.getBaseLogger());
        if (!download.canWrite()) {
            return null;
        }
        GetAction action = new GetAction(sharedFile, 0, 0);
        Message request = new Message(action, FileProtocolType.Direction.CS_RQ, clientID, sid, null);
        try {
            downloads.put(sid, download);
            tcpClient.sendMessage(request.encodeMessage());
            return sid;
        } catch (ClientException e) {
            logger.log("Unable to start download: " + ExceptionExpander.expandException(e));
            return null;
        }
    }

    /*package*/ void downloadCompleted(String sid) {
        logger.log("Completing donwload " + sid);
        Download completed = downloads.get(sid);
        for (IFileWatcher watcher : fileWatchers) {
            watcher.downloadCompleted(sid, completed.getFile());
        }
        removeDownload(sid);
    }

    private void removeDownload(String sid) {
        downloads.remove(sid);
    }

    private void updateForeignSharedFiles(Set<SharedFile> added, Set<SharedFile> removed) {
        boolean updated = false;
        if (added != null) {
            for (SharedFile file : added) {
                SharedFile foreign = sharedForeignFiles.get(file.getHash());
                if (foreign == null) {
                    sharedForeignFiles.put(file.getHash(), file);
                    logger.log("File " + file + " added");
                    updated = true;
                } else {
                    logger.log("File " + foreign + " skipped (is already shared)");
                }
            }
        }
        if (removed != null) {
            for (SharedFile file : removed) {
                SharedFile foreign = sharedForeignFiles.remove(file.getHash());
                if (foreign != null) {
                    logger.log("File " + foreign + " removed");
                    updated = true;
                }
            }
        }
        if (updated) {
            fireListUpdated();
        }
    }

    public void acceptMessage(String ip, String message) {
        if (message == null) {
            return;
        }
        Message msg = null;
        try {
            msg = Message.decodeMessage(message);
        } catch (MessagingException e) {
            logger.log("Unable to decode message, closing connection: " + ExceptionExpander.expandException(e));
            shutDown();
        }
        if (msg != null) {
            String id = msg.getClientID();
            if (!clientID.equals(id)) {
                logger.log("Identification failed: \"" + id + "\" received but\"" + clientID + "\"expected");
                shutDown();
            }
            FileProtocolType.Direction.Enum dir = msg.getDirection();
            if (dir == FileProtocolType.Direction.SC_RQ) {
                logger.log("Processing Server->Client request");
                Action action = msg.getAction();
                if (action != null) {
                    switch (action.getActionType()) {
                        case ADD:
                            AddAction addAction = (AddAction) action;
                            logger.log("Adding files: " + addAction.getFiles());
                            updateForeignSharedFiles(addAction.getFiles(), null);
                            break;
                        case REMOVE:
                            RemoveAction removeAction = (RemoveAction) action;
                            logger.log("Removing files: " + removeAction.getFiles());
                            updateForeignSharedFiles(null, removeAction.getFiles());
                            break;
                        case GET:
                            GetAction getAction = (GetAction) action;
                            SharedFile rq = getAction.getFile();
                            File local = null;
                            synchronized (sharedLocalFiles) {
                                for (SharedFile file : sharedLocalFiles.keySet()) {
                                    if (file.getHash().equals(rq.getHash())) {
                                        local = sharedLocalFiles.get(file);
                                        break;
                                    }
                                }
                            }
                            if (local != null) {
                                logger.log("Received request of file " + getAction.getFile().getName() +
                                        " [stored on disk: " + local.getPath() + "]");
                                new Thread(new FileSender(getAction.getFile(), local, msg.getSID(),
                                        getAction.getFromPart(), getAction.getTillPart())
                                ).start();
                            } else {
                                logger.log("Received request of unrecognized file");
                            }
                            break;
                        default:
                            logger.log("Unrecognized message type, closing connection");
                            shutDown();
                    }
                }
            } else if (dir == FileProtocolType.Direction.SC_RS) {
                logger.log("Processing Server->Client response");
                Action action = msg.getAction();
                if (action != null) {
                    switch (action.getActionType()) {
                        case TRANSFER:
                            Download download = downloads.get(msg.getSID());
                            if (download != null) {
                                TransferAction transfer = (TransferAction) action;
                                logger.log("Received part " + transfer.getPartNumber() +
                                        " of " + transfer.getPartsTotal() +
                                        " of " + transfer.getFile().getName());
                                try {
                                    download.processPart(transfer.getPartNumber(),
                                            transfer.getPartsTotal(), transfer.getData());
                                } catch (IOException e) {
                                    logger.log("Exception on processing part, removing download: " +
                                            ExceptionExpander.expandException(e));
                                    removeDownload(msg.getSID());
                                }
                            } else {
                                logger.log("Received unrecognized part: " + msg.getSID());
                            }
                            break;
                        default:
                            logger.log("Unrecognized message type, closing connection");
                            shutDown();
                    }
                }
            }
        }
    }

    private void shutDown() {
        shutdown = true;
        if (tcpClient.isStarted()) {
            tcpClient.stop();
        }
    }

    public void connectionClosed(String ip) {
        logger.log("Connection was closed, reconnecting");
        reconnect(TRIES);
        try {
            sendFirstMessage();
        } catch (ClientException ignored) {
        }
    }

    public void fireConnectionClosed() {
        shutDown();
        for (IFileWatcher watcher : fileWatchers) {
            watcher.connectionClosed();
        }
    }

    public boolean reconnect(int tries) {
        if (tcpClient.isStarted()) {
            logger.log("Stopping working TCP client");
            tcpClient.stop();
            logger.log("Trying to reconnect for " + TRIES + " times");

        }
        for (int i = 0; i < tries; i++) {
            logger.log("Connect attempt #" + i + "...");
            if (tcpClient.start(this, host, port)) {
                shutdown = false;
                logger.log("Connected successfully");
                return true;
            }
            logger.log("Connection attepmpt failed");
        }
        logger.log("Unable to reconnect after " + TRIES + " tries");
        return false;
    }

    private void sendFirstMessage() throws ClientException {
        AddAction a = new AddAction(sharedLocalFiles.keySet());
        Message rq = new Message(a, FileProtocolType.Direction.CS_RQ, clientID, null);
        tcpClient.sendMessage(rq.encodeMessage());
    }

    private class FileSender implements Runnable {

        private final SharedFile sharedFile;
        private final String sid;

        private RandomAccessFile raFile;
        private int totalAmnt;
        private int fromPart;
        private int tillPart;

        private FileSender(SharedFile sharedFile, File file, String sid, int fromPart, int tillPart) {
            logger.log("Preparing transfer of file " + file.getName() + "...");
            this.sharedFile = sharedFile;
            try {
                raFile = new RandomAccessFile(file, "r");
                long size = raFile.length();
                totalAmnt = (int) Math.ceil(size / (double) partSize);
                logger.log("File size: " + size + "; part size: " + partSize + "; total part amount: " + totalAmnt);
            } catch (IOException e) {
                logger.log("Transfer of file " + file.getName() + " failed: " + e.getMessage());
                raFile = null;
            }
            this.sid = sid;
            this.fromPart = fromPart;
            this.tillPart = tillPart;
        }

        public void run() {
            if (shutdown) {
                logger.log("File sender stopped");
                return;
            }
            if (raFile == null) {
                return;
            }
            String reason = null;
            if (fromPart < 0) {
                reason = "start-part number is negative";
            } else if (tillPart < 0) {
                reason = "end-part number is negative";
            } else if (fromPart > totalAmnt) {
                reason = "start-part number is greater than total part amount";
            } else if (tillPart > totalAmnt) {
                reason = "end-part number is greater than total part amount";
            } else if (tillPart > fromPart) {
                reason = "end-part number is greater than start-part number";
            }
            if (reason != null) {
                logger.log("Transfer failed: " + reason);
                return;
            }
            if (tillPart == 0) {
                fromPart = 1;
                tillPart = totalAmnt;
            }
            logger.log("Transferring parts from " + fromPart + " till " + tillPart);
            try {
                raFile.seek((fromPart - 1) * (long) partSize);
            } catch (IOException e) {
                logger.log("Transfer failed: " + e.getMessage());
                return;
            }
            System.out.println("From: " + fromPart + "; till: " + tillPart);
            for (int i = fromPart; i <= tillPart; i++) {
                if (shutdown) {
                    logger.log("File sender stopped");
                    return;
                }
                try {
                    logger.log("Sending part " + i + " from " + (tillPart - fromPart + 1) +
                            " of file " + sharedFile.getName());
                    byte[] stor;
                    if (i == totalAmnt) {
                        stor = new byte[(int) (raFile.length() - partSize * (totalAmnt - 1))];
                    } else {
                        stor = new byte[partSize];
                    }
                    raFile.readFully(stor);
                    TransferAction action = new TransferAction(sharedFile, i, totalAmnt, stor.hashCode() + "", stor);
                    Message response = new Message(action, FileProtocolType.Direction.CS_RS, clientID, sid, null);
                    tcpClient.sendMessage(response.encodeMessage());
                } catch (Exception e) {
                    logger.log("Unable to send part " + i + " of file " + sharedFile.getName() + " : " +
                            ExceptionExpander.expandException(e));
                    logger.log("Transfer failed");
                    break;
                }
            }
            try {
                logger.log("Transfer of file " + sharedFile.getName() + " finished");
                raFile.close();
            } catch (IOException ignored) {
            }
        }
    }

}
