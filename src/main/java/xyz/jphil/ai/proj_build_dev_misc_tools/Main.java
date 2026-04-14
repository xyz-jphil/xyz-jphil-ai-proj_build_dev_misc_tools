package xyz.jphil.ai.proj_build_dev_misc_tools;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import xyz.jphil.ai.proj_build_dev_misc_tools.ui.MainMenuDialog;

import java.io.IOException;

@Command(name = "prj",
         description = "Project Build & Dev Tools - Command-line tool for managing development projects",
         version = "1.0",
         mixinStandardHelpOptions = false,
         subcommands = {
             Main.OpenCommand.class,
             Main.NewCommand.class,
             Main.CloneCommand.class,
             Main.PrpCommand.class,
             Main.GlobalPrpCommand.class,
             Main.SettingsCommand.class,
             Main.IdeCommand.class,
             Main.HelpCommand.class
         },
         footer = "%nRun 'prj' without arguments to use interactive menu mode.%n")
public class Main implements Runnable {
    @picocli.CommandLine.Option(names = {"--verbose", "-v", "--debug"}, description = "Enable verbose/debug output")
    private boolean verboseOpt = false;

    private static Settings settings;
    private static SettingsManager settingsManager;
    private static Terminal terminal;
    private static LineReader lineReader;

    // Cache scanned projects at application level (scan once per run)
    private static java.util.List<String> cachedProjects = null;
    private static boolean scanInProgress = false;

    // Verbose mode flag
    private static boolean verboseMode = false;

    private static boolean verboseInstanceOpt = false;

    public static boolean isVerbose() {
        return verboseMode || verboseInstanceOpt;
    }

    public static void setVerbose(boolean verbose) {
        verboseMode = verbose;
        verboseInstanceOpt = verbose;
    }

