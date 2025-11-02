package xyz.jphil.ai.proj_build_dev_misc_tools;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class Main {
    private static Settings settings;
    private static SettingsManager settingsManager;
    private static Terminal terminal;
    private static LineReader lineReader;

    public static void main(String[] args) {
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();

            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            settingsManager = new SettingsManager();
            settings = settingsManager.loadSettings();

            printBanner();

            if (settings == null) {
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

                String openNow = lineReader.readLine("Open settings file now? (y/n): ").trim();
                if (openNow.equalsIgnoreCase("y")) {
                    try {
                        String os = System.getProperty("os.name").toLowerCase();
                        if (os.contains("win")) {
                            Runtime.getRuntime().exec("notepad.exe \"" + settingsManager.getSettingsFilePath() + "\"");
                        } else {
                            Runtime.getRuntime().exec("nano \"" + settingsManager.getSettingsFilePath() + "\"");
                        }
                        terminal.writer().println("Opening settings file...");
                        terminal.writer().flush();
                    } catch (IOException e) {
                        terminal.writer().println("Could not open editor: " + e.getMessage());
                        terminal.writer().flush();
                    }
                }
                return;
            }

            mainMenu();

        } catch (IOException e) {
            System.err.println("Error initializing terminal: " + e.getMessage());
            e.printStackTrace();
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
        while (true) {
            terminal.writer().println("\n--- Main Menu ---");
            terminal.writer().println("1. Open Project (Configured Organizations)");
            terminal.writer().println("2. Open Project (All Organizations)");
            terminal.writer().println("3. Git Clone Repository");
            terminal.writer().println("4. Create New Project");
            terminal.writer().println("5. AI Template Management");
            terminal.writer().println("6. Settings");
            terminal.writer().println("0. Exit");
            terminal.writer().println();
            terminal.writer().flush();

            String choice = lineReader.readLine("Select option: ").trim();

            switch (choice) {
                case "1":
                    ProjectManager.openProject(terminal, lineReader, settings);
                    break;
                case "2":
                    ProjectManager.openProjectAllOrganizations(terminal, lineReader, settings);
                    break;
                case "3":
                    GitManager.cloneRepository(terminal, lineReader, settings);
                    break;
                case "4":
                    ProjectManager.createNewProject(terminal, lineReader, settings);
                    break;
                case "5":
                    AITemplateManager.manageTemplates(terminal, lineReader, settings);
                    break;
                case "6":
                    settingsMenu();
                    break;
                case "0":
                    terminal.writer().println("Goodbye!");
                    terminal.writer().flush();
                    return;
                default:
                    terminal.writer().println("Invalid option. Please try again.");
                    terminal.writer().flush();
            }
        }
    }

    private static void settingsMenu() {
        while (true) {
            terminal.writer().println("\n--- Settings ---");
            terminal.writer().println("Current Settings:");
            terminal.writer().println("  Code Repos Path: " + settings.getCodeReposPath());
            terminal.writer().println("  Netbeans Path: " + settings.getNetbeansPath());
            terminal.writer().println("  Default Editor: " + settings.getDefaultEditor());
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
            terminal.writer().println("1. Edit Code Repos Path");
            terminal.writer().println("2. Edit Netbeans Path");
            terminal.writer().println("3. Edit Default Editor");
            terminal.writer().println("4. Manage Organizations");
            terminal.writer().println("5. Show Settings File Location");
            terminal.writer().println("6. Reload Settings from File");
            terminal.writer().println("0. Back to Main Menu");
            terminal.writer().println();
            terminal.writer().flush();

            String choice = lineReader.readLine("Select option: ").trim();

            switch (choice) {
                case "1":
                    String newPath = lineReader.readLine("Enter new Code Repos Path: ");
                    settings.setCodeReposPath(newPath);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "2":
                    String newNetbeansPath = lineReader.readLine("Enter new Netbeans Path: ");
                    settings.setNetbeansPath(newNetbeansPath);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "3":
                    String newEditor = lineReader.readLine("Enter new Default Editor: ");
                    settings.setDefaultEditor(newEditor);
                    settingsManager.saveSettings(settings);
                    terminal.writer().println("Settings saved.");
                    terminal.writer().flush();
                    break;
                case "4":
                    manageOrganizations();
                    break;
                case "5":
                    terminal.writer().println("Settings file: " + settingsManager.getSettingsFilePath());
                    terminal.writer().flush();
                    break;
                case "6":
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
                    return;
                default:
                    terminal.writer().println("Invalid option.");
                    terminal.writer().flush();
            }
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
            terminal.writer().println("1. Add Organization");
            terminal.writer().println("2. Edit Organization");
            terminal.writer().println("3. Remove Organization");
            terminal.writer().println("0. Back");
            terminal.writer().println();
            terminal.writer().flush();

            String choice = lineReader.readLine("Select option: ").trim();

            switch (choice) {
                case "1":
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
                    return;

                default:
                    terminal.writer().println("Invalid option.");
                    terminal.writer().flush();
            }
        }
    }
}
