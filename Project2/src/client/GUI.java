package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GUI {
    private static String PORT;

    private static JTable fileTable;
    private static JTable chunkTable;
    private static JFrame gui;

    private static JPanel FilePanel() {
        JPanel files = new JPanel();

        fileTable = new JTable(new DefaultTableModel(Connection.getFiles(PORT), new String[]{"Name", "Size"}));
        fileTable.setVisible(true);

        JScrollPane tablePane = new JScrollPane(fileTable);
        files.add(tablePane);
        tablePane.setVisible(true);

        JButton uploadFile = new JButton("Upload File");
        uploadFile.setVisible(true);
        uploadFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadFile.addActionListener(GUI::uploadFile);
        files.add(uploadFile);

        files.setLayout(new BoxLayout(files, BoxLayout.Y_AXIS));
        return files;
    }

    private static JPanel ChunkPanel() {
        JPanel chunks = new JPanel();

        chunkTable = new JTable(new DefaultTableModel(Connection.getChunks(PORT),
                new String[]{"Identifier", "Size (Max: " + Connection.getMaxStorage()
                        + " - Total: " + Connection.getStorageSize() + ")"}));
        chunkTable.setVisible(true);

        JScrollPane tablePane = new JScrollPane(chunkTable);
        chunks.add(tablePane);
        tablePane.setVisible(true);

        JPanel storagePanel = new JPanel();

        JSpinner storageInput = new JSpinner(new SpinnerNumberModel(0, 0, Long.MAX_VALUE, 1));
        storageInput.setVisible(true);
        storagePanel.add(storageInput);

        JButton storageSet = new JButton("Set Storage");
        storageSet.setVisible(true);
        storageSet.setAlignmentX(Component.CENTER_ALIGNMENT);
        storagePanel.add(storageSet);

        storagePanel.setLayout(new FlowLayout());
        chunks.add(storagePanel);


        chunks.setLayout(new BoxLayout(chunks, BoxLayout.Y_AXIS));
        return chunks;
    }

    public static void main(String[] args) {
        GUI.PORT = args[0];

        gui = new JFrame("P2P Backup Client");

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));
        final JPanel files = FilePanel();
        tabs.addTab("Uploaded Files", files);
        tabs.addTab("Stored Chunks", ChunkPanel());
        tabs.setSelectedComponent(files);
        gui.add(tabs);

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.setVisible(true);
        reloadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        reloadBtn.addActionListener(GUI::reloadTables);
        gui.add(reloadBtn);

        gui.setSize(600, 300);
        gui.getContentPane().setLayout(new BoxLayout(gui.getContentPane(), BoxLayout.Y_AXIS));
        gui.setVisible(true);

        gui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private static void uploadFile(ActionEvent e) {
        final JFileChooser chooser = new JFileChooser();

        if (chooser.showDialog(gui, "Upload") == JFileChooser.APPROVE_OPTION) {
            JProgressBar progress = new JProgressBar();
            gui.add(progress);

            Connection.uploadFile(PORT, progress, chooser.getSelectedFile().getPath());
        }
    }

    private static void reloadTables(ActionEvent e) {
        ((DefaultTableModel) (fileTable.getModel()))
                .setDataVector(Connection.getFiles(PORT), new String[]{"Name", "Size"});
        ((DefaultTableModel) (chunkTable.getModel()))
                .setDataVector(Connection.getChunks(PORT),
                        new String[]{"Identifier", "Size (Max: " + Connection.getMaxStorage()
                                + " - Total: " + Connection.getStorageSize() + ")"});
    }
}
