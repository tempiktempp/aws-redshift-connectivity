package com.edp.api.definition.tables;

import com.edp.api.definition.ColumnPreset;
import com.edp.api.definition.FilterTemplate;
import com.edp.api.definition.TableDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Table definition for crm.contact.
 *
 * Filter templates:
 *   default → open access, all contacts visible
 *
 * Column presets:
 *   default → core contact columns
 *   summary → minimal columns
 *   full    → all columns
 */
@Component
public class ContactTableDefinition
        implements TableDefinition {

    @Override
    public String getSchema() {
        return "crm";
    }

    @Override
    public String getTable() {
        return "contact";
    }

    @Override
    public String getDefaultFilterTemplate() {
        return "default";
    }

    @Override
    public String getDefaultColumnPreset() {
        return "default";
    }

    @Override
    public Map<String, ColumnPreset> getColumnPresets() {
        return Map.of(

            "default", ColumnPreset.builder()
                    .name("default")
                    .column("id")
                    .column("name")
                    .column("email")
                    .column("status")
                    .column("created_at")
                    .build(),

            "summary", ColumnPreset.builder()
                    .name("summary")
                    .column("id")
                    .column("name")
                    .column("status")
                    .build(),

            "full", ColumnPreset.builder()
                    .name("full")
                    .column("id")
                    .column("name")
                    .column("email")
                    .column("phone")
                    .column("status")
                    .column("created_at")
                    .column("updated_at")
                    .build()
        );
    }

    @Override
    public Map<String, FilterTemplate> getFilterTemplates() {
        return Map.of(

            "default", FilterTemplate.builder()
                    .name("default")
                    .sqlFragment("")
                    .isCte(false)
                    .allowedParam("status")
                    .allowedParam("name")
                    .build()
        );
    }
}
```

---

### Final package structure
```
com/edp/api/
├── controller/
│   └── GenericDataController.java
├── facade/
│   └── DataAccessFacade.java
├── registry/
│   └── TableRegistry.java
├── definition/
│   ├── TableDefinition.java
│   ├── FilterTemplate.java
│   ├── ColumnPreset.java
│   └── tables/
│       ├── InteractionTableDefinition.java
│       └── ContactTableDefinition.java
├── strategy/
│   ├── DataAccessStrategy.java      ← keep for reference
│   ├── OpenAccessStrategy.java      ← can remove
│   └── EntitlementAccessStrategy.java ← can remove
├── query/
│   └── QueryBuilder.java
├── model/
│   ├── request/
│   │   └── DataRequest.java
│   └── response/
│       └── DataResponse.java
└── exception/
    ├── EntitlementException.java
    ├── TableNotFoundException.java
    ├── InvalidTemplateException.java
    ├── InvalidColumnPresetException.java
    ├── InvalidFilterParamException.java
    └── GlobalExceptionHandler.java
```

---

### Test endpoints
```
# Default template, default columns
GET /api/v1/data/crm/interaction
X-Employee-Id: EMP001

# Entitlement template
GET /api/v1/data/crm/interaction/views/my_team
X-Employee-Id: EMP001

# Entitlement template + summary columns
GET /api/v1/data/crm/interaction/views/my_team?columns=summary
X-Employee-Id: EMP001

# Entitlement template + row limit
GET /api/v1/data/crm/interaction/views/my_team?maxResults=10
X-Employee-Id: EMP001

# Unknown view → 400
GET /api/v1/data/crm/interaction/views/unknown_view
X-Employee-Id: EMP001

# Unknown table → 404
GET /api/v1/data/crm/unknown_table
X-Employee-Id: EMP001

# Missing header → 400
GET /api/v1/data/crm/interaction

# Contact — open access
GET /api/v1/data/crm/contact
X-Employee-Id: EMP001

# Contact with filter
GET /api/v1/data/crm/contact?status=ACTIVE
X-Employee-Id: EMP001

# Single row
GET /api/v1/data/crm/interaction/INT001
X-Employee-Id: EMP001
```

---

### Adding a new table — checklist
```
1. Create NewTableDefinition.java in definition/tables/
2. Implement getSchema(), getTable()
3. Define column presets in getColumnPresets()
4. Define filter templates in getFilterTemplates()
5. Set default template and column preset
6. Add @Component
7. Done — zero other changes needed
