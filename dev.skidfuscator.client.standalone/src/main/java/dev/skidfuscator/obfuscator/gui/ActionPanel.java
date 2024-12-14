package dev.skidfuscator.obfuscator.gui;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ActionPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTextArea logArea;
    private final JButton startButton;

    public ActionPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create log area
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        startButton = new JButton("Start Obfuscation");
        startButton.addActionListener(e -> startObfuscation());
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void startObfuscation() {
        ConfigPanel config = mainFrame.getConfigPanel();
        TransformerPanel transformers = mainFrame.getTransformerPanel();

        // Validate inputs
        if (config.getInputPath().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an input JAR file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create session
        SkidfuscatorSession session = SkidfuscatorSession.builder()
                .input(new File(config.getInputPath()))
                .output(new File(config.getOutputPath()))
                .libs(config.getLibsPath().isEmpty()
                        ? new File[0]
                        : new File(config.getLibsPath()).listFiles()
                )
                .runtime(config.getRuntimePath().isEmpty()
                        ? null
                        : new File(config.getRuntimePath())
                )
                .phantom(false)
                .debug(config.isDebugEnabled())
                .build();

        // Start obfuscation in background
        startButton.setEnabled(false);
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    new Skidfuscator(session).run();
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("Error: " + e.getMessage() + "\n");
                        e.printStackTrace();
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
                logArea.append("Obfuscation completed!\n");
            }
        };
        worker.execute();
    }
}