package ru.ifmo.team.filenetwork.client.gui;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.client.FileClient;
import ru.ifmo.team.filenetwork.client.IFileClient;
import ru.ifmo.team.filenetwork.client.IFileWatcher;
import ru.ifmo.team.filenetwork.client.IManager;
import ru.ifmo.team.util.PropReader;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.client.IClient;
import ru.ifmo.team.util.tcp.client.TCPClient;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: Daniel Penkin
 * Date: May 10, 2009
 * Version: 1.0
 */
public class ClientGUI extends JFrame implements IFileWatcher, IManager {

    private static final String LOG_FILE_NAME = "client";
    private static final String PROPERTIES_FILE_NAME = "client.properties";

    private final FileListModel foreignModel = new FileListModel();
    private final FileListModel localModel = new FileListModel();

    private final Map<String, SharedFile> downloads = new HashMap<String, SharedFile>();

    private final JList foreignList = new JList(foreignModel);
    private final JList localList = new JList(localModel);

    private IFileClient fileClient;

    private String host;
    private int port;

    public ClientGUI() {
        super("File Network tcpClient");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        localList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        foreignList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        localList.setPrototypeCellValue("Some file shall be here w/size");
        foreignList.setPrototypeCellValue("Some file shall be here w/size");

        JPanel foreign = new JPanel();
        foreign.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Shared files"));
        foreign.add(new JScrollPane(foreignList));

        JPanel local = new JPanel();
        local.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Local files"));
        local.add(new JScrollPane(localList));

        Box listPanel = new Box(BoxLayout.X_AXIS);
        listPanel.add(foreign);
        listPanel.add(Box.createHorizontalStrut(5));
        listPanel.add(local);

        JButton addButton = new JButton(new AbstractAction("Add file") {
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser dlg = new JFileChooser();
                int returnVal = dlg.showOpenDialog(ClientGUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = dlg.getSelectedFile();
                    String desc = JOptionPane.showInputDialog(ClientGUI.this, "Input description of file",
                            "Description", JOptionPane.INFORMATION_MESSAGE);
                    Map<File, String> files = new HashMap<File, String>();
                    files.put(file, desc);
                    if (!fileClient.addLocalFiles(files)) {
                        JOptionPane.showMessageDialog(ClientGUI.this, "Unable to add files, see log",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        JButton removeButton = new JButton(new AbstractAction("Remove file") {
            public void actionPerformed(ActionEvent actionEvent) {
                SharedFile file = (SharedFile) localList.getSelectedValue();
                if (file != null) {
                    int res = JOptionPane.showConfirmDialog(ClientGUI.this, "Are you sure you want to delete file?",
                            "Confirm deletion", JOptionPane.YES_NO_OPTION);
                    if (res == JOptionPane.YES_OPTION) {
                        Set<SharedFile> files = new HashSet<SharedFile>();
                        files.add(file);
                        if (!fileClient.removeLocalFiles(files)) {
                            JOptionPane.showMessageDialog(ClientGUI.this, "Unable to remove files, see log",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        JButton getButton = new JButton(new AbstractAction("Get file") {
            public void actionPerformed(ActionEvent actionEvent) {
                SharedFile file = (SharedFile) foreignList.getSelectedValue();
                if (file != null) {
                    JFileChooser dlg = new JFileChooser();
                    int returnVal = dlg.showSaveDialog(ClientGUI.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File dest = dlg.getSelectedFile();
                        String id = fileClient.downloadFile(file, dest);
                        if (id == null) {
                            JOptionPane.showMessageDialog(ClientGUI.this, "You are already sharing this file.\n" +
                                    "Check your local shared files list.", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            downloads.put(id, file);
                        }
                    }
                }
            }
        });

        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(getButton);


        Box mainPanel = Box.createVerticalBox();
        mainPanel.add(listPanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);

        pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((int) ((screen.getWidth() - getWidth()) / 2),
                (int) ((screen.getHeight() - getHeight()) / 2));

        Map<String, String> props = null;
        boolean propertiesRead = true;
        try {
            props = PropReader.readProperties(new File(PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            propertiesRead = false; // not loaded
        }

        String logFile = LOG_FILE_NAME;

        if (propertiesRead) {
            String logFolder = props.get("log_folder");
            if (logFolder != null) {
                logFile = logFolder + logFile;
            } else {
                String msg = "log_folder property not found, using current folder for logging";
                System.out.println(msg);
                JOptionPane.showMessageDialog(this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
            }
            host = props.get("server_host");
            if (host == null) {
                errorExit("host property not found");
            }
            try {
                port = Integer.parseInt(props.get("server_port"));
            } catch (NumberFormatException e) {
                errorExit("port property not found or not correct");
            }
        }

        logFile += "_" + System.currentTimeMillis() % 1000;

        String log = logFile + ".log";
        String tcpLog = logFile + "_tcp.log";
        System.out.println("Trying to log into Client log: " + log);
        System.out.println("Trying to log into Client_tcp log: " + tcpLog);
        Logger clientLogger = new Logger(log);
        Logger clientTCPLogger = new Logger(tcpLog);
        clientLogger.clearLog();
        clientTCPLogger.clearLog();
        fileClient = new FileClient(this, clientLogger);
        fileClient.registerFileListener(this);
        IClient tcpClient = new TCPClient(fileClient, clientTCPLogger);
        fileClient.registerTCPClient(tcpClient);

        if (!tcpClient.start(host, port)) {
            errorExit("Unable to start tcp client, see logs for detailed information");
        }
    }

    private void errorExit(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(this, message, "Error on starting", JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }

    public void downloadCompleted(String id, File file) {
        SharedFile shared = downloads.remove(id);
        JOptionPane.showMessageDialog(this, "Download of file " + shared + " completed.\n" +
                "File saved in " + file.getPath(), "Finished", JOptionPane.INFORMATION_MESSAGE);
    }

    public void fileListUpdated(Set<SharedFile> local, Set<SharedFile> foreign) {
        localModel.setList(local.toArray(new SharedFile[1]));
        foreignModel.setList(foreign.toArray(new SharedFile[1]));
        repaint();
    }

    public void connectionClosed() {
        errorExit("Connection with server was close, see logs for detailed information.\n" +
                "Client can't work anymore until server is up");
    }

    public static void main(String[] args) {
        new ClientGUI().setVisible(true);
    }

    private class FileListModel extends AbstractListModel {

        private SharedFile[] files;

        public void setList(SharedFile[] files) {
            this.files = files;
        }

        public int getSize() {
            if (files != null) {
                return files.length;
            }
            return 0;
        }

        public Object getElementAt(int i) {
            if (files != null) {
                return files[i];
            }
            return null;
        }
    }
}
