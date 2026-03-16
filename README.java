// Only add employeeId if the template SQL
// actually uses it — avoids unused param error
// from Redshift Data API
if (template.getSqlFragment() != null
        && template.getSqlFragment()
                   .contains(":employeeId")) {
    parameters.put("employeeId",
            request.getEmployeeId());
}
