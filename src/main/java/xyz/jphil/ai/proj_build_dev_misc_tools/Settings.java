package xyz.jphil.ai.proj_build_dev_misc_tools;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    private String codeReposPath;
    private int projectScanningDepth = 4; // Default depth for scanning project directories
    private String singleFileEditor; // Editor for single files (PRPs, settings, etc.)
    private String ideLauncher; // IDE command template for opening projects (%PATH% placeholder)
    private List<Organization> organizations;
    private String prpTemplate;

    public Settings() {
        this.organizations = new ArrayList<>();
        this.prpTemplate = "PRP Number: %index%\n" +
                "PRP Name: %name%\n" +
                "Usage Guide: \n" +
                "\t- This is a Project Requirement Prompt (PRP). This file contains AI prompts that are intended to define certain requirement(s) for this project. \n" +
                "\t- Claude Code (or any other coding AI agents) will be working on implementing this requirement in this project when told by the user. For AI coding agents this file is READ-ONLY and MUST NOT be modified by AI coding agents. \n" +
                "\t- Once this PRP is completed (or temporarily stalled), it is renamed to `%index%-prp-%name%.closed.md`. \n" +
                "\t- The file `%index%-prp.status.md` carries the status update for this prp, which is to be written/updated by the AI coding agents working on this prp.\n";
    }

    public String getCodeReposPath() {
        return codeReposPath;
    }

    public void setCodeReposPath(String codeReposPath) {
        this.codeReposPath = codeReposPath;
    }

    public String getSingleFileEditor() {
        return singleFileEditor;
    }

    public void setSingleFileEditor(String singleFileEditor) {
        this.singleFileEditor = singleFileEditor;
    }

    public String getIdeLauncher() {
        return ideLauncher;
    }

    public void setIdeLauncher(String ideLauncher) {
        this.ideLauncher = ideLauncher;
    }

    public List<Organization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<Organization> organizations) {
        this.organizations = organizations;
    }

    public String getPrpTemplate() {
        return prpTemplate;
    }

    public void setPrpTemplate(String prpTemplate) {
        this.prpTemplate = prpTemplate;
    }

    public int getProjectScanningDepth() {
        return projectScanningDepth;
    }

    public void setProjectScanningDepth(int projectScanningDepth) {
        this.projectScanningDepth = projectScanningDepth;
    }
}
