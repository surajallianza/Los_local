-- =======================================================================
-- TENANT DB SCHEMA TEMPLATE
-- Run inside EACH bank/NBFC database (e.g. los_hdfc01_db, los_bajaj02_db)
-- =======================================================================
CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS customer;

-- -----------------------------------------------------------------------
-- organization.branches
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organization.branches (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    address     VARCHAR(255),
    city        VARCHAR(100),
    state       VARCHAR(100),
    pincode     VARCHAR(10),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- identity.roles  (5 tenant roles: ADMIN, CHECKER, MAKER, VIEWER, CUSTOMER)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.roles (
    id            SERIAL       PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL UNIQUE,
    panel         VARCHAR(30)  NOT NULL,
    description   VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

INSERT INTO identity.roles (name, panel, description) VALUES
    ('SUPER_ADMIN', 'BANK_NBFC', 'Bank/NBFC Super Admin — full control within this Bank/NBFC'),
    ('ADMIN', 'BANK_NBFC', 'Bank/NBFC internal admin — configures org and manages users'),
    ('MAKER', 'BANK_NBFC', 'Creates and initiates loan records for Checker approval'),
    ('CHECKER', 'BANK_NBFC', 'Reviews and approves Maker actions'),
    ('VIEWER', 'BANK_NBFC', 'Read-only access'),
    ('CUSTOMER', 'CUSTOMER', 'Loan applicant')
ON CONFLICT (name) DO NOTHING;

-- -----------------------------------------------------------------------
-- identity.permissions  (lookup codes — DB-backed, configurable by ADMIN)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.permissions (
    id          SERIAL       PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,   -- e.g. LOAN_APPLICATION_CREATE
    description VARCHAR(255),
    module      VARCHAR(60),                    -- e.g. LOAN, USER, BRANCH
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- identity.role_permissions  (M2M: role → permissions)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.role_permissions (
    role_id       INT NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    permission_id INT NOT NULL REFERENCES identity.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- -----------------------------------------------------------------------
-- identity.designation_role_mappings (Designation → Role Inheritance)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.designation_role_mappings (
    id            BIGSERIAL    PRIMARY KEY,
    designation   VARCHAR(100) NOT NULL UNIQUE,
    role_id       INT          NOT NULL REFERENCES identity.roles(id) ON DELETE CASCADE,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    modified_by   BIGINT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_drm_designation ON identity.designation_role_mappings(designation);

-- -----------------------------------------------------------------------
-- identity.permission_overrides (Explicit ALLOW / DENY overrides)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.permission_overrides (
    id              BIGSERIAL    PRIMARY KEY,
    target_type     VARCHAR(30)  NOT NULL CHECK (target_type IN ('ROLE', 'DESIGNATION')),
    target_name     VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    effect          VARCHAR(10)  NOT NULL CHECK (effect IN ('ALLOW', 'DENY')),
    reason          VARCHAR(255),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_by      BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    modified_by     BIGINT,
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_permission_override UNIQUE (target_type, target_name, permission_code)
);

CREATE INDEX IF NOT EXISTS idx_po_target ON identity.permission_overrides(target_type, target_name);


-- -----------------------------------------------------------------------
-- identity.users  (Bank/NBFC staff — full schema per senior's design)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.users (
    -- Core identity
    id                        BIGSERIAL    PRIMARY KEY,                        -- Pkid
    emp_no                    VARCHAR(30)  UNIQUE,                             -- EmpNo (auto-generated)
    username                  VARCHAR(80)  UNIQUE NOT NULL,                    -- UserName (login credential)
    password_hash             VARCHAR(255) NOT NULL,                           -- Password (BCrypt)

    -- Security
    two_fa_enabled            BOOLEAN      NOT NULL DEFAULT true,              -- 2fA
    two_fa_otp_hash           VARCHAR(255),                                    -- BCrypt hash of OTP
    two_fa_otp_expiry         TIMESTAMP,
    status                    VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION', -- Status

    -- Personal info
    first_name                VARCHAR(80)  NOT NULL,                           -- Name (split for best practice)
    middle_name               VARCHAR(80),
    last_name                 VARCHAR(80)  NOT NULL,
    dob                       DATE,                                             -- DOB
    email                     VARCHAR(150) UNIQUE,                             -- Mail
    mobile                    VARCHAR(20)  UNIQUE,                             -- Mobile
    gender                    VARCHAR(10),                                      -- MALE/FEMALE/OTHER
    designation               VARCHAR(100),                                     -- Designation

    -- Role & Branch
    role_id                   INT          NOT NULL REFERENCES identity.roles(id), -- Role
    multi_branch_access       BOOLEAN      NOT NULL DEFAULT false,             -- M_Br_access
    login_branch_id           BIGINT       REFERENCES organization.branches(id), -- Login_Branch

    -- Login controls
    login_on_holidays         BOOLEAN      NOT NULL DEFAULT false,             -- Holiday_Login
    login_time                TIME,                                             -- allowed login window start
    logout_time               TIME,                                             -- allowed logout window end
    inactive_session_timeout  INT          NOT NULL DEFAULT 1800,              -- Inactive_session_timeout (seconds)

    -- Login tracking
    no_of_bad_logins          INT          NOT NULL DEFAULT 0,                 -- noofbadlogin
    last_login_date           DATE,                                             -- lastlogindate
    last_login_time           TIME,
    is_active                 BOOLEAN      NOT NULL DEFAULT true,

    created_by                BIGINT,                                           -- CreatedBy
    created_at                TIMESTAMP    NOT NULL DEFAULT now(),              -- CreatedDate
    verified_by               BIGINT,                                           -- VerifiedBy
    verified_date             TIMESTAMP,                                        -- VerifiedDate
    modified_by               BIGINT,                                           -- ModifiedBy
    updated_at                TIMESTAMP    NOT NULL DEFAULT now()               -- ModifiedDate
);

-- Migration safety: ensure newly added columns exist in older DB instances
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS emp_no VARCHAR(30);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS username VARCHAR(80);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION';
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS first_name VARCHAR(80);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS middle_name VARCHAR(80);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS last_name VARCHAR(80);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS dob DATE;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS mobile VARCHAR(20);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS designation VARCHAR(100);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS multi_branch_access BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS login_branch_id BIGINT;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS login_on_holidays BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS login_time TIME;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS logout_time TIME;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS inactive_session_timeout INT NOT NULL DEFAULT 1800;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS no_of_bad_logins INT NOT NULL DEFAULT 0;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS last_login_date DATE;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS last_login_time TIME;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS two_fa_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS two_fa_otp_hash VARCHAR(255);
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS two_fa_otp_expiry TIMESTAMP;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS verified_by BIGINT;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS verified_date TIMESTAMP;
ALTER TABLE identity.users ADD COLUMN IF NOT EXISTS modified_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_users_role_id       ON identity.users(role_id);
CREATE INDEX IF NOT EXISTS idx_users_login_branch  ON identity.users(login_branch_id);
CREATE INDEX IF NOT EXISTS idx_users_status        ON identity.users(status);

-- -----------------------------------------------------------------------
-- identity.user_branches  (for multi_branch_access = true)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.user_branches (
    user_id     BIGINT NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    branch_id   BIGINT NOT NULL REFERENCES organization.branches(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, branch_id)
);

-- -----------------------------------------------------------------------
-- identity.otp_tokens  (2FA OTP — always enforced for tenant users)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.otp_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
    otp_hash    VARCHAR(255) NOT NULL,            -- BCrypt hash of the 6-digit OTP
    used        BOOLEAN      NOT NULL DEFAULT false,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_otp_user_id ON identity.otp_tokens(user_id);

-- -----------------------------------------------------------------------
-- identity.user_password_resets
-- Admin-initiated password reset (maker-checker dual-control)
-- Senior's DB design: Pkid, EmpNo, Password, CreatedBy/Date, VerifiedBy/Date
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.user_password_resets (
    id                BIGSERIAL    PRIMARY KEY,                                -- Pkid
    emp_no            VARCHAR(30)  NOT NULL,                                   -- EmpNo
    new_password_hash VARCHAR(255) NOT NULL,                                   -- Password (BCrypt of new pwd)
    created_by        BIGINT       NOT NULL,                                   -- CreatedBy
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),                     -- CreatedDate
    verified_by       BIGINT,                                                   -- VerifiedBy
    verified_date     TIMESTAMP,                                                -- VerifiedDate
    applied           BOOLEAN      NOT NULL DEFAULT false,
    expires_at        TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_upr_emp_no ON identity.user_password_resets(emp_no);

-- -----------------------------------------------------------------------
-- identity.self_service_reset_tokens
-- Forgot-password OTP tokens (self-service, NOT admin-initiated)
-- Production-grade: token_hash = SHA-256 of raw token (raw never stored)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.self_service_reset_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    emp_no      VARCHAR(30)  NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,            -- SHA-256 of raw token
    used        BOOLEAN      NOT NULL DEFAULT false,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ssrt_emp_no ON identity.self_service_reset_tokens(emp_no);

-- -----------------------------------------------------------------------
-- identity.session_activity  (per-JWT activity for auto-logout)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.session_activity (
    jti           VARCHAR(100) PRIMARY KEY,       -- JWT ID (jti claim)
    user_id       BIGINT       NOT NULL,
    last_seen     TIMESTAMP    NOT NULL,
    timeout_secs  INT          NOT NULL DEFAULT 1800,
    invalidated   BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_session_user_id ON identity.session_activity(user_id);
CREATE INDEX IF NOT EXISTS idx_session_invalidated ON identity.session_activity(invalidated);

-- -----------------------------------------------------------------------
-- identity.refresh_tokens
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.refresh_tokens (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    token               VARCHAR(500) NOT NULL UNIQUE,
    user_type           VARCHAR(20)  NOT NULL,
    organization_code   VARCHAR(30),
    expiry_date         TIMESTAMP    NOT NULL,
    revoked             BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- customer.customers
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer.customers (
    id               BIGSERIAL    PRIMARY KEY,
    customer_code    VARCHAR(30)  UNIQUE,
    branch_id        BIGINT       REFERENCES organization.branches(id),
    first_name       VARCHAR(80)  NOT NULL,
    middle_name      VARCHAR(80),
    last_name        VARCHAR(80)  NOT NULL,
    email            VARCHAR(150) UNIQUE,
    phone            VARCHAR(20)  UNIQUE,
    password_hash    VARCHAR(255),
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customers_branch_id ON customer.customers(branch_id);

-- -----------------------------------------------------------------------
-- customer.leads (Lead Management Table)
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer.leads (
    -- System & Identification Fields
    lead_id                     VARCHAR(50) PRIMARY KEY,
    lead_status                 VARCHAR(50) DEFAULT 'NEW',
    assigned_employee_id        VARCHAR(50),
    assignment_timestamp        TIMESTAMP,
    assigned_by                 VARCHAR(50),

    -- 1. Personal Details
    first_name_business_name    VARCHAR(150),
    dob                         VARCHAR(20),
    age                         INT,
    customer_type               VARCHAR(50) DEFAULT 'Individual',
    mobile_number               VARCHAR(20),
    otp                         VARCHAR(10),
    pan_number                  VARCHAR(20),
    pan_validation_status       VARCHAR(50),
    aadhaar_number              VARCHAR(20),
    aadhaar_validation_status   VARCHAR(50),
    residential_status          VARCHAR(50),
    gender                      VARCHAR(20),
    marital_status              VARCHAR(30),
    passport_number             VARCHAR(30),
    dedupe_status               VARCHAR(50),
    blacklist_status            VARCHAR(50),
    last_name                   VARCHAR(80),
    number_of_dependents        INT,
    email_address               VARCHAR(150),
    pin_code                    VARCHAR(10),

    -- 2. Loan Details
    loan_product_type           VARCHAR(100),
    loan_amount                 NUMERIC(15,2),
    purpose_of_loan             VARCHAR(255),
    tenure                      INT,
    number_of_instalments       INT,
    emi                         NUMERIC(15,2),
    property_value              NUMERIC(15,2),
    security_amount             NUMERIC(15,2),
    down_payment_collateral     VARCHAR(255),

    -- 3. Income Profile
    employment_type             VARCHAR(50),
    annual_income               NUMERIC(15,2),
    designation                 VARCHAR(100),
    employer_business_name      VARCHAR(150),
    location                    VARCHAR(100),
    state                       VARCHAR(100),
    take_home_pay               NUMERIC(15,2),
    deductions_or_emis_payable  NUMERIC(15,2),
    bank_name                   VARCHAR(150),
    primary_bank_account        VARCHAR(50),
    account_statement_consent   BOOLEAN DEFAULT false,
    cibil_liability_check       VARCHAR(50),
    debt_to_income_ratio        NUMERIC(10,2),
    loan_to_value_ratio         NUMERIC(10,2),
    debt_service_coverage_ratio NUMERIC(10,2),
    net_disposable_income       NUMERIC(15,2),

    -- 4. Referral Details
    sourcing_channel            VARCHAR(100),
    referral_date               VARCHAR(30),
    lsp_partner_code            VARCHAR(50),
    agent_partner_name          VARCHAR(150),
    sourcing_employee_id        VARCHAR(50),
    sourcing_employee_name      VARCHAR(150),

    -- Timestamps
    created_at                  TIMESTAMP DEFAULT now(),
    updated_at                  TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_leads_mobile ON customer.leads(mobile_number);
CREATE INDEX IF NOT EXISTS idx_leads_pan ON customer.leads(pan_number);
CREATE INDEX IF NOT EXISTS idx_leads_aadhaar ON customer.leads(aadhaar_number);
CREATE INDEX IF NOT EXISTS idx_leads_status ON customer.leads(lead_status);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_emp ON customer.leads(assigned_employee_id);
CREATE INDEX IF NOT EXISTS idx_leads_sourcing_emp ON customer.leads(sourcing_employee_id);

-- Migration safety: ensure constraints and defaults for customer.leads
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'customer' AND table_name = 'leads'
    ) THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'customer' AND table_name = 'leads' AND column_name = 'created_at') THEN
            ALTER TABLE customer.leads ALTER COLUMN created_at DROP NOT NULL;
            ALTER TABLE customer.leads ALTER COLUMN created_at SET DEFAULT now();
        ELSE
            ALTER TABLE customer.leads ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now();
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'customer' AND table_name = 'leads' AND column_name = 'updated_at') THEN
            ALTER TABLE customer.leads ALTER COLUMN updated_at DROP NOT NULL;
            ALTER TABLE customer.leads ALTER COLUMN updated_at SET DEFAULT now();
        ELSE
            ALTER TABLE customer.leads ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT now();
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'customer' AND table_name = 'leads' AND column_name = 'customer_name') THEN
            ALTER TABLE customer.leads ALTER COLUMN customer_name DROP NOT NULL;
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'customer' AND table_name = 'leads' AND column_name = 'phone') THEN
            ALTER TABLE customer.leads ALTER COLUMN phone DROP NOT NULL;
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'customer' AND table_name = 'leads' AND column_name = 'lead_number') THEN
            ALTER TABLE customer.leads ALTER COLUMN lead_number DROP NOT NULL;
        END IF;
    END IF;
END $$;

-- -----------------------------------------------------------------------
-- identity.lookup_types (Table 51001) - Bank Lookup Types
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.lookup_types (
    id            BIGSERIAL    PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    description   VARCHAR(255) NOT NULL,
    is_fixed      BOOLEAN      NOT NULL DEFAULT false,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    modified_by   BIGINT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------
-- identity.lookup_sub_types (Table 51101) - Bank Lookup Sub Types
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.lookup_sub_types (
    id                    BIGSERIAL    PRIMARY KEY,
    lookup_type_code      VARCHAR(50)  NOT NULL,
    type_description      VARCHAR(255),
    sub_type_code         VARCHAR(50)  NOT NULL,
    sub_type_description  VARCHAR(255) NOT NULL,
    is_fixed              BOOLEAN      NOT NULL DEFAULT false,
    is_active             BOOLEAN      NOT NULL DEFAULT true,
    display_order         INT          NOT NULL DEFAULT 0,
    created_by            BIGINT,
    created_at            TIMESTAMP    NOT NULL DEFAULT now(),
    modified_by           BIGINT,
    updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_lookup_sub_type UNIQUE (lookup_type_code, sub_type_code)
);

CREATE INDEX IF NOT EXISTS idx_bank_lst_type_code ON identity.lookup_sub_types(lookup_type_code);

-- -----------------------------------------------------------------------
-- identity.bank_lookup_type_permissions - Dynamic Bank Lookup Permissions
-- -----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity.bank_lookup_type_permissions (
    id                BIGSERIAL PRIMARY KEY,
    lookup_type_code  VARCHAR(50) NOT NULL,
    permission_code   VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_lt_permission UNIQUE (lookup_type_code, permission_code)
);

CREATE INDEX IF NOT EXISTS idx_bank_lt_perm_code ON identity.bank_lookup_type_permissions(lookup_type_code);

