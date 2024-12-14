package dev.skidfuscator.obfuscator.gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import dev.skidfuscator.jvm.Jvm;
import dev.skidfuscator.obfuscator.gui.autosave.AutoSaveDocumentListener;
import dev.skidfuscator.obfuscator.gui.config.SkidfuscatorConfig;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConfigPanel extends JPanel {
    private final JTextField inputField;
    private final JTextField outputField;
    private final JTextField libsField;
    private final JTextField runtimeField;
    private final JCheckBox debugBox;
    private final SkidfuscatorConfig config;

    public ConfigPanel() {
        setLayout(new GridBagLayout());

        // Load configuration
        config = SkidfuscatorConfig.load();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Input file
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Input JAR:"), gbc);
        gbc.gridx = 1;
        inputField = new JTextField(30);
        if (config.getLastInputPath() != null) {
            inputField.setText(config.getLastInputPath());
        }
        add(inputField, gbc);
        gbc.gridx = 2;
        add(createBrowseButton(inputField, false), gbc);

        // Output file
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Output JAR:"), gbc);
        gbc.gridx = 1;
        outputField = new JTextField(30);
        if (config.getLastOutputPath() != null) {
            outputField.setText(config.getLastOutputPath());
        } else if (config.getLastInputPath() != null) {
            outputField.setText(config.getLastInputPath().replace(".jar", "-obf.jar"));
        }
        add(outputField, gbc);
        gbc.gridx = 2;
        add(createBrowseButton(outputField, false), gbc);

        // Libraries
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Libraries:"), gbc);
        gbc.gridx = 1;
        libsField = new JTextField(30);
        if (config.getLastLibsPath() != null) {
            libsField.setText(config.getLastLibsPath());
        }
        add(libsField, gbc);
        gbc.gridx = 2;
        add(createBrowseButton(libsField, true), gbc);

        // Runtime
        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Runtime:"), gbc);
        gbc.gridx = 1;
        runtimeField = new JTextField(30);
        if (config.getLastRuntimePath() != null) {
            if (config.getLastRuntimePath().isEmpty()) {
                runtimeField.setText(Jvm.getLibsPath());
                runtimeField.setEnabled(false);
            } else {
                runtimeField.setText(config.getLastRuntimePath());
            }
        }
        add(runtimeField, gbc);
        gbc.gridx = 2;
        add(createBrowseButton(runtimeField, false), gbc);

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
}

