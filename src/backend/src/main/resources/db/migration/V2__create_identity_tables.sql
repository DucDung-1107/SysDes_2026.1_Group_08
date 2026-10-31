CREATE TABLE medora.facility (
    id UUID PRIMARY KEY ,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE  medora.user_account (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    auth_version BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT ck_user_account_username
        CHECK (username ~ '^[a-z0-9][a-z0-9._-]{2,63}$'),
    CONSTRAINT ck_user_account_status
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_user_account_auth_version
        CHECK (auth_version >= 0)
);

CREATE TABLE medora.app_role (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE medora.permission (
    code VARCHAR(80) PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE medora.role_permission(
    role_code VARCHAR(40) NOT NULL,
    permission_code VARCHAR(80) NOT NULL,
    PRIMARY KEY(role_code, permission_code),
        CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_code)
        REFERENCES medora.app_role (code),

    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_code)
        REFERENCES medora.permission (code)
);
CREATE TABLE medora.user_role_assignment(
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL,
    role_code VARCHAR(40) NOT NULL,
    facility_id UUID NOT NULL,

    CONSTRAINT uq_user_role_assignment
        UNIQUE (user_account_id, facility_id, role_code),

    CONSTRAINT fk_assignment_account
        FOREIGN KEY (user_account_id)
        REFERENCES medora.user_account (id),

    CONSTRAINT fk_assignment_role
        FOREIGN KEY (role_code)
        REFERENCES medora.app_role (code),

    CONSTRAINT fk_assignment_facility
        FOREIGN KEY (facility_id)
        REFERENCES medora.facility (id)
);

INSERT INTO medora.app_role (code, name)
VALUES
    ('SYSTEM_ADMINISTRATOR', 'System Administrator'),
    ('RECEPTIONIST', 'Receptionist'),
    ('NURSE', 'Nurse'),
    ('DOCTOR', 'Doctor');

INSERT INTO medora.permission (code, description)
VALUES
    ('account.read', 'Read accounts within permitted facility scope'),
    ('account.create', 'Create staff accounts'),
    ('account.update', 'Update account details and status'),
    ('account.assign_role', 'Assign roles within permitted facility scope'),
    ('patient.admin.read', 'Read patient administrative data'),
    ('patient.admin.create', 'Register a patient'),
    ('patient.admin.update', 'Update patient administrative data');

INSERT INTO medora.role_permission (role_code, permission_code)
VALUES
    ('SYSTEM_ADMINISTRATOR', 'account.read'),
    ('SYSTEM_ADMINISTRATOR', 'account.create'),
    ('SYSTEM_ADMINISTRATOR', 'account.update'),
    ('SYSTEM_ADMINISTRATOR', 'account.assign_role'),

    ('RECEPTIONIST', 'patient.admin.read'),
    ('RECEPTIONIST', 'patient.admin.create'),
    ('RECEPTIONIST', 'patient.admin.update'),

    ('NURSE', 'patient.admin.read'),
    ('DOCTOR', 'patient.admin.read');
