package xyz.jphil.ai.proj_build_dev_misc_tools.db;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import xyz.jphil.annotation_processor.generate_symbols_from_fields.GenerateSymbols;
import xyz.jphil.arcadedb.initialize_document_schema.TypeDef;

import java.util.Map;

import static xyz.jphil.arcadedb.initialize_document_schema.SchemaBuilder.defType;

/**
 * ArcadeDB model for caching project tree directory entries.
 * Each entry represents the code directory with its nested project structure.
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
@GenerateSymbols
public class ProjectTreeCacheEntry {

    Long dirHashCode;           // Unique ID based on directory path hashcode
    String dirPath;             // Absolute path to the code directory
    String projectTreeJson;     // Serialized project tree structure (JSON array of project paths)
    Long lastUpdated;           // Timestamp of last cache update
    Integer scanDepth;          // Depth used for scanning

    public static TypeDef typeDef() {
        return defType(ProjectTreeCacheEntry.class)
                .fields(ProjectTreeCacheEntrySymbols.FIELDS)
                .unique(ProjectTreeCacheEntrySymbols.$dirHashCode)
            .__();
    }

    public Map<String, Object> map() {
        return ProjectTreeCacheEntrySymbols.non_private_fields_toMap(this);
    }
}
