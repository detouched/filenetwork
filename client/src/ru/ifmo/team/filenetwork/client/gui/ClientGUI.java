package ru.ifmo.team.filenetwork.client.gui;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.client.FileClient;
import ru.ifmo.team.filenetwork.client.IFileClient;
import ru.ifmo.team.filenetwork.client.IFileWatcher;
import ru.ifmo.team.filenetwork.client.IManager;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.client.IClient;
import ru.ifmo.team.util.tcp.client.TCPClient;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.io.File;
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

    private static final String clientLog = "/Users/danielpenkin/Desktop/logs/client.log";
    private static final String clientTCPLog = "/Users/danielpenkin/Desktop/logs/client_tcp.log";

    private final Set<SharedFile> foreignFiles = new HashSet<SharedFile>();
    private final Set<SharedFile> localFiles = new HashSet<SharedFile>();

    private final FileListModel foreignModel = new FileListModel();
    private final FileListModel localModel = new FileListModel();

    private final JButton addButton;
    private final JButton removeButton;

    private final JList foreignList = new JList(foreignModel);
    private final JList localList = new JList(localModel);

    private IFileClient fileClient;

    public ClientGUI() {
        super("File Network tcpClient");

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel foreign = new JPanel();
        foreign.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Shared files"));
        foreign.add(localList);

        JPanel local = new JPanel();
        local.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Local files"));
        local.add(foreignList);

        Box listPanel = new Box(BoxLayout.X_AXIS);
        listPanel.add(foreign);
        listPanel.add(Box.createHorizontalStrut(5));
        listPanel.add(local);

        addButton = new JButton(new AbstractAction("Add file") {
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

        removeButton = new JButton(new AbstractAction("Remove file") {
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

        Box buttonPanel = Box.createHorizontalBox();
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(5));
        buttonPanel.add(removeButton);


        Box mainPanel = Box.createVerticalBox();
        mainPanel.add(listPanel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);

        pack();


        Logger clientLogger = new Logger(clientLog);
        Logger clientTCPLogger = new Logger(clientTCPLog);
        clientLogger.clearLog();
        clientTCPLogger.clearLog();
        fileClient = new FileClient(this, clientLogger);
        fileClient.registerFileListener(this);
        IClient tcpClient = new TCPClient(fileClient, clientTCPLogger);
        fileClient.registerTCPClient(tcpClient);

        if (!tcpClient.start("127.0.0.1", 5555)) {
            System.out.println("Unable to start client, exiting");
            System.exit(-1);
        }
    }

    public void downloadCompleted(String id, File file) {
        JOptionPane.showMessageDialog(this, "Download of file " + file.getName() + " completed",
                "Finished", JOptionPane.INFORMATION_MESSAGE);
    }

    public void fileListUpdated(Set<SharedFile> local, Set<SharedFile> foreign) {
        localModel.setList(local.toArray(new SharedFile[1]));
        localList.updateUI();
        foreignModel.setList(foreign.toArray(new SharedFile[1]));
        foreignList.updateUI();
        repaint();
    }

    public void connectionClosed() {
        System.out.println("Connection closed, see logs, exiting");
        System.exit(-2);
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
