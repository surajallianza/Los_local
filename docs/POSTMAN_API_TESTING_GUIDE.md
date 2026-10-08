# Loan Origination System (LOS) Backend - Security & Postman API Testing Guide

This comprehensive guide details the security architecture enhancements implemented in the **LOS Multi-Tenant Spring Boot Backend** and provides step-by-step instructions to test all REST APIs using Postman.

---

## 🔒 Security Measures Implemented

The application has been hardened with enterprise-grade security controls aligned with **OWASP Top 10** standards:

### 1. **OWASP HTTP Security Headers**
Configured strict HTTP response security headers in `SecurityConfig.java`:
- **Content-Security-Policy (CSP)**: `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none';`
- **Strict-Transport-Security (HSTS)**: `max-age=31536000; includeSubDomains`
- **X-Frame-Options**: `DENY` (clickjacking defense)
- **X-Content-Type-Options**: `nosniff` (MIME-type sniffing prevention)
- **X-XSS-Protection**: `1; mode=block`
- **Referrer-Policy**: `strict-origin-when-cross-origin`
- **Permissions-Policy**: `camera=(), microphone=(), geolocation=(), payment=()`

### 2. **CORS Hardening**
- Dynamic allowed origins configurable via `app.cors.allowed-origins` (e.g. `http://localhost:3000,http://localhost:5173`).
- Strict credential policy (`allowCredentials(true)` when origins are specified explicitly).
- Restricts allowed methods (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`) and limits exposed response headers.

### 3. **Anti-Brute Force Throttling & Rate Limiting**
- `RateLimitingFilter.java` introduces sliding-window rate limiting per IP address:
  - **Auth Endpoints** (`/api/v1/auth/login`, `/refresh-token`, `/change-password`): **10 requests per minute** max.
  - **General API Endpoints**: **120 requests per minute** max.
  - Returns `HTTP 429 Too Many Requests` when limits are exceeded.

### 4. **XSS Protection & Input Sanitization**
- `XssSanitizationFilter.java` and `XssRequestWrapper.java` intercept HTTP requests to sanitize input fields against script injections (`<script>`, `javascript:`, inline event handlers like `onerror=`).

### 5. **Account Lockout Policy**
- In `AuthenticationService.java`:
  - Tracks `failedLoginAttempts` per user.
  - Automatically locks the account (`isActive = false`) after **5 consecutive failed login attempts**.
  - Resets attempt counter to `0` upon successful login.

### 6. **JWT Token Revocation & Logout Support**
- Implemented `/api/v1/auth/logout` endpoint in `AuthController` & `TokenService`.
- Revokes refresh tokens in `RefreshTokenRepository`, rendering revoked tokens invalid for future session refresh.

### 7. **Password Policy Enforcement**
- `PasswordPolicy.java` enforces password complexity rules on user registration and password updates:
  - Minimum 8 characters, maximum 64 characters
  - Requires at least 1 uppercase letter (`A-Z`)
  - Requires at least 1 lowercase letter (`a-z`)
  - Requires at least 1 digit (`0-9`)
  - Requires at least 1 special character (`!@#$%^&*()_+-=`)

### 8. **Error Message Disclosure Prevention**
- `GlobalExceptionHandler.java` logs full exception stack traces internally while returning sanitized, non-leaking HTTP error responses to client callers.

---

## 🚀 Postman Setup & Quickstart

### 1. Import the Collection
1. Open **Postman**.
2. Click **Import** (top left).
3. Select the file: [`docs/LOS_Backend_Postman_Collection.json`](file:///c:/Users/Khushmeet%20Patil/Documents/Webiste/New%20folder/LOS-Backend/docs/LOS_Backend_Postman_Collection.json).
4. Click **Import**.

### 2. Environment Setup (Automatic Bearer Token Injection)
The collection includes variables and automatic test scripts:
- **`baseUrl`**: `http://localhost:8080`
- **`accessToken`**: Auto-populated upon successful login
- **`refreshToken`**: Auto-populated upon successful login

> 💡 **Automatic Auth**: When you execute any request in the **Authentication** folder (e.g. *Login - Master Internal Admin* or *Login - HDFC Super Admin*), Postman automatically saves the returned JWT to `{{accessToken}}`. All subsequent requests in the collection automatically use `Bearer {{accessToken}}` in their Authorization header!

---

## 🔑 Seed Test Accounts

| Account Role | Email Identifier | Password | Access Rights / Scope |
| :--- | :--- | :--- | :--- |
| **Master Platform Admin** | `admin@losplatform.com` | `Admin@123` | Platform global management, Organization onboarding |
| **HDFC Super Admin** | `superadmin@hdfcbank.com` | `Admin@123` | Full tenant management within HDFC Bank |
| **HDFC Maker** | `maker@hdfcbank.com` | `Maker@123` | Customer creation, loan application initiation |
| **HDFC Checker** | `checker@hdfcbank.com` | `Checker@123` | Loan application verification & approval |
| **HDFC Viewer** | `viewer@hdfcbank.com` | `Viewer@123` | Read-only access & reporting |
| **HDFC Customer** | `rajesh.kumar@gmail.com` | `Customer@123` | Customer self-service portal |
| **Bajaj Super Admin** | `superadmin@bajajfinance.com` | `Admin@123` | Bajaj Finance tenant management |

---

## 🧪 Step-by-Step API Testing Instructions

### Step 1: Login & Token Acquisition
1. Open the folder `Authentication` in Postman.
2. Select **`1. Login - Master Internal Admin`**.
3. Click **Send**.
4. **Expected Output**: `200 OK` response returning `accessToken`, `refreshToken`, `dashboardUrl`, and `permissions`.
5. Notice that `accessToken` is automatically set in Postman!

### Step 2: Verify Profile (`/me`)
1. Select **`5. Get Current User Profile (/me)`**.
2. Click **Send**.
3. **Expected Output**: `200 OK` returning profile details for `Super Administrator` under Organization `MASTER`.

### Step 3: Create Organization (Master Admin)
1. Select folder `Organizations (Internal Admin)`.
2. Open **`Create Organization`**.
3. Click **Send**.
4. **Expected Output**: `200 OK` returning created organization `ICICI Bank Limited`.

### Step 4: Login as Tenant Staff (HDFC Super Admin)
1. Open **`2. Login - HDFC Super Admin`** in `Authentication`.
2. Click **Send**.
3. The environment variable `{{accessToken}}` is now updated for HDFC Bank!

### Step 5: User Management (Staff User Creation)
1. Select folder `User Management (Tenant Staff)`.
2. Open **`Create Staff User`**.
3. Click **Send**.
4. **Expected Output**: `200 OK` returning created staff user `amit.joshi@hdfcbank.com`.

### Step 6: Customer Management
1. Select folder `Customer Management`.
2. Open **`Create Customer`**.
3. Click **Send**.
4. Open **`Get All Customers (Paginated)`** and click **Send**.
5. **Expected Output**: Returns paginated list of customers belonging to HDFC Bank.

### Step 7: Lead Management (Create, Get, Update, Assign, Import & Export)
1. Select folder `Lead Management`.
2. Open **`1. Create Lead`**:
   - Sends payload containing Step 1 Personal Details with `passportExpiryDate` (no `mobileNumber` or `otp`), Step 2 Loan Details with `interestRate` and `totalInterest` (no `propertyValue`), plus Income Profile and Referral Details.
   - Click **Send**.
   - **Expected Output**: `201 Created` with generated `leadId` (e.g. `LD2026100001`) and `leadStatus: "NEW"`.
3. Open **`2. Get Lead By ID`**:
   - Click **Send**.
   - **Expected Output**: `200 OK` returning complete lead details including `passportExpiryDate`, `interestRate`, and `totalInterest`.
4. Open **`3. Get All Leads`**:
   - Click **Send**.
   - **Expected Output**: `200 OK` returning list of all leads.
5. Open **`4. Update Lead`**:
   - Updates fields (e.g. `passportExpiryDate`, `interestRate`, `totalInterest`).
   - Click **Send**.
   - **Expected Output**: `200 OK` returning updated lead with updated values.
6. Open **`5. Assign Lead`**:
   - Click **Send** to assign lead to an active Maker employee.
   - **Expected Output**: `200 OK` with `leadStatus: "ASSIGNED"`.
7. Open **`6. Download Sample CSV`** & **`7. Export Leads to CSV`**:
   - Click **Send**.
   - **Expected Output**: Downloads RFC-4180 compliant CSV matching updated columns (`Passport Expiry Date`, `Interest Rate`, `Total Interest`).
8. Open **`8. Import Leads from CSV`**:
   - Upload CSV file and click **Send**.
   - **Expected Output**: `200 OK` with `CsvImportResult` showing success count and zero errors.

### Step 8: Test Dashboard Endpoint
1. Select folder `Dashboard`.
2. Open **`Get Role-Based Dashboard`**.
3. Click **Send**.
4. **Expected Output**: Dynamically returns role-specific analytics and metrics based on the current JWT user role.

---

## 🛡️ Testing Security Features in Postman

### A. Testing Rate Limiting (429 Too Many Requests)
1. In Postman, open **`1. Login - Master Internal Admin`**.
2. Repeatedly click **Send** 11 times within 60 seconds.
3. On the 11th request, you will receive:
   - **Status**: `429 Too Many Requests`
   - **Payload**:
     ```json
     {
       "success": false,
       "status": 429,
       "error": "TOO_MANY_REQUESTS",
       "message": "Too many authentication attempts. Please try again after 1 minute."
     }
     ```

### B. Testing Account Lockout (5 Failed Login Attempts)
1. Open **`1. Login - Master Internal Admin`**.
2. Change the password field to an invalid string like `WrongPass123`.
3. Click **Send** 5 times.
4. On the 5th attempt, the response will be:
   - **Status**: `401 Unauthorized`
   - **Message**: `"Your account has been locked due to 5 consecutive failed login attempts. Please contact administrator."`

### C. Testing Password Policy Enforcement
1. Open **`7. Change Password`**.
2. Change `newPassword` to a weak string like `12345`.
3. Click **Send**.
4. **Expected Response**:
   - **Status**: `400 Bad Request`
   - **Error**: `WEAK_PASSWORD`
   - **Message**: `"Password must be at least 8 characters long"`

### D. Testing Logout & Token Revocation
1. Open **`8. Logout (Revoke Token)`**.
2. Click **Send**.
3. **Response**: `"Logged out successfully"`.
4. Now, open **`6. Refresh Token`** and click **Send**.
5. **Expected Response**:
   - **Status**: `401 Unauthorized`
   - **Message**: `"Refresh token has expired or been revoked"`

---

## 📋 Security Headers Verification Matrix

Inspect the **Headers** tab of any API response in Postman:

| Response Header | Expected Value | Purpose |
| :--- | :--- | :--- |
| `Content-Security-Policy` | `default-src 'self'...` | Restricts asset loading sources |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Enforces HTTPS connections |
| `X-Frame-Options` | `DENY` | Prevents framing & clickjacking |
| `X-Content-Type-Options` | `nosniff` | Blocks MIME type guessing |
| `X-XSS-Protection` | `1; mode=block` | Enables browser XSS filters |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Protects referral URLs |
| `Permissions-Policy` | `camera=(), microphone=(), ...` | Restricts browser APIs |

---

## 🏁 Summary

With these enhancements, the LOS Backend guarantees robust security across authentication, authorization, multi-tenant isolation, data sanitization, and rate-limiting. Use the included Postman collection [`docs/LOS_Backend_Postman_Collection.json`](file:///c:/Users/Khushmeet%20Patil/Documents/Webiste/New%20folder/LOS-Backend/docs/LOS_Backend_Postman_Collection.json) to evaluate and verify all API endpoints!
