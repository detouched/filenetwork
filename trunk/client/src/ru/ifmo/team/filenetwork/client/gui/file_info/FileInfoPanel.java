package ru.ifmo.team.filenetwork.client.gui.file_info;

import ru.ifmo.team.filenetwork.SharedFile;
import sun.awt.shell.ShellFolder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FileInfoPanel extends JPanel {
    private JList foreignList;
    private JLabel fileIconLabel = new JLabel("<no file selected>");


    public FileInfoPanel(JList foreignList) {

        this.foreignList = foreignList;
        this.add(fileIconLabel);

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);    //To change body of overridden methods use File | Settings | File Templates.

        if (!foreignList.isSelectionEmpty()) {
            try {
                //Create a temporary file with the specified extension
                SharedFile sharedFile = (SharedFile) foreignList.getSelectedValue();
                int extensionIndex = sharedFile.getName().lastIndexOf(".");
                final String fileExtension = sharedFile.getName().substring(extensionIndex);

                File file = File.createTempFile("icon", fileExtension);
                ShellFolder shellFolder = ShellFolder.getShellFolder(file);
                ImageIcon icon = new ImageIcon(shellFolder.getIcon(true));

                fileIconLabel.setText(sharedFile.getName());
                fileIconLabel.setIcon(icon);
                fileIconLabel.repaint();

                //Delete the temporary file
                file.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            fileIconLabel.setText("<no file selected>");
        }
    }
}
