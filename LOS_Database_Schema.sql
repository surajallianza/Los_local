-- =====================================================================
-- LOS DATABASE SCHEMA — PHASE 1 (LOGIN ONLY)
-- =====================================================================
-- This file has TWO parts:
--   PART A -> run once, inside los_master_db      (central registry)
--   PART B -> run once, inside EACH tenant database (e.g. los_hdfc01_db)
--
-- HOW TO USE:
--   1. Create and connect to los_master_db, run PART A only.
--   2. For every new NBFC/Bank: create its own database
--      (e.g. los_hdfc01_db), connect to it, and run PART B only.
--
-- The \connect lines below are psql-only markers so you remember which
-- database each part belongs to — remove/ignore them if running through
-- a GUI tool (pgAdmin/DBeaver) where you select the database from the UI.
-- =====================================================================


-- =====================================================================
-- PART A: MASTER DATABASE  (run inside los_master_db)
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS identity;

-- ---------------------------------------------------------------------
-- organization.organizations
-- Registry of every NBFC/Bank + which physical database holds its data
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organization.organizations (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID UNIQUE DEFAULT gen_random_uuid(),
    bank_code                   VARCHAR(50)  NOT NULL UNIQUE,
    bank_name                   VARCHAR(150) NOT NULL,
    legal_name                  VARCHAR(200),
    short_name                  VARCHAR(50),
    bank_type                   VARCHAR(50)  NOT NULL,
    license_number              VARCHAR(100),
    pan                         VARCHAR(20),
    gst_no                      VARCHAR(15),
    website                     VARCHAR(255),
    logo                        TEXT,
    regulatory_authority_id     UUID,
    regulatory_status           VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    country                     VARCHAR(100) NOT NULL DEFAULT 'India',
    status                      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    contact_email               VARCHAR(150),
    contact_phone               VARCHAR(20),
    db_name                     VARCHAR(100) NOT NULL UNIQUE,
    db_host                     VARCHAR(150) NOT NULL DEFAULT 'localhost',
    db_port                     INT          NOT NULL DEFAULT 5432,
    created_by                  BIGINT,
    created_at                  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT now(),

    -- Regulatory details
    direct_clearing_member       BOOLEAN,
    direct_member_iftas          BOOLEAN,
    micr_city_code              VARCHAR(3),
    micr_bank_code              VARCHAR(3),
    micr_branch_code            VARCHAR(3),
    ifsc_code                   VARCHAR(11),
    number_of_branches          INTEGER,
    sponsor_bank_for_clearing   VARCHAR(150),
    sponsor_bank_for_iftas      VARCHAR(150),

    -- Address details
    address_type                VARCHAR(50),
    unit_gala_name_number        VARCHAR(200),
    street_road                 VARCHAR(200),
    landmark                    VARCHAR(150),
    city                        VARCHAR(100),
    state                       VARCHAR(100),
    pincode                     VARCHAR(6)
);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS bank_code VARCHAR(50);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(150);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS legal_name VARCHAR(200);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS short_name VARCHAR(50);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS bank_type VARCHAR(50);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS license_number VARCHAR(100);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS pan VARCHAR(20);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS gst_no VARCHAR(15);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS website VARCHAR(255);

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS logo TEXT;

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS regulatory_authority_id UUID;

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS regulatory_status VARCHAR(50) DEFAULT 'ACTIVE';

ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'India';

-- Regulatory details
ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS direct_clearing_member BOOLEAN,
    ADD COLUMN IF NOT EXISTS direct_member_iftas BOOLEAN,
    ADD COLUMN IF NOT EXISTS micr_city_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS micr_bank_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS micr_branch_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(11),
    ADD COLUMN IF NOT EXISTS number_of_branches INTEGER,
    ADD COLUMN IF NOT EXISTS sponsor_bank_for_clearing VARCHAR(150),
    ADD COLUMN IF NOT EXISTS sponsor_bank_for_iftas VARCHAR(150);

-- Address details
ALTER TABLE organization.organizations
    ADD COLUMN IF NOT EXISTS address_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS unit_gala_name_number VARCHAR(200),
    ADD COLUMN IF NOT EXISTS street_road VARCHAR(200),
    ADD COLUMN IF NOT EXISTS landmark VARCHAR(150),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS pincode VARCHAR(6);


