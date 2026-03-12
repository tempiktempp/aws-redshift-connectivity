package com.edp.api.registry;

import com.edp.api.definition.TableDefinition;
import com.edp.api.exception.TableNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-discovers and indexes all TableDefinition beans.
 *
 * Spring injects all TableDefinition implementations
 * automatically via constructor injection.
 * No manual registration needed — just add @Component
 * to a new TableDefinition and it appears here.
 *
 * Security note:
 *   Only tables with a registered TableDefinition
 *   are accessible. All others throw TableNotFoundException
 *   before any SQL is built — unknown tables can never
 *   be queried regardless of what the caller sends.
 */
@Slf4j
@Component
public class TableRegistry {

    private final Map<String, TableDefinition> registry;

    public TableRegistry(
            List<TableDefinition> tableDefinitions) {

        this.registry = tableDefinitions.stream()
                .collect(Collectors.toMap(
                        TableDefinition::getRegistryKey,
                        Function.identity()));

        log.info("[TableRegistry] Registered {} table(s): {}",
                registry.size(),
                registry.keySet());
    }

    /**
     * Looks up a TableDefinition by schema and table name.
     *
     * @throws TableNotFoundException if no definition exists
     */
    public TableDefinition getDefinition(
            String schema,
            String table) {

        String key = schema + "." + table;
        TableDefinition definition = registry.get(key);

        if (definition == null) {
            log.warn("[TableRegistry] No definition " +
                    "found for: {}", key);
            throw new TableNotFoundException(schema, table);
        }

        log.debug("[TableRegistry] Found definition " +
                "for: {}", key);

        return definition;
    }

    /**
     * Returns true if a definition is registered
     * for the given schema and table.
     */
    public boolean isRegistered(
            String schema,
            String table) {
        return registry.containsKey(
                schema + "." + table);
    }

    /**
     * Returns all registered table keys.
     * Useful for admin/diagnostic endpoints.
     */
    public java.util.Set<String> getRegisteredTables() {
        return registry.keySet();
    }
}
```

---

### After adding this — verify startup log

When the app starts you should see:
```
[TableRegistry] Registered 2 table(s): 
    [crm.interaction, crm.contact]
