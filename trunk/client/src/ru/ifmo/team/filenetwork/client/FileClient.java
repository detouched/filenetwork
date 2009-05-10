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
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * User: Daniel Penkin
 * Date: May 9, 2009
 * Version: 1.0
 */
public class FileClient implements IFileClient {

    private final Map<SharedFile, File> sharedLocalFiles = new LinkedHashMap<SharedFile, File>();
    private final Set<File> lockedFiles = new HashSet<File>();
    private final Set<SharedFile> sharedForeignFiles = new LinkedHashSet<SharedFile>();
    private final Set<IFileWatcher> fileWatchers = new HashSet<IFileWatcher>();
    private final Map<String, Download> downloads = new HashMap<String, Download>();
    private final PrefixLogger logger;

    private final IManager manager;

    private String clientID;
    private IClient tcpClient;

    public FileClient(IManager manager, Logger logger) {
        this.manager = manager;
        this.logger = new PrefixLogger("CL", logger);
    }

    public void registerFileListener(IFileWatcher watcher) {
        fileWatchers.add(watcher);
    }

    public void registerTCPClient(IClient client) {
        tcpClient = client;
    }

    public boolean addLocalFiles(Map<File, String> files) {
        if (clientID == null) {
            return false;
        }
        Map<SharedFile, File> added = new HashMap<SharedFile, File>();
        for (Map.Entry<File, String> fileDesc : files.entrySet()) {
            File file = fileDesc.getKey();
            if (!lockedFiles.contains(file)) {
                long checksum;
                try {
                    checksum = calculateChecksum(file);
                } catch (IOException e) {
                    logger.log("Unable to calculate checksum: " + ExceptionExpander.expandException(e));
                    return false;
                }
                long size = file.length();
                //TODO lock file
                added.put(new SharedFile(fileDesc.getKey().getName(),
                        fileDesc.getValue(), size, checksum + ""), file);
            } else {
                logger.log("File " + file.getName() + " is already shared");
            }
        }
        synchronized (sharedLocalFiles) {
            sharedLocalFiles.putAll(added);
        }
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
        if (clientID == null) {
            return false;
        }
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
        return Collections.unmodifiableSet(sharedForeignFiles);
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
        if (clientID == null) {
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
            tcpClient.sendMessage(request.encodeMessage());
            downloads.put(sid, download);
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
            if (clientID == null) {
                clientID = id;
            } else {
                if (!clientID.equals(id)) {
                    logger.log("Identification failed: \"" + id + "\" received but\"" + clientID + "\"expected");
                    shutDown();
                }
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
                            sharedForeignFiles.addAll(addAction.getFiles());
                            fireListUpdated();
                            break;
                        case REMOVE:
                            RemoveAction removeAction = (RemoveAction) action;
                            logger.log("Removing files: " + removeAction.getFiles());
                            sharedForeignFiles.removeAll(removeAction.getFiles());
                            break;
                        case GET:
                            GetAction getAction = (GetAction) action;
                            File file = sharedLocalFiles.get(getAction.getFile());
                            if (file != null) {
                                logger.log("Received request of file " + getAction.getFile().getName() +
                                        " [stored on disk: " + file.getPath() + "]");
                                new Thread(new FileSender(getAction.getFile(), file, msg.getSID(),
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
        tcpClient.stop();
        manager.connectionClosed();
    }

    public void connectionClosed(String ip) {
        shutDown();
    }

    private class FileSender implements Runnable {

        private final File file;
        private final SharedFile sharedFile;
        private final int fromPart;
        private final int tillPart;
        private final String sid;

        private FileInputStream input;

        private FileSender(SharedFile sharedFile, File file, String sid, int fromPart, int tillPart) {
            this.sharedFile = sharedFile;
            this.file = file;
            this.sid = sid;
            this.fromPart = fromPart;
            this.tillPart = tillPart;
        }

        public void run() {
            try {
                input = new FileInputStream(file);
                StringBuilder sb = new StringBuilder();
                int counter;
                byte[] buf = new byte[1024];
                do {
                    counter = input.read(buf);
                    if (counter > 0) {
                        sb.append(new String(buf, 0, counter));
                    }
                } while (counter > 0);
                TransferAction action = new TransferAction(sharedFile, 1, 1, sharedFile.getHash(), sb.toString());
                Message response = new Message(action, FileProtocolType.Direction.CS_RS, clientID, sid, null);
                tcpClient.sendMessage(response.encodeMessage());
            } catch (Exception e) {
                logger.log("Unable to send part of file " + file.getPath() + ": " + ExceptionExpander.expandException(e));
            } finally {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
