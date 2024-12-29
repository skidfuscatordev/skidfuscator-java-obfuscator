package dev.skidfuscator.obfuscator.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.SkidfuscatorSession;
import dev.skidfuscator.obfuscator.creator.SkidApplicationClassSource;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Timer;

public class LibrariesPanel extends JPanel implements SkidPanel {
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

        // Create library list panel with buttons
        JPanel libraryListPanel = new JPanel(new BorderLayout());
        libraryListPanel.setBorder(BorderFactory.createTitledBorder("Current Libraries"));
        
        // Create library list
        libraryModel = new DefaultListModel<>();
        libraryList = new JList<>(libraryModel);
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane libraryScrollPane = new JScrollPane(libraryList);

        // Create library control buttons
        JPanel libraryControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add Library");
        JButton removeButton = new JButton("Remove Library");
        
        addButton.addActionListener(e -> addManualLibrary());
        removeButton.addActionListener(e -> removeManualLibrary());
        
        // Enable/disable remove button based on selection
        libraryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeButton.setEnabled(libraryList.getSelectedValue() != null);
            }
        });
        removeButton.setEnabled(false);
        
        libraryControlPanel.add(addButton);
        libraryControlPanel.add(removeButton);
        
        // Add components to library panel
        libraryListPanel.add(libraryScrollPane, BorderLayout.CENTER);
        libraryListPanel.add(libraryControlPanel, BorderLayout.SOUTH);

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

        // Create split pane for lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, libraryListPanel, missingClassesPanel);
        splitPane.setResizeWeight(0.5);

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
        scanButton = new JButton("Rescan");
        scanButton.addActionListener(this::onScanButtonClicked);
        buttonPanel.add(scanButton);

        // Create bottom panel for status and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        // Add components to panel
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Analyze the input jar if specified in config
    }

    public void open() {
        SwingUtilities.invokeLater(this::analyzeConfigJar);
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : Color.WHITE);
        if (isError) {
            Skidfuscator.LOGGER.log(message);
        } else {
            Skidfuscator.LOGGER.log(message);
        }
    }

    private void refreshMissingClassesList() {
        missingClassesModel.clear();
        try {
            classSource.getClassTree().verify();
        } catch (Exception e) {
        }
        classSource.getMissingClassNames()
                .forEach(missingClassesModel::addElement);
    }

    private void refreshLibraryList() {
        libraryModel.clear();
        classSource.getLibraries()
                .stream()
                .map(LibraryClassSource::getParent)
                .map(ApplicationClassSource::getName)
                .filter(e -> !e.endsWith(".jmod")
                        && !e.equalsIgnoreCase("rt.jar"))
                .forEach(libraryModel::addElement);
    }

    private void refreshInput(final File input) {
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
                Skidfuscator skidfuscator = new Skidfuscator(SkidfuscatorSession.builder()
                        .input(input)
                        .libs(libraryFolder.toFile().listFiles((dir, name) -> name.endsWith(".jar")))
                        .build());

                publish("Importing JVM classes...");

                publish("Analyzing JAR file...");
                skidfuscator._importConfig();
                classSource = skidfuscator._importClasspath();
                final Set<LibraryClassSource> sources = skidfuscator._importJvm();
                classSource.addLibraries(sources.toArray(new LibraryClassSource[0]));
                refreshLibraryList();
                refreshMissingClassesList();

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

    private void onScanButtonClicked(ActionEvent e) {
        /*JFileChooser fileChooser = new JFileChooser();
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
            refreshInput(selectedFile);
        }*/
        analyzeConfigJar();
    }

    private void searchMavenCentral(String className, JButton sourceButton) {
        sourceButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        setStatus("Searching Maven Central for " + className + "...", false);

        // Start the fake progress updater
        Timer progressTimer = new Timer(100, null);
        final long startTime = System.currentTimeMillis();
        final Random random = new Random();
        final AtomicInteger currentProgress = new AtomicInteger(0);
        
        progressTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= 15000) {
                progressTimer.stop();
                return;
            }
            
            // Calculate target progress based on elapsed time (0-90%)
            int targetProgress = (int) (elapsed * 90.0 / 15000.0);
            
            // Add some random variation
            int currentValue = currentProgress.get();
            if (currentValue < targetProgress) {
                int increment = random.nextInt(3) + 1; // Random increment between 1-3
                int newProgress = Math.min(currentValue + increment, targetProgress);
                currentProgress.set(newProgress);
                progressBar.setValue(newProgress);
                
                // Update status message occasionally
                if (random.nextInt(10) == 0) {
                    String[] messages = {
                        "Searching Maven repositories...",
                        "Analyzing class dependencies...",
                        "Checking available versions...",
                        "Processing search results...",
                        "Querying Maven Central..."
                    };
                    setStatus(messages[random.nextInt(messages.length)], false);
                }
            }
        });

        SwingWorker<List<MavenArtifact>, String> worker = new SwingWorker<>() {
            @Override
            protected List<MavenArtifact> doInBackground() throws Exception {
                String searchUrl = "https://search.maven.org/solrsearch/select?q=fc:" + URLEncoder.encode(className, StandardCharsets.UTF_8) +
                        "&rows=20&wt=json&core=gav";

                HttpURLConnection connection = (HttpURLConnection) new URL(searchUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(20000); // 20 seconds timeout
                connection.setReadTimeout(20000);    // 20 seconds timeout
                connection.setRequestProperty("User-Agent", "Skidfuscator Library Manager");
                connection.setRequestProperty("Accept", "application/json");

                // Start the progress timer
                SwingUtilities.invokeLater(progressTimer::start);

                // Connect and read response in background
                CompletableFuture<JsonObject> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        connection.connect();
                        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            throw new IOException("Server returned HTTP " + connection.getResponseCode() 
                                    + ": " + connection.getResponseMessage());
                        }
                        
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            return gson.fromJson(reader, JsonObject.class);
                        }
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                });

                // Wait for the response with timeout
                JsonObject response;
                try {
                    response = future.get(15, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    throw new IOException("Connection to Maven Central timed out");
                }

                // Stop the progress timer
                SwingUtilities.invokeLater(progressTimer::stop);

                JsonObject responseObj = response.getAsJsonObject("response");
                JsonArray docs = responseObj.getAsJsonArray("docs");

                progressBar.setValue(95);
                List<MavenArtifact> artifacts = new ArrayList<>();
                int total = docs.size();
                for (int i = 0; i < total; i++) {
                    JsonObject artifact = docs.get(i).getAsJsonObject();
                    artifacts.add(new MavenArtifact(
                            artifact.get("g").getAsString(),
                            artifact.get("a").getAsString(),
                            artifact.get("v").getAsString()
                    ));
                }
                progressBar.setValue(100);
                return artifacts;
            }

            @Override
            protected void done() {
                progressTimer.stop();
                try {
                    List<MavenArtifact> artifacts = get();
                    if (artifacts.isEmpty()) {
                        setStatus("No artifacts found for " + className, true);
                    } else {
                        setStatus("Found " + artifacts.size() + " artifacts", false);
                        MavenArtifact selected = showArtifactSelectionDialog(artifacts);
                        if (selected != null) {
                            downloadLibrary(selected);
                        }
                    }
                } catch (Exception e) {
                    String errorMsg;
                    if (e.getCause() instanceof TimeoutException || e.getCause() instanceof java.net.SocketTimeoutException) {
                        errorMsg = "Connection to Maven Central timed out. Please try again.";
                    } else {
                        errorMsg = "Error searching Maven Central: " + e.getMessage();
                    }
                    setStatus(errorMsg, true);
                    JOptionPane.showMessageDialog(
                        LibrariesPanel.this,
                        errorMsg,
                        "Search Error",
                        JOptionPane.ERROR_MESSAGE
                    );
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
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
        progressBar.setStringPainted(true);
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);

        SwingWorker<File, Integer> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                progressBar.setVisible(true);
                String mavenUrl = String.format(
                        "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                        artifact.groupId.replace('.', '/'),
                        artifact.artifactId,
                        artifact.version,
                        artifact.artifactId,
                        artifact.version
                );

                HttpURLConnection connection = (HttpURLConnection) new URL(mavenUrl).openConnection();
                int fileSize = connection.getContentLength();
                
                File outputFile = libraryFolder.resolve(artifact.artifactId + "-" + artifact.version + ".jar").toFile();
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (fileSize > 0) {
                            publish((int) ((totalBytesRead * 100) / fileSize));
                        }
                    }
                }
                return outputFile;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    Skidfuscator.LOGGER.log("Download progress: " + chunks.get(chunks.size() - 1) + "%");
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    File downloadedFile = get();
                    setStatus("Importing library " + artifact + "...", false);
                    
                    try {
                        classSource.importLibrary(downloadedFile);
                        setStatus("Successfully imported " + artifact, false);
                        refreshLibraryList();
                        refreshMissingClassesList();
                    } catch (IOException e) {
                        String errorMsg = "Failed to import library: " + e.getMessage();
                        setStatus(errorMsg, true);
                        JOptionPane.showMessageDialog(
                            LibrariesPanel.this,
                            errorMsg + "\nError details: " + e.toString(),
                            "Import Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                        // Clean up the downloaded file if import fails
                        if (!downloadedFile.delete()) {
                            downloadedFile.deleteOnExit();
                        }
                    }
                } catch (Exception e) {
                    String errorMsg = "Error downloading library: " + e.getMessage();
                    setStatus(errorMsg, true);
                    JOptionPane.showMessageDialog(
                        LibrariesPanel.this,
                        errorMsg + "\nError details: " + e.toString(),
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    progressBar.setVisible(false);
                    progressBar.setStringPainted(false);
                }
            }
        };
        worker.execute();
    }

    private void addManualLibrary() {
        FileDialog fileChooser = new FileDialog((Frame) null);
        fileChooser.setVisible(true);
        fileChooser.setMode(FileDialog.LOAD);
        fileChooser.setFilenameFilter((f, name) -> f.isDirectory() || name.toLowerCase().endsWith(".jar"));
        String selectedFileStr = fileChooser.getFile();
        if (selectedFileStr != null) {
            File selectedFile = new File(fileChooser.getDirectory(), selectedFileStr);
            
            // Check if the file is outside the library folder
            if (!selectedFile.getParentFile().equals(libraryFolder.toFile())) {
                int result = JOptionPane.showConfirmDialog(
                    this,
                    "The selected library is outside the library folder.\n" +
                    "Would you like to copy it to the library folder?",
                    "Copy Library",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        File destFile = libraryFolder.resolve(selectedFile.getName()).toFile();
                        Files.copy(selectedFile.toPath(), destFile.toPath());
                        selectedFile = destFile;
                        setStatus("Library copied to library folder", false);
                    } catch (IOException e) {
                        setStatus("Failed to copy library: " + e.getMessage(), true);
                        return;
                    }
                }
            }

            try {
                classSource.importLibrary(selectedFile);
                refreshLibraryList();
                refreshMissingClassesList();
                setStatus("Successfully imported " + selectedFile.getName(), false);
            } catch (IOException e) {
                setStatus("Failed to import library: " + e.getMessage(), true);
            }
        }
    }

    private void removeManualLibrary() {
        String selectedLibrary = libraryList.getSelectedValue();
        if (selectedLibrary != null) {
            File libraryFile = libraryFolder.resolve(selectedLibrary).toFile();
            if (libraryFile.exists()) {
                int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to remove this library?\n" +
                    "This will also delete the file from the library folder.",
                    "Remove Library",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    if (libraryFile.delete()) {
                        setStatus("Successfully removed " + selectedLibrary, false);
                        classSource.getLibraries()
                                .removeIf(lib -> lib
                                        .getParent()
                                        .getName()
                                        .equals(selectedLibrary)
                                );
                        refreshMissingClassesList();
                        refreshLibraryList();
                    } else {
                        setStatus("Failed to remove library file", true);
                    }
                }
            }
        }
    }

    private void analyzeConfigJar() {
        String inputPath = configPanel.getInputPath();
        if (inputPath != null && !inputPath.isEmpty()) {
            File inputFile = new File(inputPath);
            if (inputFile.exists()) {
                refreshInput(inputFile);
            }
        }
    }
} 