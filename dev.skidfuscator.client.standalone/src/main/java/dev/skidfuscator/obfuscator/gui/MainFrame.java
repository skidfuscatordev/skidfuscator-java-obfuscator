// MainFrame.java
package dev.skidfuscator.obfuscator.gui;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

@Getter
public class MainFrame extends JFrame {
    private final JTabbedPane tabbedPane;
    private ConfigPanel configPanel;
    private TransformerPanel transformerPanel;
    private ConsolePanel consolePanel;
    private JButton startButton;

    public MainFrame() {
        setTitle("Skidfuscator Obfuscator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));

        // Create main layout
        setLayout(new BorderLayout(10, 10));

        // Initialize tabbed pane
        tabbedPane = new JTabbedPane();
        initializeTabs();

        // Initialize action panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        startButton = new JButton("Start Obfuscation");
        startButton.addActionListener(e -> startObfuscation());
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add components to frame
        add(tabbedPane, BorderLayout.CENTER);

        // Set keyboard mnemonics
        setupKeyboardShortcuts();

        // Final setup
        pack();
        setLocationRelativeTo(null);
    }

    private void initializeTabs() {
        // Initialize panels
        configPanel = new ConfigPanel();
        transformerPanel = new TransformerPanel();
        consolePanel = new ConsolePanel();

        // Add Configuration tab
        JPanel configTabPanel = createTabPanel(configPanel, "Configuration");
        tabbedPane.addTab("Configuration", null, configTabPanel, "Basic configuration settings");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);

        // Add Transformers tab
        JPanel transformerTabPanel = createTabPanel(transformerPanel, "Transformers");
        tabbedPane.addTab("Transformers", null, transformerTabPanel, "Transformer settings and options");
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_T);

        // Add Console tab
        JPanel consoleTabPanel = createTabPanel(consolePanel, "Console Output");
        tabbedPane.addTab("Console", null, consoleTabPanel, "View obfuscation progress and logs");
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_O);

        // Set default selected tab
        tabbedPane.setSelectedIndex(0);
    }

    private JPanel createTabPanel(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createTitledBorder(title)
        ));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void setupKeyboardShortcuts() {
        // Alt + C for Configuration tab
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);

        // Alt + T for Transformers tab
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_T);

        // Additional keyboard shortcuts can be added here
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        getRootPane().registerKeyboardAction(
                e -> this.startObfuscation(),
                ctrlS,
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /**
     * Returns the configuration panel instance.
     * @return The configuration panel
     */
    public ConfigPanel getConfigPanel() {
        return configPanel;
    }

    /**
     * Returns the transformer panel instance.
     * @return The transformer panel
     */
    public TransformerPanel getTransformerPanel() {
        return transformerPanel;
    }

    /**
     * Returns the currently selected tab index.
     * @return The selected tab index
     */
    public int getSelectedTab() {
        return tabbedPane.getSelectedIndex();
    }

    /**
     * Selects the specified tab.
     * @param index The tab index to select
     */
    public void selectTab(int index) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    /**
     * Updates the tab enabled states based on the current application state.
     * @param configEnabled Whether the config tab should be enabled
     * @param transformersEnabled Whether the transformers tab should be enabled
     */
    public void updateTabStates(boolean configEnabled, boolean transformersEnabled) {
        tabbedPane.setEnabledAt(0, configEnabled);
        tabbedPane.setEnabledAt(1, transformersEnabled);
    }

    public void startObfuscation() {
        ConfigPanel config = this.getConfigPanel();
        TransformerPanel transformers = this.getTransformerPanel();

        // Validate inputs
        if (config.getInputPath().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an input JAR file", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate inputs
        tabbedPane.setSelectedIndex(2);

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
                .jmod(config.getRuntimePath().contains("jmods"))
                .debug(config.isDebugEnabled())
                .build();

        // Start obfuscation in background
        startButton.setEnabled(false);
        SwingWorker<Void, String> worker = new SwingWorker() {
            @Override
            protected Void doInBackground() {
                try {
                    new Skidfuscator(session).run();
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        e.printStackTrace();
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                startButton.setEnabled(true);
            }
        };
        worker.execute();
    }
}