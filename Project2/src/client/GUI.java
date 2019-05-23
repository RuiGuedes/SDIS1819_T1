package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GUI {
    private static String PORT;

    private static JPanel FilePanel() {
        JPanel files = new JPanel();

        JTable fileTable = new JTable(Connection.getFiles(PORT), new String[]{"Name", "Size"});
        fileTable.setVisible(true);

        JScrollPane tablePane = new JScrollPane(fileTable);
        files.add(tablePane);
        tablePane.setVisible(true);

        JButton uploadFile = new JButton("Upload File");
        uploadFile.setVisible(true);
        uploadFile.setAlignmentX(Component.CENTER_ALIGNMENT);
        files.add(uploadFile);

        files.setLayout(new BoxLayout(files, BoxLayout.Y_AXIS));
        return files;
    }

    private static JPanel ChunkPanel() {
        JPanel chunks = new JPanel();

        JTable chunkTable = new JTable(Connection.getChunks(PORT),
                new String[]{"Identifier", "Size (Total: " + Connection.getStorageSize() + ")"});
        chunkTable.setVisible(true);

        JScrollPane tablePane = new JScrollPane(chunkTable);
        chunks.add(tablePane);
        tablePane.setVisible(true);

        JButton storageSet = new JButton("Set Storage");
        storageSet.setVisible(true);
        storageSet.setAlignmentX(Component.CENTER_ALIGNMENT);
        chunks.add(storageSet);

        chunks.setLayout(new BoxLayout(chunks, BoxLayout.Y_AXIS));
        return chunks;
    }

    public static void main(String[] args) {
        GUI.PORT = args[0];

        JFrame gui = new JFrame("P2P Backup Client");

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
        gui.add(reloadBtn);

        gui.setSize(300, 300);
        gui.getContentPane().setLayout(new BoxLayout(gui.getContentPane(), BoxLayout.Y_AXIS));
        gui.setVisible(true);

        gui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
