WITH team_employees AS (
    -- Step 1: find the team of the requesting employee
    -- Step 2: get all employees in that same team
    SELECT e.emp_id
    FROM dto.entitlement e
    WHERE e.team_id = (
        SELECT team_id
        FROM dto.entitlement
        WHERE emp_id = 'EMP001'
        LIMIT 1
    )
),
entitled_interactions AS (
    -- Step 3: get interactions where a team
    -- employee was the interacting employee
    SELECT
        i.id              AS interaction_id,
        i.employee_id,
        i.interaction_date,
        i.interaction_type,
        i.created_at
    FROM crm.interaction i
    WHERE i.employee_id IN (
        SELECT emp_id FROM team_employees
    )
)
-- Step 4: join with child tables
SELECT
    ei.interaction_id,
    ei.employee_id,
    ei.interaction_date,
    ei.interaction_type,
    ei.created_at,
    s.notes,
    a.emp_id            AS attendee_emp_id,
    a.attendee_name,
    a.attendee_role
FROM entitled_interactions ei
LEFT JOIN crm.interaction_summary s
    ON s.interaction_id = ei.interaction_id
LEFT JOIN crm.interaction_attendee a
    ON a.interaction_id = ei.interaction_id
ORDER BY ei.interaction_date DESC,
         ei.interaction_id
LIMIT 100;


Step 1 — Check entitlement table has data:
SELECT * FROM dto.entitlement LIMIT 10;

Step 2 — Check team lookup works:
SELECT team_id
FROM dto.entitlement
WHERE emp_id = 'EMP001';

Step 3 — Check team employees:
SELECT emp_id
FROM dto.entitlement
WHERE team_id = 'YOUR_TEAM_ID';

Step 4 — Check interactions exist for those employees:
SELECT *
FROM crm.interaction
WHERE employee_id IN ('EMP001', 'EMP002')
LIMIT 10;

  
