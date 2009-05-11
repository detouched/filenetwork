package ru.ifmo.team.filenetwork.server;

import ru.ifmo.team.filenetwork.Message;
import ru.ifmo.team.filenetwork.MessagingException;
import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.actions.Action;
import ru.ifmo.team.filenetwork.actions.AddAction;
import ru.ifmo.team.filenetwork.actions.RemoveAction;
import ru.ifmo.team.fileprotocol.FileProtocolType;
import ru.ifmo.team.util.ExceptionExpander;
import ru.ifmo.team.util.IMessageAcceptor;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.logging.PrefixLogger;
import ru.ifmo.team.util.tcp.server.IConnectionHandler;
import ru.ifmo.team.util.tcp.server.ServerException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 6, 2009
 * Version: 1.0
 */
/*package*/ class ClientHandler implements IFileSetListener, IMessageAcceptor {

    //    private final Map<String, Message> awaiting = new HashMap<String, Message>();
    private final Queue<Message> inbox = new LinkedList<Message>();
    private final Queue<Message> outbox = new LinkedList<Message>();
    private final IFileServer fileServer;
    private final String clientID;
    private final IConnectionHandler connectionHandler;
    private final PrefixLogger logger;

    private boolean shutdown = false;

    ClientHandler(IFileServer fileServer, String clientID, IConnectionHandler connectionHandler, Logger logger) {
        this.fileServer = fileServer;
        this.clientID = clientID;
        this.connectionHandler = connectionHandler;
        connectionHandler.registerMessageAcceptor(this);
        this.logger = new PrefixLogger("Client_" + hashCode(), logger);
        Thread sender = new Thread(new MessageSender());
        Thread processor = new Thread(new MessageProcessor());
        sender.setDaemon(true);
        processor.setDaemon(true);
        sender.start();
        processor.start();
    }

    public void addToOutbox(Message message) {
        outbox.add(message);
        synchronized (outbox) {
            outbox.notify();
        }
    }

    public void shutDown() {
        connectionHandler.shutDown();
    }

    public void fileSetUpdated(Set<SharedFile> added, Set<SharedFile> removed) {
        if (added != null) {
            addToOutbox(new Message(new AddAction(added), FileProtocolType.Direction.SC_RQ, clientID, null));
        }
        if (removed != null) {
            addToOutbox(new Message(new RemoveAction(removed), FileProtocolType.Direction.SC_RQ, clientID, null));
        }
    }

    public void connectionClosed(String ip) {
        shutdown = true;
        fileServer.clientLeft(clientID);
    }

    public void acceptMessage(String ip, String message) {
        try {
            inbox.add(Message.decodeMessage(message));
            synchronized (inbox) {
                inbox.notify();
            }
        } catch (MessagingException e) {
            logger.log("Unable to accept message, closing connection: " + ExceptionExpander.expandException(e));
            connectionHandler.shutDown();
        }
    }

    private class MessageSender implements Runnable {

        public void run() {
            while (!shutdown) {
                synchronized (outbox) {
                    if (outbox.isEmpty()) {
                        try {
                            outbox.wait();
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        String outMsg = outbox.poll().encodeMessage();
                        try {
                            connectionHandler.sendMessage(outMsg);
                        } catch (ServerException e) {
                            logger.log("Unable to send message, closing connection: " + ExceptionExpander.expandException(e));
                            connectionHandler.shutDown();
                        }
                    }
                }
            }
        }
    }

    private class MessageProcessor implements Runnable {

        public void run() {
            while (!shutdown) {
                synchronized (inbox) {
                    if (inbox.isEmpty()) {
                        try {
                            inbox.wait();
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        try {
                            processMessage(inbox.poll());
                        } catch (ServerException e) {
                            logger.log("Unable to process message, closing connection: " +
                                    ExceptionExpander.expandException(e));
                            connectionHandler.shutDown();
                        }
                    }
                }
            }
        }

        private void processMessage(Message message) throws ServerException {
            if (message == null) {
                return;
            }
            String id = message.getClientID();
            if (!clientID.equals(id)) {
                logger.log("Identification failed: \"" + id + "\" received but\"" + clientID + "\"expected");
                connectionHandler.shutDown();
                return;
            }
            FileProtocolType.Direction.Enum dir = message.getDirection();
            if (dir == FileProtocolType.Direction.CS_RQ) {
                Action action = message.getAction();
                if (action != null) {
//                    String response;
                    switch (action.getActionType()) {
                        case ADD:
                            AddAction addAction = (AddAction) action;
                            fileServer.addFiles(addAction.getFiles(), clientID);
//                            response = new Message(null, FileProtocolType.Direction.SC_RS,
//                                    clientID, message.getSID(), "OK").encodeMessage();
//                            connectionHandler.sendMessage(response);
                            break;
                        case REMOVE:
                            RemoveAction removeAction = (RemoveAction) action;
                            fileServer.removeFiles(removeAction.getFiles(), clientID);
//                            response = new Message(null, FileProtocolType.Direction.SC_RS,
//                                    clientID, message.getSID(), "OK").encodeMessage();
//                            connectionHandler.sendMessage(response);
                            break;
                        case GET:
                            fileServer.addMessage(message);
                            break;
                        default:
                            logger.log("Unrecognized message type, closing connection");
                            connectionHandler.shutDown();
                    }
                }
            } else if (dir == FileProtocolType.Direction.CS_RS) {
                logger.log("Processing Client->Server response");
                Action action = message.getAction();
                if (action != null) {
                    switch (action.getActionType()) {
                        case TRANSFER:
                            fileServer.addMessage(message);
                            break;
                        default:
                            logger.log("Unrecognized message type, closing connection");
                            connectionHandler.shutDown();
                    }
                }
            } else {
                logger.log("Received message from SERVER???");
            }
        }
    }

}
