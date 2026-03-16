package com.edp.api.definition;

/**
 * Shared SQL fragments for entitlement-based filtering.
 *
 * These fragments are reusable across any TableDefinition
 * that needs team-based access control.
 *
 * All fragments reference security.v_team_entitlement
 * which is owned and maintained by the security team
 * in AWS — independently of any Java release.
 *
 * Usage in a FilterTemplate:
 *
 *   FilterTemplate.builder()
 *       .name("my_team")
 *       .templateType(TemplateType.STANDARD)
 *       .sqlFragment(
 *           EntitlementFragments.teamMemberFilter(
 *               "employee_id"))
 *       .templateParam(TemplateParam.builder()
 *           .paramName("employeeId")
 *           .source(ParamSource.FROM_EMPLOYEE_HEADER)
 *           .build())
 *       .build()
 */
public final class EntitlementFragments {

    // Security view — owned by AWS/security team
    private static final String ENTITLEMENT_VIEW =
            "security.v_team_entitlement";

    // Private constructor — utility class
    private EntitlementFragments() {}

    /**
     * Generates a WHERE fragment that filters rows
     * where the given column is in the caller's
     * entitled team members.
     *
     * Generated SQL:
     *   {tableColumn} IN (
     *       SELECT team_member_id
     *       FROM security.v_team_entitlement
     *       WHERE source_employee_id = :employeeId
     *   )
     *
     * @param tableColumn the column in the target table
     *                    to filter on e.g. "employee_id"
     * @return SQL WHERE fragment with :employeeId param
     *
     * Usage:
     *   EntitlementFragments.teamMemberFilter("employee_id")
     */
    public static String teamMemberFilter(
            String tableColumn) {
        return tableColumn + " IN (" +
               "SELECT team_member_id " +
               "FROM " + ENTITLEMENT_VIEW + " " +
               "WHERE source_employee_id = :employeeId" +
               ")";
    }

    /**
     * Same as teamMemberFilter but also filters by
     * team_level — useful when you only want direct
     * reports or a specific level of the hierarchy.
     *
     * Generated SQL:
     *   {tableColumn} IN (
     *       SELECT team_member_id
     *       FROM security.v_team_entitlement
     *       WHERE source_employee_id = :employeeId
     *       AND   team_level = :teamLevel
     *   )
     *
     * @param tableColumn the column to filter on
     * @return SQL WHERE fragment with :employeeId
     *         and :teamLevel params
     */
    public static String teamMemberFilterByLevel(
            String tableColumn) {
        return tableColumn + " IN (" +
               "SELECT team_member_id " +
               "FROM " + ENTITLEMENT_VIEW + " " +
               "WHERE source_employee_id = :employeeId " +
               "AND   team_level = :teamLevel" +
               ")";
    }
}
