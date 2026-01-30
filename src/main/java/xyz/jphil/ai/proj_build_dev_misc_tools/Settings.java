package xyz.jphil.ai.proj_build_dev_misc_tools;

import java.util.ArrayList;
import java.util.List;

public class Settings {
    private String codeReposPath;
    private int projectScanningDepth = 4; // Default depth for scanning project directories
    private String singleFileEditor; // Editor for single files (PRPs, settings, etc.)
    private String ideLauncher; // IDE command template for opening projects (%PATH% placeholder)
    private List<Organization> organizations;
    private String prpTemplateSrc; // URL or local path to PRP template file
    private String statusTemplate;

    public Settings() {
        this.organizations = new ArrayList<>();
        this.prpTemplateSrc = PRPTemplateLoader.DEFAULT_TEMPLATE_SRC; // Default GitHub Pages URL
        this.statusTemplate = "PRP Number: %index%\n" +
                "PRP Name: %name%\n" +
                "Status: IN PROGRESS\n\n" +
                "## Implementation Notes\n\n" +
                "## Completed Tasks\n\n" +
                "## Pending Tasks\n\n";
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

    public String getPrpTemplateSrc() {
        return prpTemplateSrc;
    }

    public void setPrpTemplateSrc(String prpTemplateSrc) {
        this.prpTemplateSrc = prpTemplateSrc;
    }

    public String getStatusTemplate() {
        return statusTemplate;
    }

    public void setStatusTemplate(String statusTemplate) {
        this.statusTemplate = statusTemplate;
    }

    public int getProjectScanningDepth() {
        return projectScanningDepth;
    }

    public void setProjectScanningDepth(int projectScanningDepth) {
        this.projectScanningDepth = projectScanningDepth;
    }
}
