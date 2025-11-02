package xyz.jphil.ai.proj_build_dev_misc_tools;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectManager {

    /**
     * Opens a project from ALL organizations in the coderepos directory,
     * not just the ones configured in settings.
     */
    public static void openProjectAllOrganizations(Terminal terminal, LineReader lineReader, Settings settings) {
        terminal.writer().println("\n--- Open Project (All Organizations) ---");

        Path codeReposPath = Paths.get(settings.getCodeReposPath());
        if (!Files.exists(codeReposPath) || !Files.isDirectory(codeReposPath)) {
            terminal.writer().println("Code repositories path does not exist: " + codeReposPath);
            terminal.writer().flush();
            return;
        }

        // List all directories in coderepos path as potential organizations
        List<String> allOrganizations = listAllOrganizations(codeReposPath);

        if (allOrganizations.isEmpty()) {
            terminal.writer().println("No organizations found in: " + codeReposPath);
            terminal.writer().flush();
            return;
        }

        // Show organization selection
        terminal.writer().println("\nAll Organizations in " + codeReposPath + ":");
        for (int i = 0; i < allOrganizations.size(); i++) {
            terminal.writer().println((i + 1) + ". " + allOrganizations.get(i));
        }
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String choice = lineReader.readLine("Select organization: ").trim();
        if ("0".equals(choice)) {
            return;
        }

        String selectedOrgName;
        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < allOrganizations.size()) {
                selectedOrgName = allOrganizations.get(index);
            } else {
                terminal.writer().println("Invalid selection.");
                terminal.writer().flush();
                return;
            }
        } catch (NumberFormatException e) {
            terminal.writer().println("Invalid input.");
            terminal.writer().flush();
            return;
        }

        // Now proceed with project selection from the selected organization
        Path orgPath = codeReposPath.resolve(selectedOrgName);

        // Ask user for sorting preference
        terminal.writer().println("\nSort projects by:");
        terminal.writer().println("1. Alphabetically (A-Z)");
        terminal.writer().println("2. Recently modified (newest first)");
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String sortChoice = lineReader.readLine("Select sorting option: ").trim();
        if ("0".equals(sortChoice)) {
            return;
        }

        boolean sortByRecency = "2".equals(sortChoice);
        List<String> projects = listProjects(orgPath, sortByRecency);

        if (projects.isEmpty()) {
            terminal.writer().println("No projects found in organization: " + selectedOrgName);
            terminal.writer().flush();
            return;
        }

        String sortLabel = sortByRecency ? " (sorted by recency)" : " (sorted alphabetically)";
        terminal.writer().println("\nProjects in " + selectedOrgName + sortLabel + ":");
        for (int i = 0; i < projects.size(); i++) {
            terminal.writer().println((i + 1) + ". " + projects.get(i));
        }
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String projectChoice = lineReader.readLine("Select project: ").trim();
        if ("0".equals(projectChoice)) {
            return;
        }

        try {
            int index = Integer.parseInt(projectChoice) - 1;
            if (index >= 0 && index < projects.size()) {
                String selectedProject = projects.get(index);
                Path projectPath = orgPath.resolve(selectedProject);
                boolean shouldExit = openProjectOptions(terminal, lineReader, settings, projectPath);
                if (shouldExit) {
                    // Exit the entire program
                    System.exit(0);
                }
            } else {
                terminal.writer().println("Invalid selection.");
                terminal.writer().flush();
            }
        } catch (NumberFormatException e) {
            terminal.writer().println("Invalid input.");
            terminal.writer().flush();
        }
    }

    public static void openProject(Terminal terminal, LineReader lineReader, Settings settings) {
        terminal.writer().println("\n--- Open Project ---");

        Organization selectedOrg = selectOrganization(terminal, lineReader, settings);
        if (selectedOrg == null) {
            return;
        }

        Path orgPath = Paths.get(settings.getCodeReposPath(), selectedOrg.getName());
        if (!Files.exists(orgPath) || !Files.isDirectory(orgPath)) {
            terminal.writer().println("Organization directory does not exist: " + orgPath);
            terminal.writer().flush();
            return;
        }

        // Ask user for sorting preference
        terminal.writer().println("\nSort projects by:");
        terminal.writer().println("1. Alphabetically (A-Z)");
        terminal.writer().println("2. Recently modified (newest first)");
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String sortChoice = lineReader.readLine("Select sorting option: ").trim();
        if ("0".equals(sortChoice)) {
            return;
        }

        boolean sortByRecency = "2".equals(sortChoice);
        List<String> projects = listProjects(orgPath, sortByRecency);

        if (projects.isEmpty()) {
            terminal.writer().println("No projects found in organization: " + selectedOrg.getName());
            terminal.writer().flush();
            return;
        }

        String sortLabel = sortByRecency ? " (sorted by recency)" : " (sorted alphabetically)";
        terminal.writer().println("\nProjects in " + selectedOrg.getName() + sortLabel + ":");
        for (int i = 0; i < projects.size(); i++) {
            terminal.writer().println((i + 1) + ". " + projects.get(i));
        }
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String choice = lineReader.readLine("Select project: ").trim();
        if ("0".equals(choice)) {
            return;
        }

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < projects.size()) {
                String selectedProject = projects.get(index);
                Path projectPath = orgPath.resolve(selectedProject);
                boolean shouldExit = openProjectOptions(terminal, lineReader, settings, projectPath);
                if (shouldExit) {
                    // Exit the entire program
                    System.exit(0);
                }
            } else {
                terminal.writer().println("Invalid selection.");
                terminal.writer().flush();
            }
        } catch (NumberFormatException e) {
            terminal.writer().println("Invalid input.");
            terminal.writer().flush();
        }
    }

    private static boolean openProjectOptions(Terminal terminal, LineReader lineReader, Settings settings, Path projectPath) {
        terminal.writer().println("\n--- Open: " + projectPath.getFileName() + " ---");
        terminal.writer().println("1. Change to directory in THIS terminal and exit");
        terminal.writer().println("2. Open in Command Prompt (new window)");
        terminal.writer().println("3. Open in File Explorer");
        terminal.writer().println("4. Open in NetBeans");
        terminal.writer().println("5. Open in Default Editor");
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String choice = lineReader.readLine("Select option: ").trim();

        try {
            switch (choice) {
                case "1":
                    changeToDirectoryInCurrentTerminal(terminal, projectPath);
                    return true; // Signal to exit the program
                case "2":
                    openInCmd(projectPath);
                    terminal.writer().println("Opening in command prompt...");
                    terminal.writer().flush();
                    break;
                case "3":
                    openInExplorer(projectPath);
                    terminal.writer().println("Opening in file explorer...");
                    terminal.writer().flush();
                    break;
                case "4":
                    openInNetbeans(settings, projectPath);
                    terminal.writer().println("Opening in NetBeans...");
                    terminal.writer().flush();
                    break;
                case "5":
                    openInEditor(settings, projectPath);
                    terminal.writer().println("Opening in editor...");
                    terminal.writer().flush();
                    break;
                case "0":
                    return false;
                default:
                    terminal.writer().println("Invalid option.");
                    terminal.writer().flush();
            }
        } catch (IOException e) {
            terminal.writer().println("Error: " + e.getMessage());
            terminal.writer().flush();
        }
        return false;
    }

    private static void openInCmd(Path projectPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe", "/k", "cd", "/d", projectPath.toString());
            pb.start();
        } else {
            ProcessBuilder pb = new ProcessBuilder("x-terminal-emulator");
            pb.directory(projectPath.toFile());
            pb.start();
        }
    }

    private static void openInExplorer(Path projectPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            Runtime.getRuntime().exec("explorer.exe \"" + projectPath.toString() + "\"");
        } else if (os.contains("mac")) {
            Runtime.getRuntime().exec("open \"" + projectPath.toString() + "\"");
        } else {
            Runtime.getRuntime().exec("xdg-open \"" + projectPath.toString() + "\"");
        }
    }

    private static void openInNetbeans(Settings settings, Path projectPath) throws IOException {
        if (settings.getNetbeansPath() != null && !settings.getNetbeansPath().isEmpty()) {
            ProcessBuilder pb = new ProcessBuilder(settings.getNetbeansPath(), "--open", projectPath.toString());
            pb.start();
        } else {
            throw new IOException("NetBeans path not configured in settings.");
        }
    }

    private static void openInEditor(Settings settings, Path projectPath) throws IOException {
        String editor = settings.getDefaultEditor();
        if (editor != null && !editor.isEmpty()) {
            ProcessBuilder pb = new ProcessBuilder(editor, projectPath.toString());
            pb.start();
        } else {
            throw new IOException("Default editor not configured in settings.");
        }
    }

    private static void changeToDirectoryInCurrentTerminal(Terminal terminal, Path projectPath) {
        if (!TerminalHelper.isPostrunBatchAvailable()) {
            terminal.writer().println("\nERROR: Directory change feature is not available.");
            terminal.writer().println("This feature requires running the program via proj.bat wrapper script.");
            terminal.writer().println("\nAlternative: Use option 2 to open in a new command prompt window.");
            terminal.writer().flush();
            return;
        }

        boolean success = TerminalHelper.writePostrunBatch(projectPath);

        if (success) {
            terminal.writer().println("\n✓ Directory change scheduled!");
            terminal.writer().println("\nExiting program. Your terminal will change to:");
            terminal.writer().println("  " + projectPath.toAbsolutePath());
            terminal.writer().flush();
        } else {
            terminal.writer().println("\n✗ Failed to schedule directory change.");
            terminal.writer().println("\nTry using option 2 to open the project in a new window instead.");
            terminal.writer().flush();
        }
    }

    public static void createNewProject(Terminal terminal, LineReader lineReader, Settings settings) {
        terminal.writer().println("\n--- Create New Project ---");

        Organization selectedOrg = selectOrganization(terminal, lineReader, settings);
        if (selectedOrg == null) {
            return;
        }

        Path orgPath = Paths.get(settings.getCodeReposPath(), selectedOrg.getName());

        terminal.writer().println("\nExisting projects in " + selectedOrg.getName() + ":");
        if (Files.exists(orgPath)) {
            List<String> projects = listProjects(orgPath, false); // Alphabetical for new project creation
            if (!projects.isEmpty()) {
                for (String project : projects) {
                    terminal.writer().print(project + "  ");
                }
                terminal.writer().println();
            } else {
                terminal.writer().println("(No existing projects)");
            }
        } else {
            terminal.writer().println("(Organization directory will be created)");
        }
        terminal.writer().flush();

        String projectName = lineReader.readLine("\nEnter new project name: ").trim();
        if (projectName.isEmpty()) {
            terminal.writer().println("Project name cannot be empty.");
            terminal.writer().flush();
            return;
        }

        String defaultGroupId = selectedOrg.getGroupId() != null ? selectedOrg.getGroupId() : "com.example";
        terminal.writer().println("Suggested Group ID: " + defaultGroupId);
        terminal.writer().flush();

        String groupId = lineReader.readLine("Enter Group ID (press Enter for default): ").trim();
        if (groupId.isEmpty()) {
            groupId = defaultGroupId;
        }

        Path projectPath = orgPath.resolve(projectName);
        if (Files.exists(projectPath)) {
            terminal.writer().println("Project already exists: " + projectPath);
            terminal.writer().flush();
            return;
        }

        try {
            Files.createDirectories(projectPath);
            terminal.writer().println("Project directory created: " + projectPath);
            terminal.writer().println("Group ID: " + groupId);
            terminal.writer().println("Project Name: " + projectName);
            terminal.writer().println("\nNote: You can now use 'Git Clone Repository' or manually initialize the project.");
            terminal.writer().flush();
        } catch (IOException e) {
            terminal.writer().println("Error creating project: " + e.getMessage());
            terminal.writer().flush();
        }
    }

    static Organization selectOrganization(Terminal terminal, LineReader lineReader, Settings settings) {
        List<Organization> organizations = settings.getOrganizations();

        if (organizations.isEmpty()) {
            terminal.writer().println("No organizations configured. Please add organizations in Settings.");
            terminal.writer().flush();
            return null;
        }

        terminal.writer().println("\nSelect Organization:");
        for (int i = 0; i < organizations.size(); i++) {
            terminal.writer().println((i + 1) + ". " + organizations.get(i).getName());
        }
        terminal.writer().println("0. Cancel");
        terminal.writer().flush();

        String choice = lineReader.readLine("Select organization: ").trim();
        if ("0".equals(choice)) {
            return null;
        }

        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < organizations.size()) {
                return organizations.get(index);
            } else {
                terminal.writer().println("Invalid selection.");
                terminal.writer().flush();
                return null;
            }
        } catch (NumberFormatException e) {
            terminal.writer().println("Invalid input.");
            terminal.writer().flush();
            return null;
        }
    }

    /**
     * Lists all directories in the coderepos path as potential organizations.
     * Returns a sorted list of directory names.
     */
    private static List<String> listAllOrganizations(Path codeReposPath) {
        try {
            return Files.list(codeReposPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted() // Sort alphabetically
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static List<String> listProjects(Path orgPath, boolean sortByRecency) {
        try {
            List<Path> projectPaths = Files.list(orgPath)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());

            if (sortByRecency) {
                // Sort by last modified time (newest first)
                projectPaths.sort((p1, p2) -> {
                    try {
                        BasicFileAttributes attrs1 = Files.readAttributes(p1, BasicFileAttributes.class);
                        BasicFileAttributes attrs2 = Files.readAttributes(p2, BasicFileAttributes.class);
                        // Compare in reverse order (newest first)
                        return attrs2.lastModifiedTime().compareTo(attrs1.lastModifiedTime());
                    } catch (IOException e) {
                        return 0; // If error reading attributes, consider them equal
                    }
                });
            } else {
                // Sort alphabetically
                projectPaths.sort(Comparator.comparing(p -> p.getFileName().toString()));
            }

            return projectPaths.stream()
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
