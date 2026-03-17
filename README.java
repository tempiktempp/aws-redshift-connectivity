CREATE OR REPLACE VIEW security.v_interaction_entitled AS
SELECT
    i.id                AS interaction_id,
    i.employee_id,
    i.interaction_date,
    i.interaction_type,
    i.createddate,
    ent.source_employee_id
FROM crm.interaction i
INNER JOIN security.v_team_entitlement ent
    ON ent.team_member_id = i.employee_id;
