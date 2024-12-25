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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class MainFrame extends JFrame {
    private final JTabbedPane tabbedPane;
    private ConfigPanel configPanel;
    private TransformerPanel transformerPanel;
    private ConsolePanel consolePanel;
    private LibrariesPanel librariesPanel;
    private JButton startButton;
    private JButton buyEnterpriseButton;
    private JPanel headerPanel;

    public MainFrame() {
        setTitle("Skidfuscator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 700));
        setResizable(false);
        setLayout(new BorderLayout(15, 5));

        // Set application icon
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/logo.png");
            if (iconStream != null) {
                setIconImage(ImageIO.read(iconStream));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create a panel for the left side that will contain both tabbed pane and button
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(180, getHeight()));

        // Create the tabbed pane
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
                        return 180;
                    }

                    @Override
                    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                        return 180;
                    }

                    @Override
                    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
                        try {
                            // Draw logo
                            InputStream logoStream = getClass().getResourceAsStream("/images/logo.png");
                            if (logoStream != null) {
                                Image logo = ImageIO.read(logoStream);
                                Image scaledLogo = logo.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                                g.drawImage(scaledLogo, 20, 10, null);
                            }

                            // Draw separator line
                            g.setColor(Color.DARK_GRAY);
                            g.drawLine(5, 155, 175, 155);

                            // Draw version info
                            g.setColor(new Color(200,190,220));
                            g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                            g.drawString("Skidfuscator Community", 20, 175);
                            g.setColor(new Color(130, 130, 130));
                            g.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                            g.drawString("Build: 2023.1", 20, 190);

                            // Draw second separator
                            g.setColor(Color.DARK_GRAY);
                            g.drawLine(5, 205, 175, 205);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        super.paintTabArea(g, tabPlacement, selectedIndex);
                    }
                });
            }
        };

        // Create and configure start button
        startButton = new JButton("Start Obfuscation");
        startButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        startButton.setBackground(new Color(70, 130, 180));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(160, 30));
        startButton.addActionListener(e -> {
            if (e.getSource() == startButton) {
                startObfuscation();
            }
        });

        buyEnterpriseButton = new JButton("Buy Enterprise");
        buyEnterpriseButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        buyEnterpriseButton.setBackground(Color.DARK_GRAY);
        buyEnterpriseButton.setForeground(Color.WHITE);
        buyEnterpriseButton.setFocusPainted(false);
        buyEnterpriseButton.setPreferredSize(new Dimension(160, 30));

        buyEnterpriseButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://skidfuscator.dev/pricing"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        int topSpace = 220;
        int bottomPadding = this.getPreferredSize().height - 60*3;

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(startButton);
        buttonPanel.add(buyEnterpriseButton);
        
        // Add copyright and website info
        JPanel copyrightPanel = new JPanel();
        copyrightPanel.setLayout(new BoxLayout(copyrightPanel, BoxLayout.Y_AXIS));
        copyrightPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 20, 0));
        JLabel copyrightLabel = new JLabel("© 2025 Skidfuscator");
        copyrightLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        copyrightLabel.setForeground(new Color(130, 130, 130));
        copyrightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        copyrightLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel allRightsReservedText = new JLabel("<html><div style='width:100%;text-align:justify'>All rights reserved. The use of this software is subject to the terms of the Skidfuscator License Agreement. Unauthorized reproduction or distribution of this software, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible under law.</div></html>");
        allRightsReservedText.setFont(new Font("Segoe UI", Font.PLAIN, 6));
        allRightsReservedText.setForeground(new Color(130, 130, 130));
        allRightsReservedText.setAlignmentX(Component.CENTER_ALIGNMENT);
        allRightsReservedText.setHorizontalAlignment(SwingConstants.CENTER);
        allRightsReservedText.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        JSeparator separator = new JSeparator();
        separator.setForeground(Color.DARK_GRAY);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        
        JLabel websiteLabel = new JLabel("skidfuscator.dev");
        websiteLabel.setFont(new Font("Courier New", Font.PLAIN, 10));
        websiteLabel.setForeground(new Color(70, 130, 180));
        websiteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        websiteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        websiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        websiteLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://skidfuscator.dev"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        copyrightPanel.add(copyrightLabel);
        copyrightPanel.add(separator);
        copyrightPanel.add(allRightsReservedText);
        copyrightPanel.add(separator);
        copyrightPanel.add(websiteLabel);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 0));

        // Initialize panels
        configPanel = new ConfigPanel();
        transformerPanel = new TransformerPanel();
        consolePanel = new ConsolePanel();
        librariesPanel = new LibrariesPanel(configPanel, null);

        // Add tabs (without content)
        tabbedPane.addTab("Configuration", null);
        tabbedPane.addTab("Libraries", null);
        tabbedPane.addTab("Transformers", null);
        tabbedPane.addTab("Console", null);

        // Set mnemonics
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_L);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_T);
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_O);

        // Add components to the left panel
        leftPanel.add(tabbedPane, BorderLayout.NORTH);
        leftPanel.add(buttonPanel, BorderLayout.CENTER);
        leftPanel.add(copyrightPanel, BorderLayout.SOUTH);

        // Create a panel for the content area
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 0, 10, 10));
        contentPanel.add(configPanel, BorderLayout.CENTER); // Show config panel by default

        // Add panels to the frame
        add(leftPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Add a listener to update the content panel when tabs change
        tabbedPane.addChangeListener(e -> {
            contentPanel.removeAll();
            switch (tabbedPane.getSelectedIndex()) {
                case 0:
                    contentPanel.add(configPanel, BorderLayout.CENTER);
                    break;
                case 1:
                    contentPanel.add(librariesPanel, BorderLayout.CENTER);
                    break;
                case 2:
                    contentPanel.add(transformerPanel, BorderLayout.CENTER);
                    break;
                case 3:
                    contentPanel.add(consolePanel, BorderLayout.CENTER);
                    break;
            }
            contentPanel.revalidate();
            contentPanel.repaint();
        });

        // Final setup
        pack();
        setLocationRelativeTo(null);
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
        tabbedPane.setSelectedIndex(3);

        // Validate libs
        // Initialize library folder
        String configLibPath = configPanel.getLibraryPath();
        Path libraryFolder;
        if (configLibPath != null && !configLibPath.isEmpty()) {
            libraryFolder = Paths.get(configLibPath);
        } else {
            libraryFolder = Paths.get(System.getProperty("user.home"), ".ssvm", "libs");
        }

        // Create library folder if it doesn't exist
        try {
            Files.createDirectories(libraryFolder);
        } catch (IOException e) {
            Skidfuscator.LOGGER.error("Failed to create library folder", e);
        }

        // Create session
        SkidfuscatorSession session = SkidfuscatorSession.builder()
                .input(new File(config.getInputPath()))
                .output(new File(config.getOutputPath()))
                .libs(config.getLibsPath().isEmpty()
                        ? libraryFolder.toFile().listFiles()
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