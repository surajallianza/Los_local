CREATE SCHEMA IF NOT EXISTS organization;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS customer;

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
