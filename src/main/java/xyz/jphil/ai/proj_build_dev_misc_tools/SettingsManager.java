package xyz.jphil.ai.proj_build_dev_misc_tools;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String SETTINGS_DIR = "xyz-jphil" + File.separator + "ai" + File.separator + "proj-build_dev_misc_tools";
    private static final String SETTINGS_FILE = "Settings.xml";

    private Path getSettingsPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, SETTINGS_DIR, SETTINGS_FILE);
    }

    public Settings loadSettings() {
        Path settingsPath = getSettingsPath();

        if (!Files.exists(settingsPath)) {
            createTemplateSettings();
            return null;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(settingsPath.toFile());
            doc.getDocumentElement().normalize();

            Settings settings = new Settings();

            settings.setCodeReposPath(getElementText(doc, "codeReposPath"));
            settings.setNetbeansPath(getElementText(doc, "netbeansPath"));
            settings.setDefaultEditor(getElementText(doc, "defaultEditor"));

            NodeList orgList = doc.getElementsByTagName("organization");
            List<Organization> organizations = new ArrayList<>();
            for (int i = 0; i < orgList.getLength(); i++) {
                Node node = orgList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element orgElement = (Element) node;
                    String name = getChildElementText(orgElement, "name");
                    String groupId = getChildElementText(orgElement, "groupId");
                    if (name != null && !name.isEmpty()) {
                        organizations.add(new Organization(name, groupId));
                    }
                }
            }
            settings.setOrganizations(organizations);

            String prpTemplate = getElementText(doc, "prpTemplate");
            if (prpTemplate != null && !prpTemplate.isEmpty()) {
                settings.setPrpTemplate(prpTemplate);
            }

            return settings;
        } catch (Exception e) {
            System.err.println("Error loading settings: " + e.getMessage());
            return null;
        }
    }

    private String getElementText(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private String getChildElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    public void saveSettings(Settings settings) {
        try {
            Path settingsPath = getSettingsPath();
            Files.createDirectories(settingsPath.getParent());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("settings");
            doc.appendChild(root);

            appendChild(doc, root, "codeReposPath", settings.getCodeReposPath());
            appendChild(doc, root, "netbeansPath", settings.getNetbeansPath());
            appendChild(doc, root, "defaultEditor", settings.getDefaultEditor());

            Element orgsElement = doc.createElement("organizations");
            root.appendChild(orgsElement);
            for (Organization org : settings.getOrganizations()) {
                Element orgElement = doc.createElement("organization");
                appendChild(doc, orgElement, "name", org.getName());
                appendChild(doc, orgElement, "groupId", org.getGroupId());
                orgsElement.appendChild(orgElement);
            }

            appendChild(doc, root, "prpTemplate", settings.getPrpTemplate());

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(settingsPath.toFile());
            transformer.transform(source, result);

        } catch (Exception e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    private void appendChild(Document doc, Element parent, String name, String value) {
        if (value != null) {
            Element element = doc.createElement(name);
            element.setTextContent(value);
            parent.appendChild(element);
        }
    }

    private void createTemplateSettings() {
        try {
            Path settingsPath = getSettingsPath();
            Files.createDirectories(settingsPath.getParent());

            String os = System.getProperty("os.name").toLowerCase();
            String exampleCodePath = os.contains("win") ? "C:\\Users\\YourName\\code" : System.getProperty("user.home") + "/code";
            String exampleEditor = os.contains("win") ? "notepad.exe" : "nano";
            String exampleNetbeans = os.contains("win") ? "C:\\Program Files\\NetBeans\\bin\\netbeans.exe" : "/usr/local/netbeans/bin/netbeans";

            PrintWriter writer = new PrintWriter(settingsPath.toFile(), "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<!--");
            writer.println("  =====================================================");
            writer.println("  Project Build & Dev Misc Tools - Settings File");
            writer.println("  =====================================================");
            writer.println();
            writer.println("  IMPORTANT: This is a TEMPLATE file. Please edit the values below to match your environment.");
            writer.println();
            writer.println("  INSTRUCTIONS:");
            writer.println("  1. Edit the <codeReposPath> to point to your base code directory");
            writer.println("  2. Configure your default editor (notepad.exe, nano, vim, code, etc.)");
            writer.println("  3. Set the NetBeans path if you use NetBeans IDE");
            writer.println("  4. Add your organizations with their corresponding Maven groupIds");
            writer.println();
            writer.println("  STRUCTURE:");
            writer.println();
            writer.println("  <codeReposPath>");
            writer.println("    - Base directory where all your code repositories are stored");
            writer.println("    - Repositories will be organized as: <codeReposPath>/<organization>/<repository>");
            writer.println("    - Example Windows: C:\\Users\\YourName\\code");
            writer.println("    - Example Linux/Mac: /home/username/code");
            writer.println();
            writer.println("  <defaultEditor>");
            writer.println("    - Command to launch your preferred text editor");
            writer.println("    - Windows examples: notepad.exe, notepad++.exe, code (VS Code)");
            writer.println("    - Linux examples: nano, vim, gedit, code");
            writer.println();
            writer.println("  <netbeansPath>");
            writer.println("    - Full path to NetBeans executable (optional, only if you use NetBeans)");
            writer.println("    - Leave empty if you don't use NetBeans");
            writer.println();
            writer.println("  <organizations>");
            writer.println("    - List of GitHub organizations you work with");
            writer.println("    - Each organization has:");
            writer.println("      <name>: GitHub organization name (e.g., your-org, apache, etc.)");
            writer.println("      <groupId>: Maven groupId for projects in this org (e.g., io.github.your-org, org.apache, com.example)");
            writer.println("    - You can add multiple <organization> entries");
            writer.println();
            writer.println("  <prpTemplate>");
            writer.println("    - Template used when creating new PRP (Project Requirement Prompt) files");
            writer.println("    - Placeholders: %index% (PRP number), %name% (PRP name)");
            writer.println("    - Usually you don't need to modify this unless you want custom PRP format");
            writer.println();
            writer.println("  =====================================================");
            writer.println("-->");
            writer.println("<settings>");
            writer.println();
            writer.println("    <!-- Base directory for all code repositories -->");
            writer.println("    <codeReposPath>" + exampleCodePath + "</codeReposPath>");
            writer.println();
            writer.println("    <!-- Default text editor command -->");
            writer.println("    <defaultEditor>" + exampleEditor + "</defaultEditor>");
            writer.println();
            writer.println("    <!-- NetBeans IDE path (optional, leave empty if not used) -->");
            writer.println("    <netbeansPath>" + exampleNetbeans + "</netbeansPath>");
            writer.println();
            writer.println("    <!-- Organizations and their Maven groupIds -->");
            writer.println("    <organizations>");
            writer.println("        <!-- Example organization entry -->");
            writer.println("        <organization>");
            writer.println("            <name>your-github-org</name>");
            writer.println("            <groupId>io.github.your-github-org</groupId>");
            writer.println("        </organization>");
            writer.println();
            writer.println("        <!-- Add more organizations as needed -->");
            writer.println("        <!--");
            writer.println("        <organization>");
            writer.println("            <name>another-org</name>");
            writer.println("            <groupId>com.example.another</groupId>");
            writer.println("        </organization>");
            writer.println("        -->");
            writer.println("    </organizations>");
            writer.println();
            writer.println("    <!-- PRP Template (usually no need to modify) -->");
            writer.println("    <prpTemplate>PRP Number: %index%");
            writer.println("PRP Name: %name%");
            writer.println("Usage Guide: ");
            writer.println("\t- This is a Project Requirement Prompt (PRP). This file contains AI prompts that are intended to define certain requirement(s) for this project. ");
            writer.println("\t- Claude Code (or any other coding AI agents) will be working on implementing this requirement in this project when told by the user. For AI coding agents this file is READ-ONLY and MUST NOT be modified by AI coding agents. ");
            writer.println("\t- Once this PRP is completed (or temporarily stalled), it is renamed to `%index%-prp-%name%.closed.md`. ");
            writer.println("\t- The file `%index%-prp.status.md` carries the status update for this prp, which is to be written/updated by the AI coding agents working on this prp.");
            writer.println("</prpTemplate>");
            writer.println();
            writer.println("</settings>");
            writer.close();

        } catch (Exception e) {
            System.err.println("Error creating template settings: " + e.getMessage());
        }
    }

    public Path getSettingsFilePath() {
        return getSettingsPath();
    }

    public boolean isConfigured() {
        Settings settings = loadSettings();
        if (settings == null) {
            return false;
        }
        return settings.getCodeReposPath() != null && !settings.getCodeReposPath().isEmpty()
                && !settings.getOrganizations().isEmpty();
    }
}
