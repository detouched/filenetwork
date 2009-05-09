package ru.ifmo.team.filenetwork.client;

import ru.ifmo.team.filenetwork.Message;
import ru.ifmo.team.filenetwork.MessagingException;
import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.actions.AddAction;
import ru.ifmo.team.fileprotocol.FileProtocolType;
import ru.ifmo.team.util.IMessageAcceptor;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.client.ClientException;
import ru.ifmo.team.util.tcp.client.TCPClient;

import java.util.HashSet;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: Apr 26, 2009
 * Version: 1.0
 */
public class Main {
    private static final int AMOUNT = 1;
    private static final String clientLog = "/Users/danielpenkin/Desktop/logs/tcp_client";
    private static final Set<Sender> clients = new HashSet<Sender>();

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < AMOUNT; i++) {
            Logger logger = new Logger(clientLog + i + 1 + ".log");
            logger.clearLog();
            Sender sender = new Sender(logger);
            clients.add(sender);
        }

        System.out.println("Starting clients...");
        for (Sender sender : clients) {
            sender.start();
        }
        System.out.println("Waiting for clients to finish...");
        for (Sender sender : clients) {
            sender.join();
        }
    }

    private static class Sender extends Thread {
        private final TCPClient client;

        private String cid;

        private Sender(Logger logger)  {
            IMessageAcceptor messageAcceptor = new IMessageAcceptor() {
                public void acceptMessage(String ip, String message) {
                    try {
                        System.out.println("Message received: " + message);
                        Message msg = Message.decodeMessage(message);
                        cid = msg.getClientID();
                    } catch (MessagingException ignored) {
                    }
                }

                public void clientLeft(String ip) {
                    System.out.println("Client left");
                }
            };
            client = new TCPClient(messageAcceptor, logger);
        }

        public void run() {
            if (client.start("127.0.0.1", 5555)) {
                while (cid == null) {}
                try {
                    SharedFile file = new SharedFile("NewFile.exe", "Hello world", 25, "AE4325ESA");
                    Set<SharedFile> added = new HashSet<SharedFile>();
                    added.add(file);
                    AddAction addAction = new AddAction(added);
                    Message message = new Message(addAction, FileProtocolType.Direction.CS_RQ, cid, null);
                    System.out.println("Sending...");
                    client.sendMessage(message.encodeMessage());
                    Thread.sleep(3000);
                    client.stop();
                } catch (ClientException ignored) {
                } catch (InterruptedException ignored) {
                }
            } else {
                System.out.println("Starting client FAILED");
            }
            client.stop();
        }

        public void shutDown() {
            client.stop();
        }
    }

}

