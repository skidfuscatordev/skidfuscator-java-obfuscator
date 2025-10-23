package dev.skidfuscator.obfuscator.gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import dev.skidfuscator.jvm.Jvm;
import dev.skidfuscator.obfuscator.gui.autosave.AutoSaveDocumentListener;
import dev.skidfuscator.obfuscator.gui.config.SkidfuscatorConfig;
import dev.skidfuscator.obfuscator.util.JdkDownloader;
import dev.skidfuscator.obfuscator.util.Observable;

import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ConfigPanel extends JPanel implements SkidPanel{
    private final JTextField inputField;
    private final JTextField outputField;
    private final JTextField libsField;
    private final JTextField runtimeField;
    private final JCheckBox debugBox;
    private final SkidfuscatorConfig config;
    private Observable<Boolean> runtimeInstalled = new Observable.SimpleObservable<>(
            JdkDownloader.isJdkDownloaded()
    );

    public ConfigPanel() {
        setLayout(new GridBagLayout());
        
        // Create compound border with titled border and empty border for padding
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                "Configuration", 
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 16)
            ),
            BorderFactory.createEmptyBorder(20, 0, 10, 0)
        ));
        // Add description panel at the top
        JTextArea descriptionArea = new JTextArea(
            "Configure your obfuscation settings below:\n\n" +
            "• Input JAR: Select the Java archive (.jar) file you want to obfuscate\n" +
            "• Output JAR: Choose where to save the obfuscated file (.jar, .apk, or .dex)\n" +
            "• Libraries: (Optional) Directory containing dependency JARs needed by your application\n" +
            "• Runtime: JDK runtime libraries required for compilation (auto-downloaded)\n" +
            "• Debug Mode: Enable additional logging and debugging information\n\n"
        );
        descriptionArea.setEditable(false);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(null);
        descriptionArea.setBorder(null);

        // Create indicator panel
        JPanel indicatorPanel = new JPanel();
        indicatorPanel.setLayout(new BoxLayout(indicatorPanel, BoxLayout.Y_AXIS));
        indicatorPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Add indicators with colored symbols
        JLabel validLabel = new JLabel("✓ Green checkmarks indicate valid configurations");
        validLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validLabel.setForeground(new Color(0x2ECC40));

        JLabel errorLabel = new JLabel("✗ Red X marks indicate issues that need to be resolved");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(0xFF4136));

        JLabel optionalLabel = new JLabel("● Orange dots indicate optional fields");
        optionalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        optionalLabel.setForeground(new Color(0xFF851B));

        indicatorPanel.add(validLabel);
        indicatorPanel.add(Box.createVerticalStrut(5));
        indicatorPanel.add(errorLabel);
        indicatorPanel.add(Box.createVerticalStrut(5));
        indicatorPanel.add(optionalLabel);

        // Add components to panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 20;
        gbc.gridwidth = 3;
        add(descriptionArea, gbc);

        gbc.gridy = -1;
        gbc.insets = new Insets(0, 5, 20, 5);
        add(indicatorPanel, gbc);

        // Reset gridwidth for other components
        gbc.gridwidth = 1;
        gbc.gridy++;

        // Load configuration
        config = SkidfuscatorConfig.load();

        // Input file
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Input JAR:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // IMPORTANTE: el campo se expande
        gbc.fill = GridBagConstraints.HORIZONTAL; // IMPORTANTE: llena horizontal
        inputField = new JTextField(30);
        JLabel inputCheck = new JLabel("✗");
        inputCheck.setForeground(new Color(255, 65, 54));
        DocumentListener inputListener = new DocumentListener() {
            private void updateCheck() {
                boolean valid = new File(inputField.getText()).exists();
                inputCheck.setText(valid ? "✓" : "✗");
                inputCheck.setForeground(valid ? new Color(46, 204, 64) : new Color(255, 65, 54));
                System.out.println("Input valid: " + valid);
                config.getValidInput().set(valid);

                if (!valid) {
                    StringBuilder tooltip = new StringBuilder("<html><body style='width: 250px; padding: 3px; background-color: #FFF3CD; border: 2px solid #FFE69C; border-radius: 4px'>");
                    tooltip.append("<div style='color: #856404; font-weight: bold; margin-bottom: 5px'>⚠ Warning: Invalid Input Configuration</div>");
                    tooltip.append("<div style='color: #664D03; margin: 3px 0'>Input file does not exist</div>");
                    tooltip.append("</body></html>");
                    
                    ToolTipManager.sharedInstance().setInitialDelay(0);
                    ToolTipManager.sharedInstance().setDismissDelay(10000);
                    inputField.setToolTipText(tooltip.toString());
                } else {
                    inputField.setToolTipText(null);
                }
            }
            public void insertUpdate(DocumentEvent e) { updateCheck(); }
            public void removeUpdate(DocumentEvent e) { updateCheck(); }
            public void changedUpdate(DocumentEvent e) { updateCheck(); }
        };
        inputField.getDocument().addDocumentListener(inputListener);
        if (config.getLastInputPath() != null) {
            inputField.setText(config.getLastInputPath());
            inputListener.insertUpdate(null);  // Trigger initial validation
        }
        add(inputField, gbc);
        gbc.gridx = 2;
        JPanel inputButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        inputButtonPanel.setPreferredSize(new Dimension(150, 30));
        JButton inputBrowseButton = createBrowseButton(inputField, false);
        inputButtonPanel.add(inputBrowseButton);
        inputButtonPanel.add(Box.createHorizontalGlue());  // Push label to right
        inputButtonPanel.add(inputCheck);
        add(inputButtonPanel, gbc);

        // Output file
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Output JAR:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // IMPORTANTE: el campo se expande
        gbc.fill = GridBagConstraints.HORIZONTAL; // IMPORTANTE
        outputField = new JTextField(30);
        JLabel outputCheck = new JLabel("✗");
        outputCheck.setForeground(new Color(255, 65, 54));
        DocumentListener outputListener = new DocumentListener() {
            private void updateCheck() {
                final String output = outputField.getText();
                File parent = new File(output).getParentFile();

                // [condition] must be valid file extension
                final boolean validEnd = output.endsWith(".jar")
                        || output.endsWith(".apk")
                        || output.endsWith(".dex");

                // [condition] must not be input
                final boolean validInput = !inputField.getText().equals(output);

                boolean valid = parent != null && parent.exists() && validEnd && validInput;
                outputCheck.setText(valid ? "✓" : "✗");
                outputCheck.setForeground(valid
                        ? new Color(46, 204, 64)
                        : new Color(255, 65, 54)
                );
                // Set tooltip explaining validation failure
                StringBuilder tooltip = new StringBuilder("<html><body style='width: 250px; padding: 3px; background-color: #FFF3CD; border: 2px solid #FFE69C; border-radius: 4px'>");
                tooltip.append("<div style='color: #856404; font-weight: bold; margin-bottom: 5px'>⚠ Warning: Invalid Output Configuration</div>");
                
                if (parent == null || !parent.exists()) {
                    tooltip.append("<div style='color: #664D03; margin: 3px 0'>Output directory does not exist</div>");
                }
                if (!validEnd) {
                    tooltip.append("<div style='color: #664D03; margin: 3px 0'>File must end with .jar, .apk or .dex</div>");
                }
                if (!validInput) {
                    tooltip.append("<div style='color: #664D03; margin: 3px 0'>Output file cannot be the same as input file</div>");
                }
                tooltip.append("</body></html>");
                config.getValidOutput().set(validInput);
                
                if (!valid) {
                    ToolTipManager.sharedInstance().setInitialDelay(0);
                    ToolTipManager.sharedInstance().setDismissDelay(10000);

                    outputField.setToolTipText(tooltip.toString());
                } else {
                    outputField.setToolTipText(null);
                }
            }
            public void insertUpdate(DocumentEvent e) { updateCheck(); }
            public void removeUpdate(DocumentEvent e) { updateCheck(); }
            public void changedUpdate(DocumentEvent e) { updateCheck(); }
        };
        outputField.getDocument().addDocumentListener(outputListener);
        if (config.getLastOutputPath() != null) {
            outputField.setText(config.getLastOutputPath());
            outputListener.insertUpdate(null);
        } else if (config.getLastInputPath() != null || inputField.getText() != null) {
            final String input = config.getLastInputPath() != null
                    ? config.getLastInputPath()
                    : inputField.getText();

            outputField.setText(input.replace(".jar", "-obf.jar"));
            outputListener.insertUpdate(null);
        } else {
            config.getValidOutput().set(false);
        }
        add(outputField, gbc);
        gbc.gridx = 2;
        JPanel outputButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        outputButtonPanel.setPreferredSize(new Dimension(150, 30));
        JButton outputBrowseButton = createBrowseButton(outputField, false);
        outputButtonPanel.add(outputBrowseButton);
        outputButtonPanel.add(Box.createHorizontalGlue());
        outputButtonPanel.add(outputCheck);
        add(outputButtonPanel, gbc);

        // Libraries
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Libraries:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // IMPORTANTE: el campo se expande
        gbc.fill = GridBagConstraints.HORIZONTAL; // IMPORTANTE
        libsField = new JTextField(30);
        JLabel libsCheck = new JLabel("✗");
        libsCheck.setForeground(new Color(255, 65, 54));
        DocumentListener libsListener = new DocumentListener() {
            private void updateCheck() {
                boolean valid;
                if (libsField.getText().isEmpty()) {
                    valid = true; // Empty is valid
                    // Set orange dot for pending state
                    libsCheck.setText("●");
                    libsCheck.setForeground(new Color(255, 140, 0)); // Orange color
                    return;
                } else {
                    File dir = new File(libsField.getText());
                    valid = dir.exists() && dir.isDirectory();
                }
                libsCheck.setText(valid ? "✓" : "✗");
                libsCheck.setForeground(valid ? new Color(46, 204, 64) : new Color(255, 65, 54));
            }
            public void insertUpdate(DocumentEvent e) { updateCheck(); }
            public void removeUpdate(DocumentEvent e) { updateCheck(); }
            public void changedUpdate(DocumentEvent e) { updateCheck(); }
        };
        libsField.getDocument().addDocumentListener(libsListener);
        if (config.getLastLibsPath() != null) {
            libsField.setText(config.getLastLibsPath());
            libsListener.insertUpdate(null);
        }
        add(libsField, gbc);
        gbc.gridx = 2;
        JPanel libsButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        libsButtonPanel.setPreferredSize(new Dimension(150, 30));
        JButton libsBrowseButton = createBrowseButton(libsField, true);
        libsButtonPanel.add(libsBrowseButton);
        libsButtonPanel.add(Box.createHorizontalGlue());
        libsButtonPanel.add(libsCheck);
        add(libsButtonPanel, gbc);

        // Runtime
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Runtime:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0; // IMPORTANTE: el campo se expande
        gbc.fill = GridBagConstraints.HORIZONTAL; // IMPORTANTE
        runtimeField = new JTextField(30);
        
        // Check if JDK was previously downloaded
        try {
            String jmodPath = JdkDownloader.getCachedJmodPath();
            runtimeField.setText(jmodPath);
            runtimeField.setEnabled(!JdkDownloader.isJdkDownloaded());
            runtimeInstalled.set(JdkDownloader.isJdkDownloaded());
        } catch (IOException e) {
            // Fallback to config
            if (config.getLastRuntimePath() != null) {
                if (config.getLastRuntimePath().isEmpty()) {
                    runtimeField.setText(Jvm.getLibsPath());
                    runtimeField.setEnabled(false);
                    runtimeInstalled.set(true);
                } else {
                    runtimeField.setText(config.getLastRuntimePath());
                }
            }
        }

        add(runtimeField, gbc);
        
        // Add download button next to browse button
        gbc.gridx = 2;
        JPanel runtimeButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel downloadCheck = new JLabel("✗");
        downloadCheck.setForeground(new Color(255, 65, 54));

        JButton downloadButton = new JButton("Install");
        runtimeButtonPanel.setPreferredSize(new Dimension(150, 30));

        // Set initial button state based on JDK download status
        if (JdkDownloader.isJdkDownloaded()) {
            downloadButton.setText("Installed");
            downloadButton.setEnabled(false);
            downloadCheck.setText("✓");
            downloadCheck.setForeground(new Color(46, 204, 64));
            runtimeInstalled.set(true);
        }
        
        downloadButton.addActionListener(e -> {
            downloadButton.setEnabled(false);
            downloadButton.setText("Downloading...");
            
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() throws Exception {
                    return JdkDownloader.getJmodPath();
                }

                @Override
                protected void done() {
                    try {
                        String path = get();
                        runtimeField.setText(path);
                        runtimeField.setEnabled(false);
                        downloadButton.setText("Installed");
                        downloadButton.setEnabled(false);
                        downloadCheck.setText("✓");
                        downloadCheck.setForeground(new Color(46, 204, 64));
                        runtimeInstalled.set(true);

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            ConfigPanel.this,
                            "Failed to download JDK: " + ex.getMessage(),
                            "Download Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        downloadButton.setText("Install");
                        downloadButton.setEnabled(true);
                        downloadCheck.setText("✗");
                        downloadCheck.setForeground(new Color(255, 65, 54));
                        runtimeInstalled.set(false);
                    }
                }
            };
            worker.execute();
        });
        runtimeButtonPanel.add(downloadButton);
        runtimeButtonPanel.add(Box.createHorizontalGlue());
        runtimeButtonPanel.add(downloadCheck);

        // removing for now
        //runtimeButtonPanel.add(createBrowseButton(runtimeField, false));
        add(runtimeButtonPanel, gbc);

        // Checkboxes
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        debugBox = new JCheckBox("Debug Mode");
        //phantomBox = new JCheckBox("Use Phantom");
        debugBox.setSelected(config.isDebugEnabled());
        //phantomBox.setSelected(config.isPhantomEnabled());
        checkboxPanel.add(debugBox);
        //checkboxPanel.add(phantomBox);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 3;
        add(checkboxPanel, gbc);

        // Add save button
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveConfiguration());
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(saveButton, gbc);

        // Add automatic save on field changes
        setupAutoSave();
    }

    private JButton createBrowseButton(JTextField field, boolean isDirectory) {
        JButton button = new JButton("Browse");
        button.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(field.getText() == null
                    ? config.getLastDirectory()
                    : field.getText()
            );
            if (isDirectory) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
                config.setLastDirectory(chooser.getCurrentDirectory().getAbsolutePath());
                saveConfiguration();
            }
        });
        return button;
    }

    private void setupAutoSave() {
        // Add document listeners to all text fields
        inputField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        outputField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        libsField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        runtimeField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));

        // Add action listeners to checkboxes
        debugBox.addActionListener(e -> saveConfiguration());
    }

    private void saveConfiguration() {
        SwingUtilities.invokeLater(() -> {
            SkidfuscatorConfig newConfig = new SkidfuscatorConfig.Builder()
                    .setLastInputPath(inputField.getText())
                    .setLastOutputPath(outputField.getText())
                    .setLastLibsPath(libsField.getText())
                    .setLastRuntimePath(runtimeField.getText())
                    .setDebugEnabled(debugBox.isSelected())
                    .setLastDirectory(config.getLastDirectory())
                    .build();
            newConfig.save();
        });
    }

    // Getters
    public String getInputPath() { return inputField.getText(); }
    public String getOutputPath() { return outputField.getText(); }
    public String getLibsPath() { return libsField.getText(); }
    public String getRuntimePath() { return runtimeField.getText(); }
    public boolean isDebugEnabled() { return debugBox.isSelected(); }
    public String getLibraryPath() {
        // TODO: Add a library path field to the config panel
        return null;
    }

    public Observable<Boolean> getRuntimeInstalled() {
        return runtimeInstalled;
    }

    public SkidfuscatorConfig getConfig() {
        return config;
    }
}

