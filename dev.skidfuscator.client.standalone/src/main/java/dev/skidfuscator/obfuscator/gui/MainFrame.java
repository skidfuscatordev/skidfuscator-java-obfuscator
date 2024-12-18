// MainFrame.java
package dev.skidfuscator.obfuscator.gui;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import lombok.Getter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;

@Getter
public class MainFrame extends JFrame {
    private final JTabbedPane tabbedPane;
    private ConfigPanel configPanel;
    private TransformerPanel transformerPanel;
    private ConsolePanel consolePanel;
    private JButton startButton;
    private JPanel headerPanel;

    public MainFrame() {
        setTitle("Skidfuscator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 700));

        // Create main layout with increased padding
        setLayout(new BorderLayout(15, 5));

        // Add header with logo

        JPanel tabbedPanel = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT) {
            @Override
            public void updateUI() {
                super.updateUI();
                setUI(new FlatTabbedPaneUI() {
                    @Override
                    protected Insets getTabAreaInsets(int tabPlacement) {
                        Insets insets = super.getTabAreaInsets(tabPlacement);
                        return new Insets(220, insets.left, insets.bottom, insets.right);
                    }

                    @Override
                    protected int calculateTabAreaWidth(int tabPlacement, int horizRunCount, int maxTabWidth) {
                        return 160;
                    }

                    @Override
                    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                        // Make tabs fill the entire width of the tab area
                        return 160;
                    }

                    @Override
                    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                        try {
                            // Draw logo
                            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
                            if (logoStream != null) {
                                Image logo = ImageIO.read(logoStream);
                                Image scaledLogo = logo.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                                g.drawImage(scaledLogo, 5, 10, null);
                            }

                            // Draw separator line
                            g.setColor(Color.DARK_GRAY);
                            g.drawLine(5, 155, 155, 155);

                            // Draw version info
                            g.setColor(new Color(200,190,220));
                            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                            g.drawString("Skidfuscator Community", 10, 175);
                            g.setColor(new Color(130, 130, 130));
                            g.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                            g.drawString("Build: 2023.1", 10, 190);

                            // Draw second separator
                            g.setColor(Color.DARK_GRAY);
                            g.drawLine(5, 205, 155, 205);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        super.paintTabArea(g, tabPlacement, selectedIndex);
                    }
                });
            }
        };
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        initializeTabs();
        tabbedPanel.add(tabbedPane);
        add(tabbedPanel, BorderLayout.CENTER);

        // Create a more polished button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        startButton = new JButton("Start Obfuscation");
        startButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        startButton.setBackground(new Color(70, 130, 180));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == startButton) {
                    startObfuscation();
                }
            }
        });
        buttonPanel.add(startButton);
        add(buttonPanel, BorderLayout.SOUTH);

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
                SwingUtilities.invokeLater(() -> {
                    int option = JOptionPane.showOptionDialog(
                        MainFrame.this,
                        "Obfuscation completed successfully!",
                        "Success",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        UIManager.getIcon("OptionPane.informationIcon"),
                        new Object[]{"OK", "Open Output Folder"},
                        "OK"
                    );
                    
                    if (option == 1) {
                        // Open output folder
                        try {
                            File outputFile = new File(configPanel.getOutputPath());
                            Desktop.getDesktop().open(outputFile.getParentFile());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(
                                MainFrame.this,
                                "Could not open output folder: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                });
            }
        };
        worker.execute();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));

        try {
            // Load logo from resources
            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
            if (logoStream != null) {
                Image logo = ImageIO.read(logoStream);
                Image scaledLogo = logo.getScaledInstance(120, 120, Image.SCALE_DEFAULT);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
                header.add(logoLabel);
            }
            // Add title label
        } catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }
}