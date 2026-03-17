// ── VIEW — security view ───────────────────────────
templates.put("my_team_view",
        FilterTemplate.builder()
            .name("my_team_view")
            .templateType(TemplateType.VIEW)
            // View is in security schema
            // Java passes source_employee_id
            // as WHERE clause on top
            .sqlFragment(
                "SELECT * FROM " +
                "security.v_interaction_entitled " +
                "WHERE source_employee_id = :employeeId")
            .templateParam(TemplateParam.builder()
                .paramName("employeeId")
                .fromEmployeeHeader(true)
                .build())
            .allowedParam("interaction_type")
            .allowedParam("status")
            .build());
