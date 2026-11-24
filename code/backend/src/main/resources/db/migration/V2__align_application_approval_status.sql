-- Align the persisted application-state contract with the public/domain term
-- IN_APPROVAL. Workflow runtime states intentionally continue to use PENDING.

ALTER TABLE applications
    ADD CONSTRAINT ck_application_status_v2_transition
        CHECK (status IN ('DRAFT', 'PENDING', 'IN_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN'));

ALTER TABLE applications DROP CHECK ck_application_status;

UPDATE applications
SET status = 'IN_APPROVAL'
WHERE status = 'PENDING';

ALTER TABLE applications
    ADD CONSTRAINT ck_application_status
        CHECK (status IN ('DRAFT', 'IN_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    DROP CHECK ck_application_status_v2_transition;

-- result_status is the application state after the logged action, not the
-- workflow-instance state.
ALTER TABLE approval_logs
    ADD CONSTRAINT ck_approval_result_status_v2_transition
        CHECK (result_status IN ('PENDING', 'IN_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN'));

ALTER TABLE approval_logs DROP CHECK ck_approval_result_status;

UPDATE approval_logs
SET result_status = 'IN_APPROVAL'
WHERE result_status = 'PENDING';

ALTER TABLE approval_logs
    ADD CONSTRAINT ck_approval_result_status
        CHECK (result_status IN ('IN_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    DROP CHECK ck_approval_result_status_v2_transition;

ALTER TABLE workflow_instances
    ADD COLUMN completed_at DATETIME(3) NULL AFTER updated_at,
    ADD CONSTRAINT ck_instance_completion_time
        CHECK (status <> 'PENDING' OR completed_at IS NULL);
