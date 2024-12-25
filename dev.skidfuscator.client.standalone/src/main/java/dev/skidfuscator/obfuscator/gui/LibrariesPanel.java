package dev.skidfuscator.obfuscator.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.util.MapleJarUtil;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import dev.skidfuscator.jghost.GhostHelper;
import dev.skidfuscator.jghost.tree.GhostLibrary;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import dev.skidfuscator.obfuscator.util.JdkDownloader;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.net.URLEncoder;

public class LibrariesPanel extends JPanel {
    private final JList<String> libraryList;
    private final DefaultListModel<String> libraryModel;
    private final JList<String> missingClassesList;
    private final DefaultListModel<String> missingClassesModel;
    private final JButton scanButton;
    private final JButton scanSelectedButton;
    private final Path libraryFolder;
    private SkidApplicationClassSource classSource;
    private final Gson gson;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final ConfigPanel configPanel;

    public LibrariesPanel(ConfigPanel configPanel, SkidApplicationClassSource classSource) {
        this.configPanel = configPanel;
        this.classSource = classSource;
        this.gson = new Gson();
        
        // Initialize library folder
        String configLibPath = configPanel.getLibraryPath();
        if (configLibPath != null && !configLibPath.isEmpty()) {
            this.libraryFolder = Paths.get(configLibPath);
        } else {
            this.libraryFolder = Paths.get(System.getProperty("user.home"), ".ssvm", "libs");
        }
        
        // Create library folder if it doesn't exist
        try {
            Files.createDirectories(libraryFolder);
        } catch (IOException e) {
            Skidfuscator.LOGGER.error("Failed to create library folder", e);
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
                        "Libraries",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        new Font("Segoe UI", Font.BOLD, 16)
                ),
                BorderFactory.createEmptyBorder(20, 10, 10, 10)
        ));

        // Create library list panel
        JPanel libraryListPanel = new JPanel(new BorderLayout());
        libraryListPanel.setBorder(BorderFactory.createTitledBorder("Current Libraries"));
        libraryModel = new DefaultListModel<>();
        libraryList = new JList<>(libraryModel);
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane libraryScrollPane = new JScrollPane(libraryList);
        libraryListPanel.add(libraryScrollPane, BorderLayout.CENTER);

        // Create missing classes panel with side panel
        JPanel missingClassesPanel = new JPanel(new BorderLayout());
        missingClassesPanel.setBorder(BorderFactory.createTitledBorder("Missing Classes"));
        missingClassesModel = new DefaultListModel<>();
        missingClassesList = new JList<>(missingClassesModel);
        missingClassesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane missingScrollPane = new JScrollPane(missingClassesList);
        missingClassesPanel.add(missingScrollPane, BorderLayout.CENTER);

        // Create side panel for selected class actions
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        sidePanel.setPreferredSize(new Dimension(120, 0));

        scanSelectedButton = new JButton("Scan Class");
        scanSelectedButton.setEnabled(false);
        scanSelectedButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        scanSelectedButton.addActionListener(e -> {
            String selectedClass = missingClassesList.getSelectedValue();
            if (selectedClass != null) {
                searchMavenCentral(selectedClass, scanSelectedButton);
            }
        });

        // Add selection listener to enable/disable scan button
        missingClassesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                scanSelectedButton.setEnabled(missingClassesList.getSelectedValue() != null);
            }
        });

        sidePanel.add(Box.createVerticalGlue());
        sidePanel.add(scanSelectedButton);
        sidePanel.add(Box.createVerticalGlue());

        missingClassesPanel.add(sidePanel, BorderLayout.EAST);

        // Create status panel
        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(statusLabel, BorderLayout.SOUTH);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        scanButton = new JButton("Scan JAR");
        scanButton.addActionListener(this::onScanButtonClicked);
        buttonPanel.add(scanButton);

        // Create split pane for lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, libraryListPanel, missingClassesPanel);
        splitPane.setResizeWeight(0.5);

        // Create bottom panel for status and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        // Add components to panel
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.setForeground(error ? new Color(255, 65, 54) : new Color(46, 204, 64));
    }

    private void refreshLibraryList() {
        libraryModel.clear();
        try {
            Files.list(libraryFolder)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .forEach(libraryModel::addElement);
        } catch (IOException e) {
            setStatus("Error refreshing library list: " + e.getMessage(), true);
        }
    }

    private void onScanButtonClicked(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
            }

            public String getDescription() {
                return "JAR files (*.jar)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            scanButton.setEnabled(false);
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            setStatus("Scanning JAR file...", false);

            SwingWorker<List<String>, String> worker = new SwingWorker<>() {
                @Override
                protected void done() {
                    try {
                        List<String> missingClasses = get();
                        missingClassesModel.clear();
                        for (String missingClass : missingClasses) {
                            missingClassesModel.addElement(missingClass);
                        }
                        setStatus("Found " + missingClasses.size() + " missing classes", false);
                    } catch (Exception ex) {
                        setStatus("Error scanning JAR: " + ex.getMessage(), true);
                    } finally {
                        scanButton.setEnabled(true);
                        progressBar.setVisible(false);
                    }

                }

                @Override
                protected void process(List<String> chunks) {
                    // Update status with the latest message
                    if (!chunks.isEmpty()) {
                        setStatus(chunks.get(chunks.size() - 1), false);
                    }
                }

                @Override
                protected List<String> doInBackground() throws Exception {
                    publish("Initializing Skidfuscator...");
                    Skidfuscator skidfuscator = new Skidfuscator(SkidfuscatorSession.builder().build());

                    publish("Importing JVM classes...");
                    final Set<LibraryClassSource> sources = skidfuscator._importJvm();

                    publish("Analyzing JAR file...");
                    classSource = new SkidApplicationClassSource(
                            selectedFile.getName(),
                            false,
                            MapleJarUtil.importJar(selectedFile, skidfuscator).getJarContents(),
                            skidfuscator
                    );
                    classSource.addLibraries(sources.toArray(new LibraryClassSource[0]));

                    publish("Verifying class tree...");
                    try {
                        classSource.getClassTree().verify();
                    } catch (Exception ex) {
                        // Ignore verification errors as we want to find missing classes
                    }

                    return classSource.getClassTree().getMissingClasses();
                }
            };
            worker.execute();
        }
    }

    private void searchMavenCentral(String className, JButton sourceButton) {
        sourceButton.setEnabled(false);
        progressBar.setVisible(true);
        setStatus("Searching Maven Central for " + className + "...", false);

        SwingWorker<List<MavenArtifact>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MavenArtifact> doInBackground() throws Exception {
                String searchUrl = "https://search.maven.org/solrsearch/select?q=fc:" + URLEncoder.encode(className, StandardCharsets.UTF_8) +
                        "&rows=20&wt=json&core=gav";

                HttpURLConnection connection = (HttpURLConnection) new URL(searchUrl).openConnection();
                connection.setRequestMethod("GET");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    JsonObject response = gson.fromJson(reader, JsonObject.class);
                    JsonObject responseObj = response.getAsJsonObject("response");
                    JsonArray docs = responseObj.getAsJsonArray("docs");

                    List<MavenArtifact> artifacts = new ArrayList<>();
                    for (JsonElement doc : docs) {
                        JsonObject artifact = doc.getAsJsonObject();
                        artifacts.add(new MavenArtifact(
                                artifact.get("g").getAsString(),
                                artifact.get("a").getAsString(),
                                artifact.get("v").getAsString()
                        ));
                    }
                    return artifacts;
                }
            }

            @Override
            protected void done() {
                try {
                    List<MavenArtifact> artifacts = get();
                    if (artifacts.isEmpty()) {
                        setStatus("No artifacts found for " + className, true);
                    } else {
                        MavenArtifact selected = showArtifactSelectionDialog(artifacts);
                        if (selected != null) {
                            downloadLibrary(selected);
                        }
                    }
                } catch (Exception e) {
                    setStatus("Error searching Maven Central: " + e.getMessage(), true);
                } finally {
                    sourceButton.setEnabled(true);
                    progressBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private static class MavenArtifact {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public MavenArtifact(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    private MavenArtifact showArtifactSelectionDialog(List<MavenArtifact> artifacts) {
        JList<MavenArtifact> list = new JList<>(artifacts.toArray(new MavenArtifact[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        int result = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Select Library to Download",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            return list.getSelectedValue();
        }
        return null;
    }

    private void downloadLibrary(MavenArtifact artifact) {
        setStatus("Downloading " + artifact + "...", false);
        progressBar.setVisible(true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String mavenUrl = String.format(
                        "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                        artifact.groupId.replace('.', '/'),
                        artifact.artifactId,
                        artifact.version,
                        artifact.artifactId,
                        artifact.version
                );

                Path outputFile = libraryFolder.resolve(artifact.artifactId + "-" + artifact.version + ".jar");
                try (InputStream in = new URL(mavenUrl).openStream();
                     OutputStream out = Files.newOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Import the library into the class source
                GhostLibrary library = GhostHelper.readFromLibraryFile(
                        Skidfuscator.LOGGER, 
                        outputFile.toFile()
                );
                classSource.importLibrary(outputFile.toFile());

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    setStatus("Successfully downloaded " + artifact, false);
                    refreshLibraryList();
                    updateMissingClasses();
                } catch (Exception e) {
                    setStatus("Error downloading library: " + e.getMessage(), true);
                } finally {
                    progressBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void updateMissingClasses() {
        Set<String> missingClasses = classSource.getMissingClassNames();
        missingClassesModel.clear();
        for (String className : missingClasses) {
            missingClassesModel.addElement(className);
        }
    }
} 