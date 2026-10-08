# Loan Origination System (LOS) - Multi-Tenant Backend

An enterprise-grade, multi-tenant Spring Boot backend for Loan Origination System (LOS) supporting multi-bank dynamic routing, RBAC, JWT authentication, and hardened security controls.

---

## 🔒 Hardened Security Features

- **OWASP HTTP Security Headers**: HSTS, Content-Security-Policy (CSP), X-Frame-Options (DENY), X-Content-Type-Options (nosniff), X-XSS-Protection, Referrer-Policy, Permissions-Policy.
- **Strict CORS Control**: Environment-configurable allowed origins (`app.cors.allowed-origins`).
- **Anti-Brute Force Rate Limiting**: In-memory sliding-window throttling for auth endpoints (10 req/min) and API endpoints (120 req/min).
- **XSS Protection & Request Sanitization**: Automatic input filter stripping malicious scripts and tags.
- **Account Lockout Policy**: Automatic account lock after 5 consecutive failed login attempts.
- **JWT Token Revocation & Logout**: Session revocation endpoint (`/api/v1/auth/logout`).
- **Password Policy Enforcement**: Mandatory password complexity validation (min 8 chars, upper, lower, digit, special char).
- **Error Information Protection**: Sanitized 500 internal server error payloads preventing sensitive data/SQL leaks.

---

## 📬 Postman API Testing Guide & Collection

A ready-to-use Postman Collection and comprehensive guide are included:

- 📄 **Postman Guide**: [docs/POSTMAN_API_TESTING_GUIDE.md](docs/POSTMAN_API_TESTING_GUIDE.md)
- 📦 **Postman Collection**: [docs/LOS_Backend_Postman_Collection.json](docs/LOS_Backend_Postman_Collection.json)

---

## 👥 Lead Management Module

The backend includes a comprehensive 4-step Lead Origination module:
1. **Personal Details**: Customer Name, DOB, Age, Customer Type, PAN Card, PAN Validate, Aadhaar Card, Aadhaar Validate, Residential Status, Gender, Marital Status, Passport No., **Passport Expiry Date** (`passportExpiryDate`), De-Duplicate Check, Blacklist Check, Last Name, Email Address, Pin Code, Number of Dependents. *(Note: `mobileNumber` and OTP-related fields are completely removed from the Lead form/model).*
2. **Loan Details**: Loan Product Type, Loan Amount, Purpose of Loan, Tenure, Number of Instalments, EMI, **Interest Rate** (`interestRate`), **Total Interest** (`totalInterest`), Security Amount, Down Payment / Collateral. *(Note: `propertyValue` is replaced by `totalInterest`).*
3. **Income Profile**: Employment Type, Annual Income, Designation, Employer Name, Location, State, Take Home Pay, Deductions / EMIs Payable, Bank Name, Primary Bank Account, Account Statement Consent, CIBIL Liability Check, DTI, LTV, DSCR, Net Disposable Income.
4. **Referral Details**: Sourcing Channel, Referral Date, LSP Partner Code, Agent Partner Name, Sourcing Employee ID, Sourcing Employee Name.

### Lead Management REST Endpoints:
- `POST /api/v1/leads` - Create a new Lead across all 4 steps
- `GET /api/v1/leads/{leadId}` - Retrieve Lead details by ID
- `GET /api/v1/leads` - Retrieve all Leads
- `PUT /api/v1/leads/{leadId}` - Update editable Lead fields
- `PUT /api/v1/leads/{leadId}/assign` - Assign Lead to an active Maker user
- `GET /api/v1/leads/sample-csv` - Download RFC-4180 compliant CSV template
- `GET /api/v1/leads/export` - Export all leads to CSV
- `POST /api/v1/leads/import` - Bulk import leads from CSV

---

## 🚀 Getting Started

### Requirements
- **Java**: 21+
- **Database**: PostgreSQL (or embedded H2 for local development)
- **Build Tool**: Apache Maven

### Running Locally
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Access Swagger UI documentation at: `http://localhost:8080/swagger-ui.html`