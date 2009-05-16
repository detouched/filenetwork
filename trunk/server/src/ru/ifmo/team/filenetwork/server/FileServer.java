package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.filenetwork.Message;
import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.actions.Action;
import ru.ifmo.team.filenetwork.actions.GetAction;
import ru.ifmo.team.filenetwork.actions.TransferAction;
import ru.ifmo.team.fileprotocol.FileProtocolType;
import ru.ifmo.team.util.IMessageAcceptor;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;
import ru.ifmo.team.util.tcp.server.IConnectionHandler;

import java.util.*;

/**
 * User: Daniel Penkin
 * Date: May 6, 2009
 * Version: 1.0
 */
public class FileServer implements IFileServer {

    private static final String monitor = "monitor";

    private static final Random random = new Random();

    private final Map<String, SharedFile> sharedFiles = new HashMap<String, SharedFile>();        // hash - file
    private final Map<String, Set<String>> fileOwners = new HashMap<String, Set<String>>();       // hash - owners
    private final Map<String, ClientHandler> handlers = new HashMap<String, ClientHandler>();     // CID - handler
    private final Map<String, String> transfers = new HashMap<String, String>();                  // SID - CID
    private final PrefixLogger logger;

    public FileServer(Logger logger) {
        this.logger = new PrefixLogger("FileServer", logger);
    }

    public Set<SharedFile> getFileSet() {
        return new HashSet<SharedFile>(sharedFiles.values());
    }

    public void addFiles(Set<SharedFile> files, String clientOwner) {
        Set<SharedFile> added = new HashSet<SharedFile>();
        synchronized (monitor) {
            for (SharedFile file : files) {
                String hash = file.getHash();
                if (hash != null) {
                    SharedFile local = sharedFiles.get(hash);
                    if (local == null) {
                        sharedFiles.put(hash, file);
                        Set<String> owners = new HashSet<String>();
                        owners.add(clientOwner);
                        this.fileOwners.put(hash, owners);
                        added.add(file);
                        logger.log("File " + file.getName() + " [" + file.getHash() + "] added");
                    } else {
                        fileOwners.get(hash).add(clientOwner);
                    }
                }
            }
        }
        if (added.size() > 0) {
            fireUpdatedFileSet(added, null);
        }
    }

    public void removeFiles(Set<SharedFile> files, String clientOwner) {
        Set<SharedFile> removed = new HashSet<SharedFile>();
        synchronized (monitor) {
            for (SharedFile file : files) {
                String hash = file.getHash();
                if (hash != null) {
                    SharedFile local = sharedFiles.get(hash);
                    if (local != null) {
                        Set<String> owners = fileOwners.get(hash);
                        owners.remove(clientOwner);
                        if (owners.size() == 0) {
                            fileOwners.remove(hash);
                            sharedFiles.remove(hash);
                            removed.add(local);
                            logger.log("File " + local.getName() + " [" + local.getHash() + "] removed");
                        }
                    }
                }
            }
        }
        if (removed.size() > 0) {
            fireUpdatedFileSet(null, removed);
        }
    }

    public IMessageAcceptor clientJoined(String ip, IConnectionHandler connectionHandler) {
        synchronized (handlers) {
            ClientHandler handler = new ClientHandler(this, connectionHandler, logger.getBaseLogger());
            return handler;
        }
    }

    public void registerHandler(String cid, ClientHandler handler) {
        handlers.put(cid, handler);
        handler.fileSetUpdated(getFileSet(), null);
    }

    public void clientLeft(String clientID) {
        synchronized (handlers) {
            handlers.remove(clientID);
        }
        Set<SharedFile> removedFiles = new HashSet<SharedFile>();
        Set<String> removedTransfers = new HashSet<String>();
        synchronized (monitor) {
            for (Map.Entry<String, Set<String>> file : fileOwners.entrySet()) {
                Set<String> owners = file.getValue();
                if (owners.contains(clientID)) {
                    removedFiles.add(sharedFiles.get(file.getKey()));
                }
            }
            removeFiles(removedFiles, clientID);
            for (Map.Entry<String, String> transfer : transfers.entrySet()) {
                if (transfer.getValue().equals(clientID)) {
                    removedTransfers.add(transfer.getKey());
                }
            }
            for (String sid : removedTransfers) {
                String cid = transfers.remove(sid);
                logger.log("Transfer to " + cid + " removed: client left");
            }
        }
    }

    private void fireUpdatedFileSet(Set<SharedFile> added, Set<SharedFile> removed) {
        synchronized (handlers) {
            for (IFileSetListener listener : handlers.values()) {
                listener.fileSetUpdated(added, removed);
            }
        }
    }

    public void addMessage(Message message) {
        Action action = message.getAction();
        if (action != null) {
            switch (action.getActionType()) {
                case GET:
                    processGet(message.getSID(), message.getClientID(), (GetAction) action);
                    break;
                case TRANSFER:
                    processTransfer(message.getSID(), (TransferAction) action);
                    break;
                default:
                    logger.log("Unrecognized message on router: " + message.encodeMessage());
            }
        }
    }

    private void processGet(String sid, String cid, GetAction action) {
        String hash = action.getFile().getHash();
        Set<String> owners = fileOwners.get(hash);
        if ((owners != null) && (owners.size() > 0)) {
            String[] arOwners = owners.toArray(new String[1]);
            String seeder = arOwners[random.nextInt(arOwners.length)];
            synchronized (monitor) {
                transfers.put(sid, cid);
            }
            Message request = new Message(action, FileProtocolType.Direction.SC_RQ, seeder, sid, null);
            handlers.get(seeder).addToOutbox(request);
        }
    }

    private void processTransfer(String sid, TransferAction action) {
        String cid = transfers.get(sid);
        if (cid != null) {
            Message response = new Message(action, FileProtocolType.Direction.SC_RS, cid, sid, null);
            handlers.get(cid).addToOutbox(response);
        }
    }

    public void shutDown() {
        synchronized (handlers) {
            for (ClientHandler handler : handlers.values()) {
                handler.shutDown();
            }
        }
    }

}
