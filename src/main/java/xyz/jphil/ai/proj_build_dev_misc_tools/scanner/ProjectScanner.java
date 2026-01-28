package xyz.jphil.ai.proj_build_dev_misc_tools.scanner;

import com.arcadedb.database.Database;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.query.sql.executor.ResultSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jline.terminal.Terminal;
import xyz.jphil.ai.proj_build_dev_misc_tools.db.DB;
import xyz.jphil.ai.proj_build_dev_misc_tools.db.ProjectTreeCacheEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans directory tree for projects with configurable depth and caches results in ArcadeDB.
 */
public class ProjectScanner {

    public interface ProgressListener {
        void onProgress(int foldersScanned, int projectsFound);
    }

    private final Gson gson = new Gson();
    private final Path rootPath;
    private final int maxDepth;
    private final Terminal terminal;
    private ProgressListener progressListener;

    private final AtomicInteger foldersScanned = new AtomicInteger(0);
    private final AtomicInteger projectsFound = new AtomicInteger(0);

    public ProjectScanner(Path rootPath, int maxDepth, Terminal terminal) {
        this.rootPath = rootPath;
        this.maxDepth = maxDepth;
        this.terminal = terminal;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Load projects from cache. Returns empty list if cache doesn't exist or is stale.
     */
    public List<String> loadFromCache() {
        try {
            Database db = DB.get().getDatabase();
            long dirHash = rootPath.toString().hashCode();

            String query = "SELECT * FROM ProjectTreeCacheEntry WHERE dirHashCode = ?";
            try (ResultSet rs = db.query("sql", query, dirHash)) {
                if (rs.hasNext()) {
                    var result = rs.next();
                    String jsonProjects = result.getProperty("projectTreeJson");
                    Integer cachedDepth = result.getProperty("scanDepth");

                    // Invalidate cache if depth changed
                    if (cachedDepth != null && cachedDepth == maxDepth && jsonProjects != null) {
                        List<String> projects = gson.fromJson(jsonProjects, new TypeToken<List<String>>(){}.getType());
                        return projects != null ? projects : new ArrayList<>();
                    }
                }
            }
        } catch (Exception e) {
            // Cache miss or error - return empty list
        }
        return new ArrayList<>();
    }

    /**
     * Scan filesystem and update cache. Shows progress on terminal.
     */
    public List<String> scanAndCache() {
        foldersScanned.set(0);
        projectsFound.set(0);

        if (terminal != null && xyz.jphil.ai.proj_build_dev_misc_tools.Main.isVerbose()) {
            terminal.writer().println("\nScanning projects...");
            terminal.writer().flush();
        }

        List<String> allProjects = new ArrayList<>();
        scanRecursive(rootPath, 0, allProjects);

        if (terminal != null && xyz.jphil.ai.proj_build_dev_misc_tools.Main.isVerbose()) {
            terminal.writer().println(String.format(
                "Scan complete: %d projects found (scanned %d folders)",
                projectsFound.get(), foldersScanned.get()));
            terminal.writer().flush();
        }

        // Save to cache
        saveToCache(allProjects);

        return allProjects;
    }

    private void scanRecursive(Path currentPath, int currentDepth, List<String> projects) {
        if (currentDepth >= maxDepth) {
            return;
        }

        try (Stream<Path> entries = Files.list(currentPath)) {
            List<Path> directories = entries
                .filter(Files::isDirectory)
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .collect(Collectors.toList());

            for (Path dir : directories) {
                foldersScanned.incrementAndGet();

                // Show progress every 10 folders
                if (foldersScanned.get() % 10 == 0) {
                    int scanned = foldersScanned.get();
                    int found = projectsFound.get();

                    if (terminal != null && xyz.jphil.ai.proj_build_dev_misc_tools.Main.isVerbose()) {
                        terminal.writer().print(String.format(
                            "\rScanning... %d folders, %d projects found",
                            scanned, found));
                        terminal.writer().flush();
                    }

                    if (progressListener != null) {
                        progressListener.onProgress(scanned, found);
                    }
                }

                // Add as project if it looks like a project directory
                if (isProjectDirectory(dir)) {
                    String relativePath = rootPath.relativize(dir).toString();
                    projects.add(relativePath);
                    projectsFound.incrementAndGet();
                }

                // Recurse into subdirectories
                scanRecursive(dir, currentDepth + 1, projects);
            }
        } catch (IOException e) {
            // Skip directories we can't read
        }
    }

    private boolean isProjectDirectory(Path dir) {
        // Consider it a project if it has common project markers
        return Files.exists(dir.resolve("pom.xml")) ||
               Files.exists(dir.resolve("build.gradle")) ||
               Files.exists(dir.resolve("package.json")) ||
               Files.exists(dir.resolve(".git")) ||
               Files.exists(dir.resolve("README.md")) ||
               Files.exists(dir.resolve("src"));
    }

    private void saveToCache(List<String> projects) {
        try {
            Database db = DB.get().getDatabase();
            long dirHash = rootPath.toString().hashCode();

            ProjectTreeCacheEntry entry = new ProjectTreeCacheEntry()
                .setDirHashCode(dirHash)
                .setDirPath(rootPath.toString())
                .setProjectTreeJson(gson.toJson(projects))
                .setLastUpdated(System.currentTimeMillis())
                .setScanDepth(maxDepth);

            // Delete old entry if exists and insert new one
            db.transaction(() -> {
                String deleteQuery = "DELETE FROM ProjectTreeCacheEntry WHERE dirHashCode = ?";
                db.command("sql", deleteQuery, dirHash);

                // Insert new entry using MutableDocument
                MutableDocument doc = db.newDocument("ProjectTreeCacheEntry");
                doc.set("dirHashCode", entry.getDirHashCode());
                doc.set("dirPath", entry.getDirPath());
                doc.set("projectTreeJson", entry.getProjectTreeJson());
                doc.set("lastUpdated", entry.getLastUpdated());
                doc.set("scanDepth", entry.getScanDepth());
                doc.save();
            });

        } catch (Exception e) {
            System.err.println("Failed to save cache: " + e.getMessage());
        }
    }
}
