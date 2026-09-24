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

import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ConfigPanel extends JPanel implements SkidPanel {

    private static final String CHECK_LABEL = "checkLabel";
    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JTextField libsField = new JTextField();
    private final JTextField runtimeField = new JTextField();
    private final JCheckBox debugBox = new JCheckBox("Debug Mode");
    // Load configuration
    private static final SkidfuscatorConfig CONFIG = SkidfuscatorConfig.load();
    private final Observable<Boolean> runtimeInstalled = new Observable.SimpleObservable<>(
            JdkDownloader.isJdkDownloaded()
    );

    private static final int MARGIN_HORIZONTAL = 40;  // Margen izquierdo y derecho
    private static final int MARGIN_VERTICAL = 10;    // Espaciado vertical entre componentes
    private static final int LABEL_WIDTH = 100;       // Ancho fijo para las etiquetas
    private static final int BUTTON_PANEL_WIDTH = 150; // Ancho fijo para el panel de botones
    private static final String SEGOE_UI = "Segoe UI";

    public ConfigPanel() {
        setLayout(new GridBagLayout());
        // Create compound border with titled border and empty border for padding
        setBorder(createBorder());
        // Input file
        int currentRow = 0;
        addConfigRow(currentRow++, "Input JAR:", inputField, createInputValidation(), false);
        // Output file
        addConfigRow(currentRow++, "Output JAR:", outputField, createOutputValidation(), false);
        // Libraries
        addConfigRow(currentRow++, "Libraries:", libsField, createLibsValidation(), true);
        addRuntimeRow(currentRow++);
        addCheckboxRow(currentRow++);
        addSaveButtonRow(currentRow++);
        createDescriptionAreaAndIndicatorPanel(currentRow++);
        setupAutoSave();
    }

    private CompoundBorder createBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                        "Configuration",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font(SEGOE_UI, Font.BOLD, 16)
                ),
                BorderFactory.createEmptyBorder(20, 0, 10, 0)
        );
    }

    /**
     * Crea las constraints base con configuración común
     */
    private GridBagConstraints createBaseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    /**
     * Crea constraints para las etiquetas
     */
    private GridBagConstraints createLabelConstraints(int row) {
        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.insets = new Insets(MARGIN_VERTICAL, MARGIN_HORIZONTAL, MARGIN_VERTICAL, 10);
        gbc.fill = GridBagConstraints.NONE;
        return gbc;
    }

    /**
     * Crea constraints para los campos de texto
     */
    private GridBagConstraints createFieldConstraints(int row) {
        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(MARGIN_VERTICAL, 0, MARGIN_VERTICAL, 10);
        return gbc;
    }

    /**
     * Crea constraints para el panel de botones
     */
    private GridBagConstraints createButtonPanelConstraints(int row) {
        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(MARGIN_VERTICAL, 0, MARGIN_VERTICAL, MARGIN_HORIZONTAL);
        return gbc;
    }

    /**
     * Añade una fila de configuración completa
     */
    private void addConfigRow(int row, String labelText, JTextField field,
                              DocumentListener validator, boolean isDirectory) {
        // Label
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 25));
        add(label, createLabelConstraints(row));

        // Field
        field.getDocument().addDocumentListener(validator);
        add(field, createFieldConstraints(row));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setPreferredSize(new Dimension(BUTTON_PANEL_WIDTH, 35));

        JButton browseButton = createBrowseButton(field, isDirectory);
        JLabel checkLabel = new JLabel("✗");
        checkLabel.setForeground(new Color(255, 65, 54));

        // Store check label in field's client property for validator access
        field.putClientProperty(CHECK_LABEL, checkLabel);

        buttonPanel.add(browseButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(checkLabel);

        add(buttonPanel, createButtonPanelConstraints(row));

        // Load saved value if exists
        loadSavedValue(field, validator);
    }

    /**
     * Añade la fila de runtime con botón de instalación
     */
    private void addRuntimeRow(int row) {
        // Label
        JLabel label = new JLabel("Runtime:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 25));
        add(label, createLabelConstraints(row));

        // Field
        add(runtimeField, createFieldConstraints(row));

        // Button panel with install button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setPreferredSize(new Dimension(BUTTON_PANEL_WIDTH, 35));

        JLabel downloadCheck = new JLabel("✗");
        downloadCheck.setForeground(new Color(255, 65, 54));

        JButton downloadButton = new JButton("Install");

        // Initialize runtime field
        initializeRuntimeField(downloadButton, downloadCheck);

        downloadButton.addActionListener(e -> handleRuntimeDownload(downloadButton, downloadCheck));

        buttonPanel.add(downloadButton);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(downloadCheck);

        add(buttonPanel, createButtonPanelConstraints(row));
    }

    /**
     * Añade la fila de checkboxes
     */
    private void addCheckboxRow(int row) {
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        debugBox.setSelected(CONFIG.isDebugEnabled());
        checkboxPanel.add(debugBox);

        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(MARGIN_VERTICAL, MARGIN_HORIZONTAL, MARGIN_VERTICAL, MARGIN_HORIZONTAL);
        add(checkboxPanel, gbc);
    }

    /**
     * Añade la fila del botón de guardado
     */
    private void addSaveButtonRow(int row) {
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(e -> saveConfiguration());

        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(MARGIN_VERTICAL, MARGIN_HORIZONTAL, MARGIN_VERTICAL, MARGIN_HORIZONTAL);
        add(saveButton, gbc);
    }

    private void createDescriptionAreaAndIndicatorPanel(int currentRow) {
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
        descriptionArea.setFont(new Font(SEGOE_UI, Font.PLAIN, 12));
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
        validLabel.setFont(new Font(SEGOE_UI, Font.PLAIN, 12));
        validLabel.setForeground(new Color(0x2ECC40));

        JLabel errorLabel = new JLabel("✗ Red X marks indicate issues that need to be resolved");
        errorLabel.setFont(new Font(SEGOE_UI, Font.PLAIN, 12));
        errorLabel.setForeground(new Color(0xFF4136));

        JLabel optionalLabel = new JLabel("● Orange dots indicate optional fields");
        optionalLabel.setFont(new Font(SEGOE_UI, Font.PLAIN, 12));
        optionalLabel.setForeground(new Color(0xFF851B));

        indicatorPanel.add(validLabel);
        indicatorPanel.add(Box.createVerticalStrut(5));
        indicatorPanel.add(errorLabel);
        indicatorPanel.add(Box.createVerticalStrut(5));
        indicatorPanel.add(optionalLabel);

        // Add description and indicators at the end
        GridBagConstraints gbc = createBaseConstraints();
        gbc.gridx = 0;
        gbc.gridy = currentRow++;
        gbc.gridwidth = 3;
        gbc.weighty = 0;  // Don't expand vertically
        gbc.insets = new Insets(20, MARGIN_HORIZONTAL, 0, MARGIN_HORIZONTAL);
        add(descriptionArea, gbc);

        gbc.gridy = currentRow;
        gbc.insets = new Insets(0, MARGIN_HORIZONTAL, 10, MARGIN_HORIZONTAL);
        add(indicatorPanel, gbc);
    }

    private DocumentListener createInputValidation() {
        return new DocumentListener() {
            private void updateCheck() {
                JLabel checkLabel = (JLabel) inputField.getClientProperty(CHECK_LABEL);
                boolean valid = new File(inputField.getText()).exists();

                checkLabel.setText(valid ? "✓" : "✗");
                checkLabel.setForeground(valid ? new Color(46, 204, 64) : new Color(255, 65, 54));
                CONFIG.getValidInput().set(valid);

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
    }

    private DocumentListener createOutputValidation() {
        return new DocumentListener() {
            private void updateCheck() {
                JLabel checkLabel = (JLabel) outputField.getClientProperty(CHECK_LABEL);
                final String output = outputField.getText();
                File parent = new File(output).getParentFile();

                final boolean validEnd = output.endsWith(".jar")
                        || output.endsWith(".apk")
                        || output.endsWith(".dex");
                final boolean validInput = !inputField.getText().equals(output);

                boolean valid = parent != null && parent.exists() && validEnd && validInput;
                checkLabel.setText(valid ? "✓" : "✗");
                checkLabel.setForeground(valid ? new Color(46, 204, 64) : new Color(255, 65, 54));

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
                CONFIG.getValidOutput().set(validInput);

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
    }

    private DocumentListener createLibsValidation() {
        return new DocumentListener() {
            private void updateCheck() {
                JLabel checkLabel = (JLabel) libsField.getClientProperty(CHECK_LABEL);
                boolean valid;
                if (libsField.getText().isEmpty()) {
                    valid = true;
                    checkLabel.setText("●");
                    checkLabel.setForeground(new Color(255, 140, 0));
                    return;
                } else {
                    File dir = new File(libsField.getText());
                    valid = dir.exists() && dir.isDirectory();
                }
                checkLabel.setText(valid ? "✓" : "✗");
                checkLabel.setForeground(valid ? new Color(46, 204, 64) : new Color(255, 65, 54));
            }
            public void insertUpdate(DocumentEvent e) { updateCheck(); }
            public void removeUpdate(DocumentEvent e) { updateCheck(); }
            public void changedUpdate(DocumentEvent e) { updateCheck(); }
        };
    }

    private void loadSavedValue(JTextField field, DocumentListener validator) {
        String savedValue = null;

        if (field == inputField) {
            savedValue = CONFIG.getLastInputPath();
        } else if (field == outputField) {
            savedValue = CONFIG.getLastOutputPath();
            if (savedValue == null && CONFIG.getLastInputPath() != null) {
                savedValue = CONFIG.getLastInputPath().replace(".jar", "-obf.jar");
            }
        } else if (field == libsField) {
            savedValue = CONFIG.getLastLibsPath();
        }

        if (savedValue != null) {
            field.setText(savedValue);
            validator.insertUpdate(null);
        }
    }

    private void initializeRuntimeField(JButton downloadButton, JLabel downloadCheck) {
        try {
            String jmodPath = JdkDownloader.getCachedJmodPath();
            runtimeField.setText(jmodPath);
            runtimeField.setEnabled(!JdkDownloader.isJdkDownloaded());
            runtimeInstalled.set(JdkDownloader.isJdkDownloaded());

            if (JdkDownloader.isJdkDownloaded()) {
                downloadButton.setText("Installed");
                downloadButton.setEnabled(false);
                downloadCheck.setText("✓");
                downloadCheck.setForeground(new Color(46, 204, 64));
            }
        } catch (IOException e) {
            if (CONFIG.getLastRuntimePath() != null) {
                if (CONFIG.getLastRuntimePath().isEmpty()) {
                    runtimeField.setText(Jvm.getLibsPath());
                    runtimeField.setEnabled(false);
                    runtimeInstalled.set(true);
                } else {
                    runtimeField.setText(CONFIG.getLastRuntimePath());
                }
            }
        }
    }

    private void handleRuntimeDownload(JButton downloadButton, JLabel downloadCheck) {
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
    }

    private JButton createBrowseButton(JTextField field, boolean isDirectory) {
        final JButton button = new JButton("Browse");
        button.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(field.getText() == null
                    ? CONFIG.getLastDirectory()
                    : field.getText()
            );
            if (isDirectory) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
                CONFIG.setLastDirectory(chooser.getCurrentDirectory().getAbsolutePath());
                saveConfiguration();
            }
        });
        return button;
    }

    private void setupAutoSave() {
        inputField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        outputField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        libsField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
        runtimeField.getDocument().addDocumentListener(new AutoSaveDocumentListener(this::saveConfiguration));
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
                    .setLastDirectory(CONFIG.getLastDirectory())
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
    public String getLibraryPath() { return null; }
    public Observable<Boolean> getRuntimeInstalled() { return runtimeInstalled; }
    public SkidfuscatorConfig getConfig() { return CONFIG; }
}