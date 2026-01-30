package xyz.jphil.ai.proj_build_dev_misc_tools.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import xyz.jphil.ai.proj_build_dev_misc_tools.Settings;
import xyz.jphil.ai.proj_build_dev_misc_tools.TerminalHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectPickerDialog {

    public enum Action {
        NONE,           // User pressed Enter - show TUI menu
        CHANGE_DIR,     // User pressed C - change directory
        OPEN_FOLDER,    // User pressed F - open in file explorer
        OPEN_EDITOR     // User pressed E - open in editor
    }

    public static class Result {
        public final String projectPath;
        public final Action action;

        public Result(String projectPath, Action action) {
            this.projectPath = projectPath;
            this.action = action;
        }
    }

    private Stage stage;
    private TextField searchField;
    private ListView<String> projectListView;
    private ObservableList<String> allProjects;
    private ObservableList<String> filteredProjects;
    private Result result = null;
    private Settings settings;
    private Path codeReposPath;
    private Label instructions;
    private int maxFilteredCount;
    private Button refreshButton;
    private Label refreshStatusLabel;
    private ProgressBar progressBar;
    private Label progressLabel;
    private CheckBox quickSelectCheckBox;
    private boolean quickSelectEnabled = true;

    public ProjectPickerDialog(List<String> projects, Settings settings) {
        this.allProjects = FXCollections.observableArrayList(projects);
        this.filteredProjects = FXCollections.observableArrayList(projects);
        this.settings = settings;
        this.codeReposPath = Paths.get(settings.getCodeReposPath());
        this.maxFilteredCount = projects.size(); // Initialize with total count
        initUI();
    }

    private void initUI() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Select Project");
        stage.setWidth(900);
        stage.setHeight(600);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Top toolbar with search field and refresh button
        HBox searchToolbar = new HBox(8);
        searchToolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Type to filter projects (regex supported)...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        refreshButton = new Button("↻ Refresh");
        refreshButton.setStyle("-fx-font-size: 11px;");
        refreshButton.setTooltip(new Tooltip("Refresh project list from filesystem"));
        refreshButton.setMinWidth(80);

        refreshStatusLabel = new Label();
        refreshStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        quickSelectCheckBox = new CheckBox("Quick Select");
        quickSelectCheckBox.setSelected(true);
        quickSelectCheckBox.setStyle("-fx-font-size: 11px;");
        quickSelectCheckBox.setTooltip(new Tooltip("Press 1-9 to select items by number (requires unchecking to search for numbers)"));
        quickSelectCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            quickSelectEnabled = newVal;
            projectListView.refresh(); // Refresh cells to show/hide numbering
            updateInstructionsLabel(); // Update instructions to show/hide 1-9 hint
        });

        searchToolbar.getChildren().addAll(searchField, refreshButton, refreshStatusLabel, quickSelectCheckBox);

        // Progress bar (compact)
        progressBar = new ProgressBar(0);
        progressBar.setStyle("-fx-padding: 0;");
        progressBar.setPrefHeight(6);
        progressBar.setVisible(false);

        progressLabel = new Label();
        progressLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        progressLabel.setVisible(false);

        // ListView with custom cell renderer (project name + inline buttons)
        projectListView = new ListView<>();
        projectListView.setItems(filteredProjects);
        projectListView.setPrefHeight(500);

        // Custom cell factory with project name and inline buttons
        projectListView.setCellFactory(lv -> new ListCell<String>() {
            private final HBox cellBox = new HBox(10);
            private final Label numberLabel = new Label();
            private final Label projectLabel = new Label();
            private final Button btnC = new Button("C");
            private final Button btnF = new Button("F");
            private final Button btnE = new Button("E");

            {
                numberLabel.setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold; -fx-min-width: 20;");
                numberLabel.setAlignment(Pos.CENTER_RIGHT);

                projectLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(projectLabel, Priority.ALWAYS);

                btnC.setTooltip(new Tooltip("Change to directory in terminal"));
                btnF.setTooltip(new Tooltip("Open in File Explorer"));
                btnE.setTooltip(new Tooltip("Open in Editor"));

                btnC.setMinWidth(40);
                btnF.setMinWidth(40);
                btnE.setMinWidth(40);

                // Apply gentle colors to buttons
                btnC.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2;");
                btnF.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00;");
                btnE.setStyle("-fx-background-color: #F3E5F5; -fx-text-fill: #7B1FA2;");

                // Button click handlers
                btnC.setOnAction(e -> {
                    String project = getItem();
                    if (project != null) {
                        executeAction(project, Action.CHANGE_DIR);
                    }
                });

                btnF.setOnAction(e -> {
                    String project = getItem();
                    if (project != null) {
                        executeAction(project, Action.OPEN_FOLDER);
                    }
                });

                btnE.setOnAction(e -> {
                    String project = getItem();
                    if (project != null) {
                        executeAction(project, Action.OPEN_EDITOR);
                    }
                });

                cellBox.getChildren().addAll(numberLabel, projectLabel, btnC, btnF, btnE);
                cellBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    projectLabel.setText(item);

                    // Show number if quick select enabled and index < 9
                    if (quickSelectEnabled && getIndex() >= 0 && getIndex() < 9) {
                        numberLabel.setText(String.valueOf(getIndex() + 1));
                        numberLabel.setVisible(true);
                    } else {
                        numberLabel.setVisible(false);
                    }

                    setGraphic(cellBox);
                }
            }
        });

        // Handle search field text changes
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterResults(newVal);
        });

        // Handle refresh button click
        refreshButton.setOnAction(e -> {
            refreshProjectList();
        });

        // Handle keyboard events in search field
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                selectWithoutAction();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
                // Transfer focus to list when arrow keys pressed
                projectListView.requestFocus();
                if (projectListView.getSelectionModel().getSelectedIndex() < 0) {
                    projectListView.getSelectionModel().selectFirst();
                }
                event.consume();
            } else if (quickSelectEnabled && event.getCode().isDigitKey()) {
                // When quick select is enabled, number keys select items, not typed in search
                // Transfer focus to list to handle number key
                projectListView.requestFocus();
                // Manually trigger the number selection
                int digit = Integer.parseInt(event.getText());
                if (digit >= 1 && digit <= 9) {
                    int index = digit - 1;
                    if (index < filteredProjects.size()) {
                        projectListView.getSelectionModel().select(index);
                        projectListView.scrollTo(index);
                    }
                }
                event.consume();
            }
            // Note: C, F, E keys are NOT handled here - only in ListView when it has focus
        });

        // Handle circular navigation with event filter (runs BEFORE default ListView behavior)
        projectListView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (filteredProjects.isEmpty()) return;

            int currentIndex = projectListView.getSelectionModel().getSelectedIndex();

            if (event.getCode() == KeyCode.UP && currentIndex == 0) {
                // At top, wrap to bottom
                projectListView.getSelectionModel().selectLast();
                projectListView.scrollTo(filteredProjects.size() - 1);
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN && currentIndex == filteredProjects.size() - 1) {
                // At bottom, wrap to top
                projectListView.getSelectionModel().selectFirst();
                projectListView.scrollTo(0);
                event.consume();
            }
            // If not at boundaries, don't consume - let ListView handle normal navigation
        });

        // Handle keyboard events in ListView - ONLY when ListView has focus
        projectListView.setOnKeyPressed(event -> {
            String selected = projectListView.getSelectionModel().getSelectedItem();

            // Handle number keys for quick select (1-9)
            if (quickSelectEnabled && event.getCode().isDigitKey()) {
                int digit = Integer.parseInt(event.getText());
                if (digit >= 1 && digit <= 9) {
                    int index = digit - 1;
                    if (index < filteredProjects.size()) {
                        projectListView.getSelectionModel().select(index);
                        projectListView.scrollTo(index);
                        event.consume();
                        return;
                    }
                }
            }

            if (selected == null) return;

            if (event.getCode() == KeyCode.C) {
                executeAction(selected, Action.CHANGE_DIR);
                event.consume();
            } else if (event.getCode() == KeyCode.F) {
                executeAction(selected, Action.OPEN_FOLDER);
                event.consume();
            } else if (event.getCode() == KeyCode.E) {
                executeAction(selected, Action.OPEN_EDITOR);
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                selectWithoutAction();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                closeDialog();
                event.consume();
            }
        });

        // Double-click to select (NONE action - show TUI menu)
        projectListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                selectWithoutAction();
            }
        });

        // Instructions label with count
        instructions = new Label();
        instructions.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");
        updateInstructionsLabel();

        root.getChildren().addAll(searchToolbar, progressBar, progressLabel, projectListView, instructions);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Focus search field on show and auto-trigger initial refresh
        stage.setOnShown(e -> {
            // Disable refresh button initially (scan in progress)
            refreshButton.setDisable(true);
            // Auto-trigger refresh on initial load
            refreshProjectList();
        });

        // Close handler is set up in showOnFxThread() now
    }

    private void executeAction(String project, Action action) {
        result = new Result(project, action);

        // Execute the action
        Path projectPath = codeReposPath.resolve(project);

        try {
            switch (action) {
                case CHANGE_DIR:
                    changeToDirectory(projectPath);
                    break;
                case OPEN_FOLDER:
                    openInExplorer(projectPath);
                    break;
                case OPEN_EDITOR:
                    openInEditor(projectPath);
                    break;
            }
        } catch (IOException e) {
            System.err.println("Error executing action: " + e.getMessage());
            e.printStackTrace();
            // Don't show GUI error dialog, just print to console and exit
            stage.close();
            System.exit(1);
            return;
        }

        stage.close();

        // Properly shut down JavaFX and database before exiting
        Platform.runLater(() -> {
            try {
                Platform.exit();

                // Shutdown database cleanly - this is a blocking call
                try {
                    xyz.jphil.ai.proj_build_dev_misc_tools.db.DB.get().shutdown();
                } catch (Exception e) {
                    // Ignore shutdown errors during exit
                }
            } finally {
                System.exit(0);
            }
        });
    }

    private void changeToDirectory(Path projectPath) throws IOException {
        // Use TerminalHelper to write post-run batch script
        if (!TerminalHelper.isPostrunBatchAvailable()) {
            System.err.println("Directory change feature requires running via proj.bat wrapper script.");
            throw new IOException("Directory change feature requires running via proj.bat wrapper script.");
        }

        boolean success = TerminalHelper.writePostrunBatch(projectPath);
        if (!success) {
            System.err.println("Failed to schedule directory change.");
            throw new IOException("Failed to schedule directory change.");
        }

        System.out.println("\n✓ Directory change scheduled!");
        System.out.println("Exiting program. Your terminal will change to:");
        System.out.println("  " + projectPath.toAbsolutePath());
    }

    private void openInExplorer(Path projectPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Runtime.getRuntime().exec("explorer.exe \"" + projectPath.toString() + "\"");
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec("open \"" + projectPath.toString() + "\"");
        } else {
            Runtime.getRuntime().exec("xdg-open \"" + projectPath.toString() + "\"");
        }
        System.out.println("Opened in file explorer: " + projectPath);
    }

    private void openInEditor(Path projectPath) throws IOException {
        String ideLauncher = settings.getIdeLauncher();
        if (ideLauncher == null || ideLauncher.isEmpty()) {
            throw new IOException("IDE Launcher not configured in settings.");
        }

        // Replace %PATH% placeholder with actual project path
        String command = ideLauncher.replace("%PATH%", projectPath.toString());

        // Parse command into executable and arguments
        // Handle both quoted and unquoted paths
        String[] parts = parseCommand(command);
        if (parts.length == 0) {
            throw new IOException("Invalid IDE launcher command: " + command);
        }

        ProcessBuilder pb = new ProcessBuilder(parts);
        pb.start();
        System.out.println("Opened in IDE: " + projectPath);
    }

    /**
     * Parses a command string into executable and arguments.
     * Handles quoted strings properly.
     */
    private String[] parseCommand(String command) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }

    private void selectWithoutAction() {
        String selected = projectListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            result = new Result(selected, Action.NONE);
            stage.close();
        }
    }

    private void closeDialog() {
        result = null;
        stage.close();
    }

    private void filterResults(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredProjects.setAll(allProjects);
            // Update max count when showing all items
            maxFilteredCount = allProjects.size();
            updateInstructionsLabel();
            return;
        }

        List<String> filtered;
        try {
            // Convert space-separated terms to fuzzy regex
            // e.g., "xyz ai" becomes "xyz.*ai" (matches xyz-ai, xyz_ai, xyz anything ai, etc.)
            String[] terms = searchText.trim().split("\\s+");
            String fuzzyPattern = String.join(".*", terms);

            Pattern pattern = Pattern.compile(fuzzyPattern, Pattern.CASE_INSENSITIVE);
            filtered = allProjects.stream()
                .filter(p -> pattern.matcher(p).find())
                .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback to simple contains search
            String lower = searchText.toLowerCase();
            filtered = allProjects.stream()
                .filter(p -> p.toLowerCase().contains(lower))
                .collect(Collectors.toList());
        }

        filteredProjects.setAll(filtered);

        // Update max count cache
        if (filtered.size() > maxFilteredCount) {
            maxFilteredCount = filtered.size();
        }

        // Update instructions label with new count
        updateInstructionsLabel();

        // Auto-select first item if available
        if (!filtered.isEmpty()) {
            projectListView.getSelectionModel().selectFirst();
        }
    }

    private void updateInstructionsLabel() {
        int currentCount = filteredProjects.size();
        String countText;

        if (currentCount == maxFilteredCount) {
            // No filtering applied or showing max count
            countText = currentCount + " items";
        } else {
            // Filtering applied
            countText = currentCount + "/" + maxFilteredCount + " items";
        }

        String instructionText = countText + " | Type to filter | ↑↓: Navigate | Enter: Select | C: Terminal | F: Folder | E: Editor | Esc: Cancel";
        if (quickSelectEnabled) {
            instructionText += " | 1-9: Quick Select";
        }
        instructions.setText(instructionText);
    }

    private void refreshProjectList() {
        // Disable button and show progress
        refreshButton.setDisable(true);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressBar.setProgress(0);
        progressLabel.setText("Scanning...");

        // Run refresh on background thread to avoid blocking UI
        Thread refreshThread = new Thread(() -> {
            try {
                // Create progress listener
                xyz.jphil.ai.proj_build_dev_misc_tools.scanner.ProjectScanner.ProgressListener progressListener =
                    (foldersScanned, projectsFound) -> {
                        Platform.runLater(() -> {
                            // Estimate progress (folders scanned / estimated total ~100)
                            double progress = Math.min((double) foldersScanned / 100.0, 0.95);
                            progressBar.setProgress(progress);
                            progressLabel.setText(String.format("Scanning... (%d folders, %d projects)", foldersScanned, projectsFound));
                        });
                    };

                // Call ProjectManager.refreshProjects with progress listener
                List<String> newProjects = xyz.jphil.ai.proj_build_dev_misc_tools.ProjectManager.refreshProjects(null, settings, progressListener);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    // Preserve user's search text while updating projects
                    String currentSearchText = searchField.getText();

                    // Update all projects list
                    allProjects.setAll(newProjects);

                    // Re-apply search filter with current text and new projects
                    if (currentSearchText.isEmpty()) {
                        // No search - show all projects
                        filteredProjects.setAll(newProjects);
                        maxFilteredCount = newProjects.size();
                    } else {
                        // Re-apply search filter with user's existing text
                        filterResults(currentSearchText);
                    }

                    // Update instructions with new count
                    updateInstructionsLabel();

                    // Select first item if available
                    if (!filteredProjects.isEmpty()) {
                        projectListView.getSelectionModel().selectFirst();
                    }

                    // Hide progress bar and show results
                    progressBar.setVisible(false);
                    progressLabel.setVisible(false);

                    // Focus search field (so user can continue typing)
                    searchField.requestFocus();

                    // Re-enable button
                    refreshButton.setDisable(false);
                });

            } catch (Exception e) {
                System.err.println("Error during refresh: " + e.getMessage());
                e.printStackTrace();

                // Update UI to show error
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressLabel.setText("✗ Error during scan");
                    progressLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: red;");
                    refreshButton.setDisable(false);

                    // Reset label style after 3 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        Platform.runLater(() -> {
                            progressLabel.setVisible(false);
                            progressLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                        });
                    }, "RefreshStatus-Clearer").start();
                });
            }
        }, "ProjectList-Refresh");

        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    /**
     * Shows the dialog and waits for user selection.
     * @return Result containing project path and action
     */
    public Result showAndWait() {
        stage.showAndWait();
        return result;
    }

    /**
     * Static helper to show picker dialog from any thread.
     */
    public static Result show(List<String> projects, Settings settings) {
        System.out.println("[ProjectPickerDialog] Showing project picker dialog with " + projects.size() + " projects");
        return JavaFXManager.getInstance().showDialog(
            (lock, resultHolder) -> showOnFxThread(projects, settings, lock, resultHolder),
            600000  // 10 minute timeout - user may browse projects for a while
        );
    }

    /**
     * Called on JavaFX thread to create and show the dialog.
     * MUST NOT BLOCK - returns immediately after showing dialog.
     */
    private static void showOnFxThread(List<String> projects, Settings settings, Object lock, Object[] resultHolder) {
        System.out.println("[ProjectPickerDialog] Creating dialog on JavaFX thread");
        ProjectPickerDialog dialog = new ProjectPickerDialog(projects, settings);

        // Set up close handler to notify when dialog closes
        dialog.stage.setOnHidden(event -> {
            System.out.println("[ProjectPickerDialog] Dialog closed");
            synchronized (lock) {
                // If result is null (user cancelled with Esc), return null to signal cancellation
                resultHolder[0] = dialog.result;
                lock.notifyAll();
            }
        });

        dialog.stage.show();
        System.out.println("[ProjectPickerDialog] Stage shown successfully (not blocking FX thread)");
    }
}
