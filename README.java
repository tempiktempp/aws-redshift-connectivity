package com.edp.api.definition;

/**
 * Shared SQL fragments for entitlement-based filtering.
 *
 * Reusable across any TableDefinition that needs
 * team-based access control.
 *
 * All fragments reference security.v_team_entitlement
 * which is owned by the security team in AWS —
 * independently of any Java release.
 */
public final class EntitlementFragments {

    private static final String ENTITLEMENT_VIEW =
            "security.v_team_entitlement";

    private EntitlementFragments() {}

    /**
     * Generates WHERE fragment filtering rows where
     * tableColumn is in the caller's entitled team.
     *
     * Generated SQL:
     *   {tableColumn} IN (
     *       SELECT team_member_id
     *       FROM security.v_team_entitlement
     *       WHERE source_employee_id = :employeeId
     *   )
     *
     * @param tableColumn column in target table to filter
     * @return SQL WHERE fragment
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
     * Same as teamMemberFilter but also filters
     * by team_level.
     *
     * Generated SQL:
     *   {tableColumn} IN (
     *       SELECT team_member_id
     *       FROM security.v_team_entitlement
     *       WHERE source_employee_id = :employeeId
     *       AND   team_level = :teamLevel
     *   )
     *
     * @param tableColumn column in target table to filter
     * @return SQL WHERE fragment
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
