package xyz.jphil.ai.proj_build_dev_misc_tools;

import java.nio.file.Path;

public class TerminalHelper {

    /**
     * Write a postrun batch file to change the parent terminal's directory.
     * This works by writing commands to a batch file that the parent proj.bat
     * will execute after the Java program exits.
     *
     * @param targetDirectory The directory to change to
     * @return true if the postrun batch file was successfully written, false otherwise
     */
    public static boolean writePostrunBatch(Path targetDirectory) {
        String postrunBatchPath = System.getenv("PROJ_POSTRUN_BATCH");

        if (postrunBatchPath == null || postrunBatchPath.isEmpty()) {
            // Not running from proj.bat, this feature is not available
            return false;
        }

        try {
            // Write the batch file that will change directory and delete itself
            java.io.PrintWriter writer = new java.io.PrintWriter(postrunBatchPath, "UTF-8");
            writer.println("@echo off");
            writer.println("cd /d \"" + targetDirectory.toAbsolutePath().toString() + "\"");
            writer.println("echo.");
            writer.println("echo Successfully changed directory to:");
            writer.println("echo   %CD%");
            writer.println("echo.");
            // Delete self - use explicit path
            writer.println("del \"" + postrunBatchPath + "\" 2>nul");
            writer.close();
            return true;
        } catch (Exception e) {
            System.err.println("Error writing postrun batch file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the postrun batch feature is available.
     * This is true when the program is launched via proj.bat which sets the
     * PROJ_POSTRUN_BATCH environment variable.
     *
     * @return true if postrun batch feature is available
     */
    public static boolean isPostrunBatchAvailable() {
        String postrunBatchPath = System.getenv("PROJ_POSTRUN_BATCH");
        return postrunBatchPath != null && !postrunBatchPath.isEmpty();
    }
}
