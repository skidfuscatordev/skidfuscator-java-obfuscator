package dev.skidfuscator.obfuscator.gui;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import dev.skidfuscator.obfuscator.config.SkidfuscatorConfig;
import dev.skidfuscator.obfuscator.gui.transformer.TransformerOptionDefinition;
import dev.skidfuscator.obfuscator.gui.transformer.TransformerOptionType;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class TransformerPanel extends JPanel {
    private final Map<String, TransformerSection> transformerSections;
    private final JButton saveConfigButton;
    private final JButton loadConfigButton;
    private final File defaultConfigFile = new File("skidfuscator-config.conf");

    public TransformerPanel() {
        setLayout(new BorderLayout(10, 10));

        // Create transformer sections panel
        JPanel sectionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        transformerSections = new HashMap<>();

        // Initialize transformer sections with their options
        initializeTransformerSections(sectionsPanel);

        // Add sections to a scrollable panel
        JScrollPane scrollPane = new JScrollPane(sectionsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        // Create buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveConfigButton = new JButton("Save Config");
        loadConfigButton = new JButton("Load Config");

        saveConfigButton.addActionListener(e -> saveConfiguration());
        loadConfigButton.addActionListener(e -> loadConfiguration());

        buttonPanel.add(loadConfigButton);
        buttonPanel.add(saveConfigButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initializeTransformerSections(JPanel panel) {
        // Define transformer sections with their options
        addTransformerSection(panel, "stringEncryption", "String Encryption", Arrays.asList(
                TransformerOptionDefinition.builder()
                        .key("type")
                        .label("Encryption Type")
                        .type(TransformerOptionType.ENUM)
                        .enumValues(Arrays.asList("STANDARD", "POLYMORPHIC"))
                        .defaultValue("STANDARD")
                        .description("Type of string encryption to apply")
                        .build()
        ), true, "Encrypts string constants in the bytecode using various obfuscation techniques. " +
                "Makes it harder to identify and modify important string values.");

        addTransformerSection(panel, "flowException", "Flow Exception", Arrays.asList(
                TransformerOptionDefinition.builder()
                        .key("strength")
                        .label("Strength")
                        .type(TransformerOptionType.ENUM)
                        .enumValues(Arrays.asList("WEAK", "GOOD", "AGGRESSIVE"))
                        .defaultValue("GOOD")
                        .description("Flow exception transformation strength")
                        .build()
        ), true, "Adds complex exception handling to the control flow. " +
                "Makes it more difficult to understand the program's logic through static analysis.");

        // Number Encryption transformer
        addTransformerSection(panel, "numberEncryption", "Number Encryption", Collections.emptyList(), true,
                "Encrypts numeric constants in the bytecode using mathematical transformations. " +
                        "Makes it harder to identify and modify important numeric values.");

        // Flow Condition transformer
        addTransformerSection(panel, "flowCondition", "Flow Condition", Collections.emptyList(), true,
                "Adds complex conditional statements and bogus branches to obscure the original control flow. " +
                        "Makes it more difficult to understand the program's logic through static analysis.");

        // Flow Range transformer
        addTransformerSection(panel, "flowRange", "Flow Range", Collections.emptyList(), true,
                "Implements range-based control flow obfuscation by splitting loops and iterative structures. " +
                        "Helps prevent accurate decompilation of loop constructs and iterations.");

        // Native transformer
        addTransformerSection(panel, "native", "Native", Collections.emptyList(), false,
                "Converts selected Java methods to native code implementations. " +
                        "Provides strongest protection but requires platform-specific compilation.");
    }

    private void addTransformerSection(JPanel panel, String id, String name,
                                       List<TransformerOptionDefinition> options,
                                       boolean defaultEnabled) {
        TransformerSection section = new TransformerSection(id, name, options, defaultEnabled, null);
        transformerSections.put(id, section);
        panel.add(section);
    }

    private void addTransformerSection(JPanel panel, String id, String name,
                                       List<TransformerOptionDefinition> options,
                                       boolean defaultEnabled,
                                       String description) {
        TransformerSection section = new TransformerSection(id, name, options, defaultEnabled, description);
        transformerSections.put(id, section);
        panel.add(section);
    }

    private void addSimpleTransformerSection(JPanel panel, String id, String name,
                                             boolean defaultEnabled,
                                             String description) {
        addTransformerSection(panel, id, name, Collections.emptyList(), defaultEnabled, description);
    }


    // Inner class representing a transformer section
    private static class TransformerSection extends JPanel {
        private final String id;
        private final JCheckBox enabledBox;
        private final List<TransformerOptionDefinition> options;
        private final Map<String, JComponent> optionComponents;
        private final JPanel optionsPanel;
        private final JButton toggleButton;
        private boolean optionsVisible = false;

        public TransformerSection(String id, String name,
                                  List<TransformerOptionDefinition> options,
                                  boolean defaultEnabled, String description) {
            this.id = id;
            this.options = options;
            this.optionComponents = new HashMap<>();

            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createEtchedBorder());

            // Create header panel with checkbox and toggle button
            JPanel headerPanel = new JPanel(new BorderLayout());
            enabledBox = new JCheckBox(name);
            enabledBox.setSelected(defaultEnabled);

            // Add tooltip with description if provided
            // Add description if provided
            if (description != null && !description.trim().isEmpty()) {
                JTextArea descriptionArea = new JTextArea(description);
                descriptionArea.setEditable(false);
                descriptionArea.setWrapStyleWord(true);
                descriptionArea.setLineWrap(true);
                descriptionArea.setBackground(getBackground());
                descriptionArea.setForeground(Color.GRAY);
                descriptionArea.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
                descriptionArea.setFont(UIManager.getFont("Label.font").deriveFont(11f));

                // Calculate preferred height based on content
                FontMetrics fm = descriptionArea.getFontMetrics(descriptionArea.getFont());
                int availableWidth = 400; // Adjust this value based on your panel width
                int lineHeight = fm.getHeight();

                // Create a temporary text area to calculate wrapped height
                JTextArea temp = new JTextArea(description);
                temp.setLineWrap(true);
                temp.setWrapStyleWord(true);
                temp.setSize(availableWidth, Integer.MAX_VALUE);
                int preferredHeight = Math.min(3 * lineHeight, temp.getPreferredSize().height);

                descriptionArea.setPreferredSize(new Dimension(availableWidth, preferredHeight));
                headerPanel.add(descriptionArea, BorderLayout.SOUTH);
            }

            headerPanel.add(enabledBox, BorderLayout.WEST);

            if (!options.isEmpty()) {
                toggleButton = new JButton("▼");
                toggleButton.setPreferredSize(new Dimension(50, 25));
                toggleButton.addActionListener(e -> toggleOptions());
                headerPanel.add(toggleButton, BorderLayout.EAST);
            } else {
                toggleButton = null;
            }

            add(headerPanel, BorderLayout.NORTH);

            // Create options panel
            if (!options.isEmpty()) {
                optionsPanel = new JPanel();
                optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
                optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));

                for (TransformerOptionDefinition option : options) {
                    JPanel optionPanel = createOptionPanel(option);
                    optionsPanel.add(optionPanel);
                }

                optionsPanel.setVisible(false);
                add(optionsPanel, BorderLayout.CENTER);
            } else {
                optionsPanel = null;
            }
        }

        private JPanel createOptionPanel(TransformerOptionDefinition option) {
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            JLabel label = new JLabel(option.getLabel() + ":");
            panel.add(label, BorderLayout.WEST);

            JComponent inputComponent;
            switch (option.getType()) {
                case ENUM:
                    JComboBox<String> comboBox = new JComboBox<>(
                            option.getEnumValues().toArray(new String[0])
                    );
                    comboBox.setSelectedItem(option.getDefaultValue());
                    inputComponent = comboBox;
                    break;

                case INTEGER:
                    JSpinner spinner = new JSpinner(
                            new SpinnerNumberModel(((Integer) option.getDefaultValue()).doubleValue(), 0, 100, 1)
                    );
                    inputComponent = spinner;
                    break;

                case BOOLEAN:
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected((Boolean)option.getDefaultValue());
                    inputComponent = checkBox;
                    break;

                default:
                case STRING:
                    JTextField textField = new JTextField(
                            option.getDefaultValue().toString(), 15
                    );
                    inputComponent = textField;
                    break;
            }

            // Add tooltip with description
            if (option.getDescription() != null) {
                label.setToolTipText(option.getDescription());
                inputComponent.setToolTipText(option.getDescription());
            }

            optionComponents.put(option.getKey(), inputComponent);
            panel.add(inputComponent, BorderLayout.CENTER);
            return panel;
        }

        private void toggleOptions() {
            optionsVisible = !optionsVisible;
            optionsPanel.setVisible(optionsVisible);
            toggleButton.setText(optionsVisible ? "▲" : "▼");
            revalidate();
            repaint();
        }

        public String getId() {
            return id;
        }

        public boolean isEnabled() {
            return enabledBox.isSelected();
        }

        public Map<String, Object> getOptionValues() {
            Map<String, Object> values = new HashMap<>();
            for (TransformerOptionDefinition option : options) {
                JComponent component = optionComponents.get(option.getKey());
                if (component instanceof JComboBox) {
                    values.put(option.getKey(), ((JComboBox<?>)component).getSelectedItem());
                } else if (component instanceof JSpinner) {
                    values.put(option.getKey(), ((JSpinner)component).getValue());
                } else if (component instanceof JCheckBox) {
                    values.put(option.getKey(), ((JCheckBox)component).isSelected());
                } else if (component instanceof JTextField) {
                    values.put(option.getKey(), ((JTextField)component).getText());
                }
            }
            return values;
        }

        public void setOptionValue(String key, Object value) {
            JComponent component = optionComponents.get(key);
            if (component instanceof JComboBox) {
                ((JComboBox<?>)component).setSelectedItem(value);
            } else if (component instanceof JSpinner) {
                ((JSpinner)component).setValue(value);
            } else if (component instanceof JCheckBox) {
                ((JCheckBox)component).setSelected((Boolean)value);
            } else if (component instanceof JTextField) {
                ((JTextField)component).setText(value.toString());
            }
        }
    }

    // Configuration save/load methods remain the same but updated to handle new option types
    public void saveConfiguration() {
        SkidfuscatorConfig config = new SkidfuscatorConfig();

        // Add global exemptions
        List<String> globalExempts = Arrays.asList(
                "class{^(?!(dev\\/skidfuscator)).*$}",
                "class{^jghost\\/}",
                "class{Dump}"
        );
        config.setGlobalExemptions(globalExempts);

        // Add transformer configurations
        for (TransformerSection section : transformerSections.values()) {
            Map<String, Object> options = section.getOptionValues();
            config.addTransformer(
                    section.getId(),
                    section.isEnabled(),
                    options,
                    Collections.emptyList()  // Exemptions handled globally
            );
        }

        try (FileWriter writer = new FileWriter(defaultConfigFile)) {
            writer.write(config.renderConfig());
            JOptionPane.showMessageDialog(this,
                    "Configuration saved successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadConfiguration() {
        if (!defaultConfigFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "No configuration file found at " + defaultConfigFile.getAbsolutePath(),
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Config config = ConfigFactory.parseFile(defaultConfigFile);

            for (TransformerSection section : transformerSections.values()) {
                String id = section.getId();
                if (config.hasPath(id)) {
                    Config transformerConfig = config.getConfig(id);
                    section.setEnabled(transformerConfig.getBoolean("enabled"));

                    // Load additional options if they exist
                    if (transformerConfig.hasPath("options")) {
                        Config options = transformerConfig.getConfig("options");
                        for (Map.Entry<String, ConfigValue> entry : options.entrySet()) {
                            section.setOptionValue(entry.getKey(), entry.getValue().unwrapped().toString());
                        }
                    }
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Configuration loaded successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