-- ---------------------------------------------------------------------
-- identity.roles  (Internal panel only)
-- ---------------------------------------------------------------------
CREATE TABLE identity.roles (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL UNIQUE,
    panel         VARCHAR(30) NOT NULL CHECK (panel IN ('INTERNAL')),
    description   VARCHAR(255),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO identity.roles (name, panel, description) VALUES
    ('INTERNAL_ADMIN', 'INTERNAL', 'Platform team; manages tenants and features');

-- ---------------------------------------------------------------------
-- identity.internal_users  (your own team)
-- ---------------------------------------------------------------------
CREATE TABLE identity.internal_users (
    id                      BIGSERIAL PRIMARY KEY,
    user_code               VARCHAR(30) UNIQUE,
    role_id                 INT NOT NULL REFERENCES identity.roles(id),
    first_name              VARCHAR(80) NOT NULL,
    middle_name             VARCHAR(80),
    last_name               VARCHAR(80) NOT NULL,
    email                   VARCHAR(150) UNIQUE,
    phone                   VARCHAR(20) UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT true,
    last_login_at           TIMESTAMP,
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    created_by              BIGINT REFERENCES identity.internal_users(id),
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- identity.login_directory
-- Routing table: given an identifier (user_code/email/phone), find
-- which organization (and therefore which tenant database) it belongs to.
-- ---------------------------------------------------------------------
CREATE TABLE identity.login_directory (
    id                BIGSERIAL PRIMARY KEY,
    user_code         VARCHAR(30) UNIQUE,
    email             VARCHAR(150) UNIQUE,
    phone             VARCHAR(20) UNIQUE,
    organization_id   BIGINT NOT NULL REFERENCES organization.organizations(id),
    user_type         VARCHAR(20) NOT NULL CHECK (user_type IN ('STAFF','CUSTOMER','INTERNAL')),
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_directory_org ON identity.login_directory(organization_id);

-- ---------------------------------------------------------------------
-- identity.lookup_types (Table 51001) - Master Lookup Types
-- ---------------------------------------------------------------------
CREATE TABLE identity.lookup_types (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    description   VARCHAR(255) NOT NULL,
    is_fixed      BOOLEAN NOT NULL DEFAULT false,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_by    BIGINT,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    modified_by   BIGINT,
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- identity.lookup_sub_types (Table 51101) - Master Lookup Sub Types
-- ---------------------------------------------------------------------
CREATE TABLE identity.lookup_sub_types (
    id                    BIGSERIAL PRIMARY KEY,
    lookup_type_code      VARCHAR(50) NOT NULL,
    type_description      VARCHAR(255),
    sub_type_code         VARCHAR(50) NOT NULL,
    sub_type_description  VARCHAR(255) NOT NULL,
    is_fixed              BOOLEAN NOT NULL DEFAULT false,
    is_active             BOOLEAN NOT NULL DEFAULT true,
    display_order         INT NOT NULL DEFAULT 0,
    created_by            BIGINT,
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    modified_by           BIGINT,
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_master_lookup_sub_type UNIQUE (lookup_type_code, sub_type_code)
);

CREATE INDEX idx_master_lst_type_code ON identity.lookup_sub_types(lookup_type_code);

-- ---------------------------------------------------------------------
-- identity.master_lookup_type_permissions - Dynamic Master Lookup Permissions
-- ---------------------------------------------------------------------
CREATE TABLE identity.master_lookup_type_permissions (
    id                BIGSERIAL PRIMARY KEY,
    lookup_type_code  VARCHAR(50) NOT NULL,
    permission_code   VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_master_lt_permission UNIQUE (lookup_type_code, permission_code)
);

CREATE INDEX idx_master_lt_perm_code ON identity.master_lookup_type_permissions(lookup_type_code);

-- =====================================================================
-- END OF PART A
-- =====================================================================


-- =====================================================================
-- PART B: TENANT DATABASE TEMPLATE
-- Run this exact block inside EVERY new NBFC/Bank database
-- e.g. los_hdfc01_db, los_bajaj02_db, etc.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS customer;

-- ---------------------------------------------------------------------
-- organization.branches
-- ---------------------------------------------------------------------
CREATE TABLE organization.branches (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    code            VARCHAR(20) NOT NULL UNIQUE,
    address         VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- identity.roles  (local copy — 5 roles for this tenant)
-- ---------------------------------------------------------------------
CREATE TABLE identity.roles (
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(50) NOT NULL UNIQUE,
    panel         VARCHAR(30) NOT NULL CHECK (panel IN ('BANK_NBFC','CUSTOMER')),
    description   VARCHAR(255),
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO identity.roles (name, panel, description) VALUES
    ('SUPER_ADMIN', 'BANK_NBFC', 'Full control within this NBFC/Bank'),
    ('CHECKER',     'BANK_NBFC', 'Reviews and approves Maker actions'),
    ('MAKER',       'BANK_NBFC', 'Creates/initiates records for Checker approval'),
    ('VIEWER',      'BANK_NBFC', 'Read-only access'),
    ('CUSTOMER',    'CUSTOMER',  'Loan applicant');

-- ---------------------------------------------------------------------
-- identity.users  (Bank/NBFC staff — branch-linked, split name)
-- ---------------------------------------------------------------------
CREATE TABLE identity.users (
    id                      BIGSERIAL PRIMARY KEY,
    user_code               VARCHAR(30) UNIQUE,
    branch_id               BIGINT REFERENCES organization.branches(id),
    role_id                 INT NOT NULL REFERENCES identity.roles(id),
    first_name              VARCHAR(80) NOT NULL,
    middle_name             VARCHAR(80),
    last_name               VARCHAR(80) NOT NULL,
    email                   VARCHAR(150) UNIQUE,
    phone                   VARCHAR(20) UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT true,
    last_login_at           TIMESTAMP,
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    created_by              BIGINT REFERENCES identity.users(id),
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_branch_id ON identity.users(branch_id);
CREATE INDEX idx_users_role_id ON identity.users(role_id);

-- ---------------------------------------------------------------------
-- customer.customers  (branch-linked, split name)
-- ---------------------------------------------------------------------
CREATE TABLE customer.customers (
    id               BIGSERIAL PRIMARY KEY,
    customer_code    VARCHAR(30) UNIQUE,
    branch_id        BIGINT REFERENCES organization.branches(id),
    first_name       VARCHAR(80) NOT NULL,
    middle_name      VARCHAR(80),
    last_name        VARCHAR(80) NOT NULL,
    email            VARCHAR(150) UNIQUE,
    phone            VARCHAR(20) UNIQUE,
    password_hash    VARCHAR(255),
    is_active        BOOLEAN NOT NULL DEFAULT true,
    last_login_at    TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_customers_branch_id ON customer.customers(branch_id);

-- ---------------------------------------------------------------------
-- customer.leads (Lead Management Table)
-- Encapsulates all 4-section multi-step form fields matching Lead entity
-- ---------------------------------------------------------------------
CREATE TABLE customer.leads (
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

CREATE INDEX idx_leads_mobile ON customer.leads(mobile_number);
CREATE INDEX idx_leads_pan ON customer.leads(pan_number);
CREATE INDEX idx_leads_aadhaar ON customer.leads(aadhaar_number);
CREATE INDEX idx_leads_status ON customer.leads(lead_status);
CREATE INDEX idx_leads_assigned_emp ON customer.leads(assigned_employee_id);
CREATE INDEX idx_leads_sourcing_emp ON customer.leads(sourcing_employee_id);

-- ---------------------------------------------------------------------
-- identity.lookup_types (Table 51001) - Bank Lookup Types
-- ---------------------------------------------------------------------
CREATE TABLE identity.lookup_types (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    description   VARCHAR(255) NOT NULL,
    is_fixed      BOOLEAN NOT NULL DEFAULT false,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_by    BIGINT,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    modified_by   BIGINT,
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- identity.lookup_sub_types (Table 51101) - Bank Lookup Sub Types
-- ---------------------------------------------------------------------
CREATE TABLE identity.lookup_sub_types (
    id                    BIGSERIAL PRIMARY KEY,
    lookup_type_code      VARCHAR(50) NOT NULL,
    type_description      VARCHAR(255),
    sub_type_code         VARCHAR(50) NOT NULL,
    sub_type_description  VARCHAR(255) NOT NULL,
    is_fixed              BOOLEAN NOT NULL DEFAULT false,
    is_active             BOOLEAN NOT NULL DEFAULT true,
    display_order         INT NOT NULL DEFAULT 0,
    created_by            BIGINT,
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    modified_by           BIGINT,
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_lookup_sub_type UNIQUE (lookup_type_code, sub_type_code)
);

CREATE INDEX idx_bank_lst_type_code ON identity.lookup_sub_types(lookup_type_code);

-- ---------------------------------------------------------------------
-- identity.bank_lookup_type_permissions - Dynamic Bank Lookup Permissions
-- ---------------------------------------------------------------------
CREATE TABLE identity.bank_lookup_type_permissions (
    id                BIGSERIAL PRIMARY KEY,
    lookup_type_code  VARCHAR(50) NOT NULL,
    permission_code   VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_bank_lt_permission UNIQUE (lookup_type_code, permission_code)
);

CREATE INDEX idx_bank_lt_perm_code ON identity.bank_lookup_type_permissions(lookup_type_code);

CREATE INDEX idx_bank_lst_type_code ON identity.lookup_sub_types(lookup_type_code);

-- =====================================================================
-- END OF PART B
-- =====================================================================

