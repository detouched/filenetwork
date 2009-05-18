package ru.ifmo.team.filenetwork.client.gui;

import ru.ifmo.team.filenetwork.SharedFile;
import ru.ifmo.team.filenetwork.client.FileClient;
import ru.ifmo.team.filenetwork.client.IFileClient;
import ru.ifmo.team.filenetwork.client.IFileWatcher;
import ru.ifmo.team.util.PropReader;
import ru.ifmo.team.util.logging.Logger;
import ru.ifmo.team.util.tcp.client.IClient;
import ru.ifmo.team.util.tcp.client.TCPClient;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

/**
 * User: Daniel Penkin
 * Date: May 10, 2009
 * Version: 1.0
 */
public class ClientGUI extends JFrame implements IFileWatcher {

    private static final String LOG_FILE_NAME = "client";
    private static final String PROPERTIES_FILE_NAME = "client.properties";
    private static final int WINDOW_HEIGHT = 400;
    private static final int WINDOW_WIDTH = 600;
    private static final Map<String, Icon> icons = new HashMap<String, Icon>();

    private final Map<String, SharedFile> downloads = new HashMap<String, SharedFile>();

    private final FileTableModel foreignModel = new FileTableModel();
    private final FileTableModel localModel = new FileTableModel();
    private final JTable foreignTable = new JTable(foreignModel);
    private final JTable localTable = new JTable(localModel);

    private IFileClient fileClient;
    private List<SharedFile> foreignFiles;
    private List<SharedFile> localFiles;

    private String host;
    private int port;

    public ClientGUI() {
        super("File Network tcpClient");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        foreignTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        foreignTable.setRowSelectionAllowed(true);
        localTable.setRowSelectionAllowed(true);

        final JPanel foreignPanel = new FileInfoPanel(foreignTable, 2);
        foreignTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                foreignPanel.updateUI();
            }
        });

        final JPanel localPanel = new FileInfoPanel(localTable, 1);
        localTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                localPanel.updateUI();
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        final JScrollPane foreignScrollPane = new JScrollPane(foreignTable);
        final JScrollPane localScrollPane = new JScrollPane(localTable);


        final AbstractAction addFileAction = new AbstractAction("Add file") {
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
        };
        JButton addButton = new JButton(addFileAction);
        final AbstractAction removeFileAction = new AbstractAction("Remove file") {
            public void actionPerformed(ActionEvent actionEvent) {
                SharedFile file = localFiles.get(foreignTable.getSelectedRow());
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
        };
        JButton removeButton = new JButton(removeFileAction);
        final AbstractAction getFileAction = new AbstractAction("Get file") {
            public void actionPerformed(ActionEvent actionEvent) {
                SharedFile file = localFiles.get(foreignTable.getSelectedRow());
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
        };
        JButton getButton = new JButton(getFileAction);

        localPanel.add(addButton);
        localPanel.add(removeButton);
        localPanel.add(getButton);

        foreignPanel.add(getButton);

        JSplitPane foreignSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, foreignScrollPane, foreignPanel);
        foreignSplitPane.setResizeWeight(0.7);
        tabbedPane.addTab("Shared files", foreignSplitPane);

        JSplitPane localSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, localScrollPane, localPanel);
        localSplitPane.setResizeWeight(0.7);


        tabbedPane.addTab("Local files", localSplitPane);


        getContentPane().add(tabbedPane);

        pack();
        moveWindowToCenter();

        Map<String, String> props = null;
        try {
            props = PropReader.readProperties(new File(PROPERTIES_FILE_NAME));
        } catch (IOException e) {
            errorExit("Cannot read properties file (" + PROPERTIES_FILE_NAME + "): " + e.getMessage());
        }

        String logFile = LOG_FILE_NAME;
        int partSize = 0;

        if (props != null) {
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
                port = 0;
            }
            if (port == 0) {
                errorExit("port property not found or set incorrect");
            }

            try {
                partSize = Integer.parseInt(props.get("part_size"));
            } catch (NumberFormatException e) {
                partSize = 0;
            }
        } else {
            errorExit("Properties were not read properly");
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
        IClient tcpClient = new TCPClient(clientTCPLogger);
        fileClient = new FileClient(tcpClient, host, port, partSize, clientLogger);
        fileClient.registerFileListener(this);
        fileClient.start();
    }

    private void moveWindowToCenter() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((int) ((screen.getWidth() - getWidth()) / 2),
                (int) ((screen.getHeight() - getHeight()) / 2));
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
        localFiles = new LinkedList<SharedFile>(local);
        foreignFiles = new LinkedList<SharedFile>(foreign);
        localModel.setFiles(localFiles);
        foreignModel.setFiles(foreignFiles);
        localTable.updateUI();
        foreignTable.updateUI();
    }

    public void connectionClosed() {
        errorExit("Connection with server was closed, see logs for detailed information.\n" +
                "Client can't work anymore until server is up");
    }

    private List<SharedFile> getList(int number) {
        if (number == 1) {
            return localFiles;
        }
        return foreignFiles;
    }

    public static void main(String[] args) {
        new ClientGUI().setVisible(true);
    }

    private class FileTableModel extends AbstractTableModel {
        private List<SharedFile> files = new LinkedList<SharedFile>();
        private String[] colNames = new String[]{"Name", "Size (Kb)", "Description"};

        public void setFiles(List<SharedFile> files) {
            this.files = files;
        }

        public int getRowCount() {
            return files.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int i, int i1) {
            if (i1 == 0) {
                return files.get(i).getName();
            } else if (i1 == 1) {
                BigDecimal dec = new BigDecimal(files.get(i).getSize() / 1024d);
                dec = dec.setScale(2, BigDecimal.ROUND_UP);
                return dec;
            } else if (i1 == 2) {
                return files.get(i).getDescription();
            }
            return null;
        }

        public String getColumnName(int i) {
            return colNames[i];
        }
    }

    private class FileInfoPanel extends JPanel {
        private JTable table;
        private JLabel fileIconLabel = new JLabel("<no file selected>");
        private int listNum; //local = 1; foreign = 2

        public FileInfoPanel(JTable table, int list) {
            this.table = table;
            if (list != 1) {
                this.add(fileIconLabel);
            }
            listNum = list;
        }

        private Icon getIcon(String extension) {
            Icon icon = icons.get(extension);
            if (icon == null) {
                try {
                    //Create a temporary file with the specified extension
                    File file = File.createTempFile("icon", extension);
//                ShellFolder shellFolder = ShellFolder.getShellFolder(file);
//                System.out.println(shellFolder);
//                icon = new ImageIcon(shellFolder.getIcon(true));

                    FileSystemView view = FileSystemView.getFileSystemView();
                    icon = view.getSystemIcon(file);

                    file.delete();
                } catch (IOException e) {
                    System.out.println("Can't manage icon: " + e.getMessage());
                }
                return icon;
            }
            return icon;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.

            int row = table.getSelectedRow();
            if (row >= 0) {
                SharedFile file = getList(listNum).get(row);
                int extensionIndex = file.getName().lastIndexOf(".");
                final String fileExtension = file.getName().substring(extensionIndex);
                Icon icon = getIcon(fileExtension);
                fileIconLabel.setText(file.getName());
                fileIconLabel.setIcon(icon);
                fileIconLabel.repaint();
            } else {
                fileIconLabel.setText("<no file selected>");
            }
        }
    }

}