    public static void main(String[] args) {
        // Initialize logging configuration FIRST (before any library loads)
        // Default to false, will be updated if Picocli sets it
        LogConfig.initialize(false);

        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            settingsManager = new SettingsManager();
            settings = settingsManager.loadSettings();

            // Handle first run setup
            if (settings == null) {
                handleFirstRun();
                System.exit(0);
            }

            // Check if CLI arguments provided
            if (args.length == 0) {
                // No arguments - show JavaFX interactive menu
                mainMenu();
            } else {
                // CLI mode - use Picocli to parse and execute command
                Main mainCmd = new Main();
                CommandLine cmd = new CommandLine(mainCmd);

                // Execute command (this will parse and set verboseOpt)
                int exitCode = cmd.execute(args);

                // After execution, update verbose flag if it was set
                if (mainCmd.verboseOpt) {
                    setVerbose(true);
                }

                System.exit(exitCode);
            }

        } catch (IOException e) {
            System.err.println("Error initializing terminal: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }

    /**
     * Implements Runnable - called when main command is invoked without subcommands
     */
    @Override
    public void run() {
        // Update verbose flag from command option
        if (verboseOpt) {
            setVerbose(true);
            System.out.println("[DEBUG] Verbose mode enabled");
        }

        // When 'prj' is called without subcommands in CLI mode, show JavaFX interactive menu
        try {
            mainMenu();
        } catch (Exception e) {
            System.err.println("Error running interactive menu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleFirstRun() {
        printBanner();
        terminal.writer().println("FIRST RUN DETECTED");
        terminal.writer().println();
        terminal.writer().println("A template Settings.xml file has been created at:");
        terminal.writer().println("  " + settingsManager.getSettingsFilePath());
        terminal.writer().println();
        terminal.writer().println("Please edit this file to configure:");
        terminal.writer().println("  - Your code repositories path");
        terminal.writer().println("  - Default editor");
        terminal.writer().println("  - Organizations with their groupIds");
        terminal.writer().println("  - Other preferences");
        terminal.writer().println();
        terminal.writer().println("After editing the file, run this program again.");
        terminal.writer().flush();

        try {
            String openNow = lineReader.readLine("Open settings file now? (y/n): ").trim();
            if (openNow.equalsIgnoreCase("y")) {
                openFileWithFallback(settingsManager.getSettingsFilePath().toFile());
            }
        } catch (Exception e) {
            // Ignore - just exit
        }
    }

    private static void printBanner() {
        terminal.writer().println("========================================");
        terminal.writer().println("  Project Build & Dev Tools");
        terminal.writer().println("  Version 1.0");
        terminal.writer().println("========================================");
        terminal.writer().println();
        terminal.writer().flush();
    }

    private static void mainMenu() {
        // Show main menu - show once, sub-actions don't loop back
        MainMenuDialog.Action action = MainMenuDialog.show(settings);

        switch (action) {
            case OPEN_PROJECT:
                ProjectManager.openProject(terminal, lineReader, settings, getCachedProjects());
                break;
            case GIT_CLONE:
                GitManager.cloneRepository(terminal, lineReader, settings);
                break;
            case NEW_PROJECT:
                ProjectManager.createNewProject(terminal, lineReader, settings);
                break;
            case PRPS_MANAGER:
                AITemplateManager.manageTemplatesUI(settings);
                break;
            case GLOBAL_PRPS_MANAGER:
                AITemplateManager.manageGlobalTemplatesUI(settings);
                break;
            case SETTINGS:
                settingsMenu();
                break;
            case IDE_EDITOR:
                openCurrentDirInIDE();
                break;
            case BACK:
            case EXIT:
            default:
                // Any exit action (BACK from menu, EXIT, or null/default)
                terminal.writer().println("\nGoodbye!");
                terminal.writer().flush();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
        }
    }

    private static String readSingleKey() {
        Attributes originalAttributes = null;
        try {
            // Save original attributes before entering raw mode
            originalAttributes = terminal.getAttributes();

            // Enable raw mode to read single characters
            terminal.enterRawMode();
            int c = terminal.reader().read();

            // Handle ESC key
            if (c == 27) {
                return "\u001B";
            }

            return String.valueOf((char) c);
        } catch (IOException e) {
            terminal.writer().println("Error reading input: " + e.getMessage());
            terminal.writer().flush();
            return "";
        } finally {
            // Restore original attributes
            if (originalAttributes != null) {
                try {
                    terminal.setAttributes(originalAttributes);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    private static void settingsMenu() {
        while (true) {
            terminal.writer().println("\n--- Settings ---");
            terminal.writer().println("Current Settings:");
            terminal.writer().println("  Code Repos Path: " + settings.getCodeReposPath());
            terminal.writer().println("  Single File Editor: " + settings.getSingleFileEditor());
            terminal.writer().println("  IDE Launcher: " + settings.getIdeLauncher());
            terminal.writer().print("  Organizations: ");
            if (settings.getOrganizations().isEmpty()) {
                terminal.writer().println("(none)");
            } else {
                for (Organization org : settings.getOrganizations()) {
                    terminal.writer().print(org.getName() + " ");
                }
                terminal.writer().println();
            }
            terminal.writer().println();
            terminal.writer().println("1. Configure (C)ode Repository Path");
            terminal.writer().println("2. Configure Project Scanning (D)epth");
            terminal.writer().println("3. Manage (O)rganizations");
            terminal.writer().println("4. Configure Single File (E)ditor");
            terminal.writer().println("5. Configure Project IDE (L)auncher");
            terminal.writer().println("6. Edit Settings File (R)aw");
            terminal.writer().println("7. Show Settings (F)ile Location");
            terminal.writer().println("8. Re(l)oad Settings from File");
            terminal.writer().println("0. E(x)it to Main Menu (Esc)");
            terminal.writer().println();
            terminal.writer().flush();

            String choice = readSingleKey();

            switch (choice.toLowerCase()) {
                case "1":
                case "c":
                    String newPath = lineReader.readLine("Enter new Code Repos Path: ");
                    settings.setCodeReposPath(newPath);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "2":
                case "d":
                    String depthStr = lineReader.readLine("Enter project scanning depth (current: " + settings.getProjectScanningDepth() + "): ");
                    try {
                        int depth = Integer.parseInt(depthStr.trim());
                        settings.setProjectScanningDepth(depth);
                        settingsManager.saveSettings(settings);
                        terminal.writer().println("Settings saved.");
                    } catch (NumberFormatException e) {
                        terminal.writer().println("Invalid number. Settings not changed.");
                    }
                    terminal.writer().flush();
                    break;
                case "3":
                case "o":
                    manageOrganizations();
                    break;
                case "4":
                case "e":
                    String newEditor = lineReader.readLine("Enter Single File Editor (full path): ");
                    settings.setSingleFileEditor(newEditor);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "5":
                case "l":
                    terminal.writer().println("\nEnter IDE Launcher command template.");
                    terminal.writer().println("Use %PATH% as placeholder for project path.");
                    terminal.writer().println("Example: C:\\Program Files\\NetBeans\\bin\\netbeans.exe --open \"%PATH%\"");
                    terminal.writer().flush();
                    String newLauncher = lineReader.readLine("IDE Launcher: ");
                    settings.setIdeLauncher(newLauncher);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "6":
                case "r":
                    openSettingsFileInEditor();
                    break;
                case "7":
                case "f":
                    terminal.writer().println("Settings file: " + settingsManager.getSettingsFilePath());
                    terminal.writer().flush();
                    break;
                case "8":
                    settings = settingsManager.loadSettings();
                    if (settings == null) {
                        terminal.writer().println("Error reloading settings. Using previous settings.");
                        settings = settingsManager.loadSettings();
                    } else {
                        terminal.writer().println("Settings reloaded successfully.");
                    }
                    terminal.writer().flush();
                    break;
                case "0":
                case "x":
                case "\u001B": // ESC key
                    return;
                default:
                    terminal.writer().println("Invalid option.");
                    terminal.writer().flush();
            }
        }
    }

    private static void openSettingsFileInEditor() {
        openFileWithFallback(settingsManager.getSettingsFilePath().toFile());
    }

    /**
     * Opens a file using configured editor or falls back to Desktop API.
     */
    private static void openFileWithFallback(java.io.File file) {
        String editor = settings != null ? settings.getSingleFileEditor() : null;

        // Try configured editor first
        if (editor != null && !editor.isEmpty()) {
            try {
                ProcessBuilder pb = new ProcessBuilder(editor, file.getAbsolutePath());
                pb.start();
                terminal.writer().println("Opening file in configured editor...");
                terminal.writer().flush();
                return;
            } catch (Exception e) {
                terminal.writer().println("WARNING: Failed to open with configured editor: " + e.getMessage());
                terminal.writer().println("Falling back to system default editor...");
                terminal.writer().flush();
            }
        }

        // Fallback to java.awt.Desktop
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.EDIT)) {
                    desktop.edit(file);
                    terminal.writer().println("Opening file with system default editor...");
                    terminal.writer().flush();
                } else if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(file);
                    terminal.writer().println("Opening file with system default application...");
                    terminal.writer().flush();
                } else {
                    terminal.writer().println("ERROR: No way to open file - Desktop API not supported.");
                    terminal.writer().println("File location: " + file.getAbsolutePath());
                    terminal.writer().flush();
                }
            } else {
                terminal.writer().println("ERROR: Desktop API not supported on this system.");
                terminal.writer().println("File location: " + file.getAbsolutePath());
                terminal.writer().flush();
            }
        } catch (Exception e) {
            terminal.writer().println("ERROR: Failed to open file: " + e.getMessage());
            terminal.writer().println("File location: " + file.getAbsolutePath());
            terminal.writer().flush();
        }
    }

    private static void manageOrganizations() {
        while (true) {
            terminal.writer().println("\n--- Manage Organizations ---");
            terminal.writer().println("Current Organizations:");
            for (int i = 0; i < settings.getOrganizations().size(); i++) {
                Organization org = settings.getOrganizations().get(i);
                terminal.writer().println((i + 1) + ". " + org.getName() + " (groupId: " + org.getGroupId() + ")");
            }
            terminal.writer().println();
            terminal.writer().println("1. (A)dd Organization");
            terminal.writer().println("2. (E)dit Organization");
            terminal.writer().println("3. (R)emove Organization");
            terminal.writer().println("0. E(x)it (Esc)");
            terminal.writer().println();
            terminal.writer().flush();

            String choice = readSingleKey();

            switch (choice.toLowerCase()) {
                case "1":
                case "a":
                    String newOrgName = lineReader.readLine("Enter organization name: ").trim();
                    if (newOrgName.isEmpty()) {
                        terminal.writer().println("Organization name cannot be empty.");
                        terminal.writer().flush();
                        break;
                    }

                    boolean exists = false;
                    for (Organization org : settings.getOrganizations()) {
                        if (org.getName().equals(newOrgName)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        terminal.writer().println("Organization already exists.");
                        terminal.writer().flush();
                        break;
                    }

                    String newGroupId = lineReader.readLine("Enter Maven groupId (e.g., io.github." + newOrgName + "): ").trim();
                    if (newGroupId.isEmpty()) {
                        newGroupId = "io.github." + newOrgName;
                    }

                    settings.getOrganizations().add(new Organization(newOrgName, newGroupId));
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Organization added.");
                    terminal.writer().flush();
                    break;

                case "2":
                case "e":
                    if (settings.getOrganizations().isEmpty()) {
                        terminal.writer().println("No organizations to edit.");
                        terminal.writer().flush();
                        break;
                    }

                    String editIndexStr = lineReader.readLine("Enter organization number to edit: ");
                    try {
                        int editIndex = Integer.parseInt(editIndexStr) - 1;
                        if (editIndex >= 0 && editIndex < settings.getOrganizations().size()) {
                            Organization orgToEdit = settings.getOrganizations().get(editIndex);

                            terminal.writer().println("Current name: " + orgToEdit.getName());
                            String newName = lineReader.readLine("Enter new name (or press Enter to keep): ").trim();
                            if (!newName.isEmpty()) {
                                orgToEdit.setName(newName);
                            }

                            terminal.writer().println("Current groupId: " + orgToEdit.getGroupId());
                            String newGid = lineReader.readLine("Enter new groupId (or press Enter to keep): ").trim();
                            if (!newGid.isEmpty()) {
                                orgToEdit.setGroupId(newGid);
                            }

                            settingsManager.saveSettings(settings);
                            terminal.writer().println("Organization updated.");
                        } else {
                            terminal.writer().println("Invalid number.");
                        }
                    } catch (NumberFormatException e) {
                        terminal.writer().println("Invalid input.");
                    }
                    terminal.writer().flush();
                    break;

                case "3":
                case "r":
                    if (settings.getOrganizations().isEmpty()) {
                        terminal.writer().println("No organizations to remove.");
                        terminal.writer().flush();
                        break;
                    }

                    String removeIndexStr = lineReader.readLine("Enter organization number to remove: ");
                    try {
                        int removeIndex = Integer.parseInt(removeIndexStr) - 1;
                        if (removeIndex >= 0 && removeIndex < settings.getOrganizations().size()) {
                            settings.getOrganizations().remove(removeIndex);
                            settingsManager.saveSettings(settings);
                            terminal.writer().println("Organization removed.");
                        } else {
                            terminal.writer().println("Invalid number.");
                        }
                    } catch (NumberFormatException e) {
                        terminal.writer().println("Invalid input.");
                    }
                    terminal.writer().flush();
                    break;

                case "0":
                case "x":
                case "\u001B": // ESC key
                    break; // Return to main menu

                default:
                    terminal.writer().println("Invalid option.");
                    terminal.writer().flush();
            }
        }
    }

    /**
     * Opens the current directory in the configured IDE.
     */
    private static void openCurrentDirInIDE() {
        String ideLauncher = settings.getIdeLauncher();

        if (ideLauncher == null || ideLauncher.isEmpty()) {
            terminal.writer().println("ERROR: IDE Launcher not configured in settings.");
            terminal.writer().println("Please configure it in Settings menu (option 5).");
            terminal.writer().flush();
            return;
        }

        try {
            String currentDir = System.getProperty("user.dir");
            terminal.writer().println("\n--- Open in IDE ---");
            terminal.writer().println("Current directory: " + currentDir);
            terminal.writer().println("Opening in IDE...");
            terminal.writer().flush();

            // Replace %PATH% placeholder with current directory
            String command = ideLauncher.replace("%PATH%", currentDir);

            // Parse and execute command
            String[] parts = parseCommand(command);
            if (parts.length == 0) {
                terminal.writer().println("ERROR: Invalid IDE launcher command.");
                terminal.writer().flush();
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.start();

            terminal.writer().println("✓ Opened current directory in IDE.");
            terminal.writer().flush();

        } catch (Exception e) {
            terminal.writer().println("ERROR: Failed to open IDE: " + e.getMessage());
            terminal.writer().flush();
        }
    }

    /**
     * Parses a command string into executable and arguments.
     * Handles quoted strings properly.
     */
    private static String[] parseCommand(String command) {
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

    /**
     * Returns cached projects or null if not yet scanned.
     * This ensures we only scan once per application run.
     */
    private static java.util.concurrent.atomic.AtomicReference<java.util.List<String>> getCachedProjects() {
        return new java.util.concurrent.atomic.AtomicReference<>(cachedProjects);
    }

    /**
     * Updates the cached projects after scan completes.
     */
    public static void updateCachedProjects(java.util.List<String> projects) {
        cachedProjects = projects;
    }

    // ============== CLI Subcommands ==============

    @Command(name = "open", aliases = {"o"}, description = "Open project picker to select and navigate to projects")
    static class OpenCommand implements Runnable {
        @Override
        public void run() {
            try {
                ProjectManager.openProject(terminal, lineReader, settings, getCachedProjects());
            } catch (Exception e) {
                System.err.println("Error opening project: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "new", aliases = {"n"}, description = "Create a new project")
    static class NewCommand implements Runnable {
        @Override
        public void run() {
            try {
                ProjectManager.createNewProject(terminal, lineReader, settings);
            } catch (Exception e) {
                System.err.println("Error creating new project: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "clone", aliases = {"g"}, description = "Clone a GitHub repository")
    static class CloneCommand implements Runnable {
        @Override
        public void run() {
            try {
                GitManager.cloneRepository(terminal, lineReader, settings);
            } catch (Exception e) {
                System.err.println("Error cloning repository: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "prp", aliases = {"p"}, description = "Manage Project Requirement Prompts (PRPs)")
    static class PrpCommand implements Runnable {
        @Override
        public void run() {
            try {
                AITemplateManager.manageTemplatesUI(settings);
            } catch (Exception e) {
                System.err.println("Error managing PRPs: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "global-prp", aliases = {"gp"}, description = "Global PRP viewer - browse PRPs across all projects")
    static class GlobalPrpCommand implements Runnable {
        @Override
        public void run() {
            try {
                AITemplateManager.manageGlobalTemplatesUI(settings);
            } catch (Exception e) {
                System.err.println("Error opening Global PRP viewer: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "settings", aliases = {"s"}, description = "Configure application settings")
    static class SettingsCommand implements Runnable {
        @Override
        public void run() {
            try {
                settingsMenu();
            } catch (Exception e) {
                System.err.println("Error accessing settings: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "ide", aliases = {"e"}, description = "Open current directory as project in configured IDE")
    static class IdeCommand implements Runnable {
        @Override
        public void run() {
            try {
                openCurrentDirInIDE();
            } catch (Exception e) {
                System.err.println("Error opening IDE: " + e.getMessage());
                e.printStackTrace();
                System.exit(3);
            }
        }
    }

    @Command(name = "help", aliases = {"h", "-h", "--help"}, description = "Show this help message")
    static class HelpCommand implements Runnable {
        @picocli.CommandLine.Spec
        private picocli.CommandLine.Model.CommandSpec spec;

        @Override
        public void run() {
            // Print header first to ensure visibility
            System.out.println("========================================");
            System.out.println("  Project Build & Dev Tools");
            System.out.println("  Version 1.0");
            System.out.println("========================================");
            System.out.println();

            // Manually print simple help
            System.out.println("Usage: prj [COMMAND]");
            System.out.println();
            System.out.println("Commands:");
            System.out.println("  open, o       Open project picker to select and navigate to projects");
            System.out.println("  new, n        Create a new project");
            System.out.println("  clone, g      Clone a GitHub repository");
            System.out.println("  prp, p        Manage Project Requirement Prompts (PRPs)");
            System.out.println("  global-prp, gp  Global PRP viewer - browse PRPs across all projects");
            System.out.println("  settings, s   Configure application settings");
            System.out.println("  ide, e        Open current directory as project in configured IDE");
            System.out.println("  help, h       Show this help message");
            System.out.println();
            System.out.println("Run 'prj' without arguments to use interactive menu mode.");
            System.out.println();
            System.out.flush();
        }
    }
}
