-- Enterprise approval system initial schema.
-- MySQL 8.x / InnoDB / utf8mb4. Business amounts use integer minor units.

CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    department_code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_department_code UNIQUE (department_code),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES departments (id),
    CONSTRAINT ck_department_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    department_id BIGINT NULL,
    username VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_user_status CHECK (status IN ('PENDING_CONFIRMATION', 'ACTIVE', 'DISABLED')),
    CONSTRAINT ck_active_user_department CHECK (status <> 'ACTIVE' OR department_id IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_role_key UNIQUE (role_key),
    CONSTRAINT ck_role_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    granted_by BIGINT NULL,
    granted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_user_role_grantor FOREIGN KEY (granted_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE membership_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_type VARCHAR(32) NOT NULL,
    requested_department_id BIGINT NULL,
    requested_role_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    pending_user_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN user_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_membership_pending_user UNIQUE (pending_user_id),
    INDEX idx_membership_status_time (status, created_at),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_membership_department FOREIGN KEY (requested_department_id) REFERENCES departments (id),
    CONSTRAINT fk_membership_role FOREIGN KEY (requested_role_id) REFERENCES roles (id),
    CONSTRAINT fk_membership_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_membership_type CHECK (request_type IN ('DEPARTMENT_JOIN', 'ROLE_GRANT')),
    CONSTRAINT ck_membership_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_membership_target CHECK (
        (request_type = 'DEPARTMENT_JOIN' AND requested_department_id IS NOT NULL AND requested_role_id IS NULL)
        OR
        (request_type = 'ROLE_GRANT' AND requested_department_id IS NULL AND requested_role_id IS NOT NULL)
    ),
    CONSTRAINT ck_membership_review CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR status = 'WITHDRAWN'
        OR (status IN ('APPROVED', 'REJECTED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE approval_positions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    duty_key VARCHAR(64) NOT NULL,
    effective_from DATETIME(3) NOT NULL,
    effective_to DATETIME(3) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_position_start UNIQUE (department_id, duty_key, effective_from),
    INDEX idx_position_resolution (department_id, duty_key, status, effective_from, effective_to),
    CONSTRAINT fk_position_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_position_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_position_period CHECK (effective_to IS NULL OR effective_to > effective_from)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE position_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assignment_type VARCHAR(16) NOT NULL,
    assigned_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NULL,
    active_primary_position_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN assignment_type = 'PRIMARY' AND ended_at IS NULL THEN position_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_position_active_primary UNIQUE (active_primary_position_id),
    CONSTRAINT uk_position_assignment_start UNIQUE (position_id, user_id, assigned_at),
    INDEX idx_assignment_user_active (user_id, ended_at, assignment_type),
    CONSTRAINT fk_assignment_position FOREIGN KEY (position_id) REFERENCES approval_positions (id),
    CONSTRAINT fk_assignment_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_assignment_type CHECK (assignment_type IN ('PRIMARY', 'BACKUP')),
    CONSTRAINT ck_assignment_period CHECK (ended_at IS NULL OR ended_at > assigned_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workflow_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_template_key UNIQUE (template_key),
    CONSTRAINT fk_template_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_template_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'DISABLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workflow_template_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(3) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_template_version UNIQUE (template_id, version_no),
    CONSTRAINT fk_template_version_template FOREIGN KEY (template_id) REFERENCES workflow_templates (id),
    CONSTRAINT fk_template_version_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_template_version_number CHECK (version_no > 0),
    CONSTRAINT ck_template_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'DISABLED')),
    CONSTRAINT ck_template_version_publish_time CHECK (
        (status = 'DRAFT' AND published_at IS NULL)
        OR (status IN ('PUBLISHED', 'DISABLED') AND published_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE template_fields (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    label VARCHAR(100) NOT NULL,
    data_type VARCHAR(24) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    validation_rule JSON NULL,
    sequence_no INT NOT NULL,
    CONSTRAINT uk_template_field_key UNIQUE (template_version_id, field_key),
    CONSTRAINT uk_template_field_sequence UNIQUE (template_version_id, sequence_no),
    CONSTRAINT uk_field_id_version UNIQUE (id, template_version_id),
    CONSTRAINT fk_template_field_version FOREIGN KEY (template_version_id) REFERENCES workflow_template_versions (id),
    CONSTRAINT ck_template_field_type CHECK (data_type IN ('TEXT', 'TEXTAREA', 'INTEGER', 'DECIMAL', 'DATE', 'DATETIME', 'BOOLEAN', 'SELECT')),
    CONSTRAINT ck_template_field_sequence CHECK (sequence_no > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE template_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_version_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    approval_mode VARCHAR(16) NOT NULL,
    duty_key VARCHAR(64) NOT NULL,
    assignee_rule JSON NOT NULL,
    condition_rule JSON NULL,
    CONSTRAINT uk_template_node_sequence UNIQUE (template_version_id, sequence_no),
    CONSTRAINT fk_template_node_version FOREIGN KEY (template_version_id) REFERENCES workflow_template_versions (id),
    CONSTRAINT ck_template_node_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_template_node_mode CHECK (approval_mode IN ('SINGLE', 'ALL', 'ANY_ONE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(32) NOT NULL,
    application_type VARCHAR(32) NOT NULL,
    applicant_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    total_amount_minor BIGINT NULL,
    template_version_id BIGINT NULL,
    source_application_id BIGINT NULL,
    row_version INT NOT NULL DEFAULT 0,
    submitted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_application_no UNIQUE (application_no),
    CONSTRAINT uk_application_id_template UNIQUE (id, template_version_id),
    INDEX idx_applicant_status_time (applicant_id, status, created_at),
    INDEX idx_department_type_status (department_id, application_type, status, submitted_at),
    INDEX idx_application_source_status (source_application_id, status),
    CONSTRAINT fk_application_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT fk_application_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_application_template_version FOREIGN KEY (template_version_id) REFERENCES workflow_template_versions (id),
    CONSTRAINT fk_application_source FOREIGN KEY (source_application_id) REFERENCES applications (id),
    CONSTRAINT ck_application_type CHECK (application_type IN ('PROCUREMENT', 'TRAVEL', 'REIMBURSEMENT', 'CUSTOM')),
    CONSTRAINT ck_application_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_application_amount CHECK (total_amount_minor IS NULL OR total_amount_minor >= 0),
    CONSTRAINT ck_application_version CHECK (row_version >= 0),
    CONSTRAINT ck_application_template_type CHECK (
        (application_type = 'CUSTOM' AND template_version_id IS NOT NULL)
        OR (application_type <> 'CUSTOM' AND template_version_id IS NULL)
    ),
    CONSTRAINT ck_application_submit_time CHECK (
        (status = 'DRAFT' AND submitted_at IS NULL)
        OR (status <> 'DRAFT' AND submitted_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- MySQL CHECK expressions cannot reference AUTO_INCREMENT columns. The submit
-- transaction therefore rejects source_application_id = id after locking both
-- application rows; the self-referencing foreign key still enforces existence.

CREATE TABLE procurement_details (
    application_id BIGINT PRIMARY KEY,
    reason TEXT NOT NULL,
    expected_date DATE NOT NULL,
    budget_minor BIGINT NOT NULL,
    chosen_quote_id BIGINT NULL,
    CONSTRAINT fk_procurement_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT ck_procurement_budget CHECK (budget_minor >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE procurement_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    procurement_id BIGINT NOT NULL,
    item_name VARCHAR(160) NOT NULL,
    specification VARCHAR(500) NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    unit_price_minor BIGINT NOT NULL,
    line_total_minor BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    CONSTRAINT uk_procurement_item_sequence UNIQUE (procurement_id, sequence_no),
    CONSTRAINT fk_procurement_item_detail FOREIGN KEY (procurement_id) REFERENCES procurement_details (application_id) ON DELETE CASCADE,
    CONSTRAINT ck_procurement_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_procurement_item_price CHECK (unit_price_minor >= 0),
    CONSTRAINT ck_procurement_item_total CHECK (line_total_minor >= 0),
    CONSTRAINT ck_procurement_item_sequence CHECK (sequence_no > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE procurement_quotes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    procurement_id BIGINT NOT NULL,
    supplier_name VARCHAR(160) NOT NULL,
    quote_total_minor BIGINT NOT NULL,
    quoted_at DATE NOT NULL,
    is_selected BOOLEAN NOT NULL DEFAULT FALSE,
    selection_reason VARCHAR(1000) NULL,
    selected_procurement_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN is_selected THEN procurement_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_quote_id_procurement UNIQUE (id, procurement_id),
    CONSTRAINT uk_procurement_selected_quote UNIQUE (selected_procurement_id),
    INDEX idx_quote_procurement (procurement_id),
    CONSTRAINT fk_quote_procurement FOREIGN KEY (procurement_id) REFERENCES procurement_details (application_id),
    CONSTRAINT ck_quote_total CHECK (quote_total_minor >= 0),
    CONSTRAINT ck_quote_selection_reason CHECK (NOT is_selected OR selection_reason IS NOT NULL)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE travel_details (
    application_id BIGINT PRIMARY KEY,
    purpose TEXT NOT NULL,
    origin VARCHAR(160) NOT NULL,
    destination VARCHAR(160) NOT NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    budget_minor BIGINT NOT NULL,
    duration_days INT NOT NULL,
    CONSTRAINT fk_travel_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT ck_travel_period CHECK (end_at >= start_at),
    CONSTRAINT ck_travel_budget CHECK (budget_minor >= 0),
    CONSTRAINT ck_travel_duration CHECK (duration_days > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reimbursement_details (
    application_id BIGINT PRIMARY KEY,
    source_application_id BIGINT NULL,
    explanation TEXT NOT NULL,
    budget_policy_snapshot VARCHAR(16) NOT NULL,
    over_budget_reason VARCHAR(1000) NULL,
    CONSTRAINT uk_reimbursement_source_pair UNIQUE (application_id, source_application_id),
    CONSTRAINT fk_reimbursement_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_reimbursement_source FOREIGN KEY (source_application_id) REFERENCES applications (id),
    CONSTRAINT ck_reimbursement_policy CHECK (budget_policy_snapshot IN ('WARN_ONLY', 'HARD_LIMIT')),
    CONSTRAINT ck_reimbursement_not_self_source CHECK (source_application_id IS NULL OR source_application_id <> application_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE expense_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reimbursement_id BIGINT NOT NULL,
    expense_type VARCHAR(64) NOT NULL,
    incurred_at DATE NOT NULL,
    amount_minor BIGINT NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    no_receipt_exception BOOLEAN NOT NULL DEFAULT FALSE,
    exception_reason VARCHAR(1000) NULL,
    sequence_no INT NOT NULL,
    CONSTRAINT uk_expense_id_reimbursement UNIQUE (id, reimbursement_id),
    CONSTRAINT uk_expense_sequence UNIQUE (reimbursement_id, sequence_no),
    CONSTRAINT fk_expense_reimbursement FOREIGN KEY (reimbursement_id) REFERENCES reimbursement_details (application_id) ON DELETE CASCADE,
    CONSTRAINT ck_expense_amount CHECK (amount_minor > 0),
    CONSTRAINT ck_expense_receipt_exception CHECK (
        (no_receipt_exception AND exception_reason IS NOT NULL)
        OR (NOT no_receipt_exception AND exception_reason IS NULL)
    ),
    CONSTRAINT ck_expense_sequence CHECK (sequence_no > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploader_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_attachment_storage_key UNIQUE (storage_key),
    INDEX idx_attachment_uploader_time (uploader_id, created_at),
    INDEX idx_attachment_sha256 (sha256),
    CONSTRAINT fk_attachment_uploader FOREIGN KEY (uploader_id) REFERENCES users (id),
    CONSTRAINT ck_attachment_size CHECK (size_bytes > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE item_receipts (
    expense_item_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    reimbursement_id BIGINT NOT NULL,
    PRIMARY KEY (expense_item_id, attachment_id),
    CONSTRAINT uk_receipt_reimbursement_attachment UNIQUE (reimbursement_id, attachment_id),
    CONSTRAINT fk_receipt_expense_reimbursement FOREIGN KEY (expense_item_id, reimbursement_id)
        REFERENCES expense_items (id, reimbursement_id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_attachment FOREIGN KEY (attachment_id) REFERENCES attachments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE application_field_values (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    template_version_id BIGINT NOT NULL,
    template_field_id BIGINT NOT NULL,
    value_text TEXT NULL,
    CONSTRAINT uk_application_field UNIQUE (application_id, template_field_id),
    CONSTRAINT fk_value_application_template FOREIGN KEY (application_id, template_version_id)
        REFERENCES applications (id, template_version_id) ON DELETE CASCADE,
    CONSTRAINT fk_value_field_template FOREIGN KEY (template_field_id, template_version_id)
        REFERENCES template_fields (id, template_version_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workflow_instances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    template_version_id BIGINT NULL,
    fixed_workflow_version_key VARCHAR(64) NULL,
    current_node_id BIGINT NULL,
    status VARCHAR(24) NOT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_instance_application UNIQUE (application_id),
    CONSTRAINT uk_instance_id_pair UNIQUE (id, application_id),
    CONSTRAINT fk_instance_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_instance_template_version FOREIGN KEY (template_version_id) REFERENCES workflow_template_versions (id),
    CONSTRAINT ck_instance_version_ref CHECK (
        (template_version_id IS NULL) <> (fixed_workflow_version_key IS NULL)
    ),
    CONSTRAINT ck_instance_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_instance_row_version CHECK (row_version >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workflow_instance_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instance_id BIGINT NOT NULL,
    sequence_no INT NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    approval_mode VARCHAR(16) NOT NULL,
    condition_result_snapshot BOOLEAN NOT NULL,
    rule_snapshot JSON NOT NULL,
    status VARCHAR(24) NOT NULL,
    acted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_instance_sequence UNIQUE (instance_id, sequence_no),
    CONSTRAINT uk_node_id_instance UNIQUE (id, instance_id),
    INDEX idx_node_instance_status (instance_id, status, sequence_no),
    CONSTRAINT fk_node_instance FOREIGN KEY (instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT ck_instance_node_sequence CHECK (sequence_no > 0),
    CONSTRAINT ck_instance_node_mode CHECK (approval_mode IN ('SINGLE', 'ALL', 'ANY_ONE')),
    CONSTRAINT ck_instance_node_status CHECK (status IN ('WAITING', 'PENDING', 'APPROVED', 'REJECTED', 'SKIPPED', 'CANCELLED')),
    CONSTRAINT ck_instance_node_condition_status CHECK (
        condition_result_snapshot OR status IN ('SKIPPED', 'CANCELLED')
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE workflow_node_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    opinion TEXT NULL,
    acted_at DATETIME(3) NULL,
    transferred_from_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    active_assignee_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status = 'PENDING' THEN assignee_id ELSE NULL END
    ) STORED,
    CONSTRAINT uk_node_active_assignee UNIQUE (node_id, active_assignee_id),
    INDEX idx_assignee_status_time (assignee_id, status, created_at),
    CONSTRAINT fk_task_node FOREIGN KEY (node_id) REFERENCES workflow_instance_nodes (id),
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users (id),
    CONSTRAINT fk_task_transfer_source FOREIGN KEY (transferred_from_id) REFERENCES users (id),
    CONSTRAINT ck_task_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_task_action_time CHECK (
        (status = 'PENDING' AND acted_at IS NULL)
        OR (status <> 'PENDING' AND acted_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE idempotency_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    resource_id BIGINT NOT NULL,
    draft_version INT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    saved_result JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    CONSTRAINT uk_actor_operation_idempotency UNIQUE (actor_id, operation, idempotency_key),
    INDEX idx_idempotency_created (created_at),
    CONSTRAINT fk_idempotency_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_idempotency_draft_version CHECK (draft_version IS NULL OR draft_version >= 0),
    CONSTRAINT ck_idempotency_completion CHECK (
        (status = 'PROCESSING' AND completed_at IS NULL)
        OR (status IN ('SUCCEEDED', 'FAILED') AND completed_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE approval_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    node_id BIGINT NULL,
    task_id BIGINT NULL,
    idempotency_record_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    opinion TEXT NULL,
    from_assignee_id BIGINT NULL,
    to_assignee_id BIGINT NULL,
    result_status VARCHAR(24) NOT NULL,
    request_key VARCHAR(80) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_approval_log_idempotency UNIQUE (idempotency_record_id),
    INDEX idx_approval_application_time (application_id, created_at),
    CONSTRAINT fk_log_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_log_node FOREIGN KEY (node_id) REFERENCES workflow_instance_nodes (id),
    CONSTRAINT fk_log_task FOREIGN KEY (task_id) REFERENCES workflow_node_tasks (id),
    CONSTRAINT fk_log_idempotency FOREIGN KEY (idempotency_record_id) REFERENCES idempotency_records (id),
    CONSTRAINT fk_log_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT fk_log_from_assignee FOREIGN KEY (from_assignee_id) REFERENCES users (id),
    CONSTRAINT fk_log_to_assignee FOREIGN KEY (to_assignee_id) REFERENCES users (id),
    CONSTRAINT ck_approval_action CHECK (action IN ('SUBMIT', 'APPROVE', 'REJECT', 'WITHDRAW', 'TRANSFER')),
    CONSTRAINT ck_approval_result_status CHECK (result_status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_approval_transfer_fields CHECK (
        (action = 'TRANSFER' AND from_assignee_id IS NOT NULL AND to_assignee_id IS NOT NULL AND opinion IS NOT NULL)
        OR (action <> 'TRANSFER' AND from_assignee_id IS NULL AND to_assignee_id IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    event_key VARCHAR(120) NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    application_id BIGINT NOT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_recipient_event UNIQUE (recipient_id, event_key),
    INDEX idx_recipient_unread (recipient_id, read_at, created_at),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notification_application FOREIGN KEY (application_id) REFERENCES applications (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_key VARCHAR(120) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    delivered_at DATETIME(3) NULL,
    last_error VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_outbox_event_key UNIQUE (event_key),
    INDEX idx_outbox_delivery (status, next_attempt_at, id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    CONSTRAINT ck_outbox_attempts CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_delivered CHECK (
        (status = 'DELIVERED' AND delivered_at IS NOT NULL)
        OR (status <> 'DELIVERED' AND delivered_at IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE attachment_binds (
    application_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    quote_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (application_id, attachment_id),
    CONSTRAINT fk_attachment_bind_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_bind_attachment FOREIGN KEY (attachment_id) REFERENCES attachments (id),
    CONSTRAINT fk_attachment_bind_quote_application FOREIGN KEY (quote_id, application_id)
        REFERENCES procurement_quotes (id, procurement_id),
    CONSTRAINT ck_attachment_bind_purpose CHECK (purpose IN ('APPLICATION', 'QUOTE', 'OTHER')),
    CONSTRAINT ck_attachment_bind_quote CHECK (
        (purpose = 'QUOTE' AND quote_id IS NOT NULL)
        OR (purpose <> 'QUOTE' AND quote_id IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE push_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    endpoint_hash CHAR(64) NOT NULL,
    encrypted_endpoint TEXT NOT NULL,
    encrypted_key_material TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    revoked_at DATETIME(3) NULL,
    CONSTRAINT uk_push_endpoint_hash UNIQUE (endpoint_hash),
    INDEX idx_push_user_active (user_id, revoked_at),
    CONSTRAINT fk_push_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE receipt_extractions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attachment_id BIGINT NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    extracted_fields JSON NOT NULL,
    confidence DECIMAL(5, 4) NOT NULL,
    review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_receipt_extraction_attachment UNIQUE (attachment_id),
    INDEX idx_receipt_content_hash (content_sha256),
    CONSTRAINT fk_extraction_attachment FOREIGN KEY (attachment_id) REFERENCES attachments (id),
    CONSTRAINT fk_extraction_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_extraction_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_extraction_review_status CHECK (review_status IN ('PENDING_REVIEW', 'CONFIRMED', 'IGNORED')),
    CONSTRAINT ck_extraction_review CHECK (
        (review_status = 'PENDING_REVIEW' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR (review_status IN ('CONFIRMED', 'IGNORED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NOT NULL,
    before_summary JSON NULL,
    after_summary JSON NULL,
    reason VARCHAR(1000) NULL,
    idempotency_record_id BIGINT NULL,
    request_key VARCHAR(80) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_admin_audit_target (target_type, target_id, created_at),
    INDEX idx_admin_audit_actor_time (actor_id, created_at),
    CONSTRAINT fk_admin_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT fk_admin_audit_idempotency FOREIGN KEY (idempotency_record_id) REFERENCES idempotency_records (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Circular and same-parent ownership constraints are added after both sides exist.
ALTER TABLE procurement_details
    ADD CONSTRAINT fk_chosen_quote_same_procurement
        FOREIGN KEY (chosen_quote_id, application_id)
        REFERENCES procurement_quotes (id, procurement_id);

ALTER TABLE workflow_instances
    ADD CONSTRAINT fk_current_node_same_instance
        FOREIGN KEY (current_node_id, id)
        REFERENCES workflow_instance_nodes (id, instance_id);

-- Synthetic baseline data. Password hashes use BCrypt cost 12; no plaintext password is stored.
INSERT INTO departments (id, department_code, name, status)
VALUES (1, 'R_AND_D', '研发部', 'ACTIVE'),
       (2, 'MARKETING', '市场部', 'ACTIVE'),
       (3, 'FINANCE', '财务部', 'ACTIVE');

INSERT INTO roles (id, role_key, display_name, status)
VALUES (1, 'EMPLOYEE', '普通员工', 'ACTIVE'),
       (2, 'DEPARTMENT_MANAGER', '部门负责人', 'ACTIVE'),
       (3, 'FINANCE_APPROVER', '财务审批人', 'ACTIVE'),
       (4, 'ENTERPRISE_APPROVER', '企业负责人', 'ACTIVE'),
       (5, 'SYSTEM_ADMIN', '系统管理员', 'ACTIVE');

INSERT INTO users (id, department_id, username, full_name, email, password_hash, status)
VALUES
    (1, 1, 'admin.demo', '系统管理员（演示）', 'admin.demo@example.test',
     '$2a$12$rx8roAi/WDCTSkqS/mg3xuKpNS9As9x.WkFjFeByUFdqGUZad.6om', 'ACTIVE'),
    (2, 1, 'employee.rd', '研发部员工（演示）', 'employee.rd@example.test',
     '$2a$12$psngUVtlUxoF3rS8QjjPcOeP1Rr1TWuhJgg30.o7A3s8uCpCnI1B2', 'ACTIVE'),
    (3, 1, 'manager.rd', '研发部负责人（演示）', 'manager.rd@example.test',
     '$2a$12$Zeg4jmfqjbq96kfhVqSuu.iwBiLp6iRdJ.kW76eQcl08PdXGNcace', 'ACTIVE'),
    (4, 2, 'employee.marketing', '市场部员工（演示）', 'employee.marketing@example.test',
     '$2a$12$3x.p0EIjZMUv2Do/nSVeAOeIn.byvUPaCJc84S5d/Knk5.Jl.mRl6', 'ACTIVE'),
    (5, 2, 'manager.marketing', '市场部负责人（演示）', 'manager.marketing@example.test',
     '$2a$12$mekI0dFBqwFnaPEL1DCRmuUERFxDxH.mYN.oxvQvcmP/YkKfGwaeK', 'ACTIVE'),
    (6, 3, 'finance.approver', '财务审批人（演示）', 'finance.approver@example.test',
     '$2a$12$LLZN4.e8VL7J65pPgcCDZ.uqwinIUdEFTlULjyRmqE7SD2krnyiz6', 'ACTIVE'),
    (7, 1, 'enterprise.approver', '企业负责人（演示）', 'enterprise.approver@example.test',
     '$2a$12$rx8roAi/WDCTSkqS/mg3xuKpNS9As9x.WkFjFeByUFdqGUZad.6om', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id, granted_by)
VALUES (1, 1, 1), (1, 5, 1),
       (2, 1, 1),
       (3, 1, 1), (3, 2, 1),
       (4, 1, 1),
       (5, 1, 1), (5, 2, 1),
       (6, 1, 1), (6, 3, 1),
       (7, 1, 1), (7, 4, 1);

INSERT INTO approval_positions (id, department_id, duty_key, effective_from, status)
VALUES (1, 1, 'DEPARTMENT_MANAGER', '2026-01-01 00:00:00.000', 'ACTIVE'),
       (2, 2, 'DEPARTMENT_MANAGER', '2026-01-01 00:00:00.000', 'ACTIVE'),
       (3, 3, 'FINANCE_APPROVER', '2026-01-01 00:00:00.000', 'ACTIVE'),
       (4, 1, 'ENTERPRISE_APPROVER', '2026-01-01 00:00:00.000', 'ACTIVE');

INSERT INTO position_assignments (position_id, user_id, assignment_type, assigned_at)
VALUES (1, 3, 'PRIMARY', '2026-01-01 00:00:00.000'),
       (2, 5, 'PRIMARY', '2026-01-01 00:00:00.000'),
       (3, 6, 'PRIMARY', '2026-01-01 00:00:00.000'),
       (4, 7, 'PRIMARY', '2026-01-01 00:00:00.000');
