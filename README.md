# Scrap Connection — Complete Full-Stack Spring Boot Application

**Scrap Connection** is an enterprise-grade full-stack web application developed with **Spring Boot 3.3.5**, **MySQL**, **Spring Data JPA / Hibernate**, **Spring Security**, and **Thymeleaf**. The platform bridges the gap between urban households/businesses selling recyclable scrap and verified local scrap shopkeepers, while offering an integrated recycled-goods marketplace, digital coupon wallet, and automated loyalty reward system.

Designed specifically for academic evaluation (e.g., B.Tech Information Technology capstone projects) and production readiness in **Spring Tool Suite (STS)** and the **MySQL Command Line Client**.

---

## Table of Contents
1. [Core Features](#1-core-features)
2. [Technology Stack](#2-technology-stack)
3. [Architecture & Project Structure](#3-architecture--project-structure)
4. [Prerequisites](#4-prerequisites)
5. [Database Setup (MySQL Command Line)](#5-database-setup-mysql-command-line)
6. [Importing & Running in Spring Tool Suite (STS)](#6-importing--running-in-spring-tool-suite-sts)
7. [Running via Command Line / Terminal](#7-running-via-command-line--terminal)
8. [Pre-Seeded Demo Credentials](#8-pre-seeded-demo-credentials)
9. [End-to-End User Journeys](#9-end-to-end-user-journeys)
10. [Automated Test Suite (17 Comprehensive Tests)](#10-automated-test-suite-17-comprehensive-tests)
11. [Configuration Reference](#11-configuration-reference)
12. [Troubleshooting & FAQ](#12-troubleshooting--faq)

---

## 1. Core Features

### 👤 Multi-Role Identity & Access Management
* **Role-Based Access Control:** Distinct portals for `ROLE_CUSTOMER`, `ROLE_SHOPKEEPER`, and `ROLE_ADMIN`.
* **Mobile Number Identifier:** Mobile numbers act as unique login credentials paired with BCrypt-hashed passwords (`$2a$10$...`).
* **Cryptographic OTP Verification:** 6-digit secure OTP with 5-minute expiry, rate-limiting (maximum 3 requests per 10 minutes, 5 verification attempts), BCrypt OTP storage, and zero-cost development fallback mode.
* **Shopkeeper KYC Verification:** Shopkeepers undergo an administrative approval workflow (`PENDING` ➔ `APPROVED` or `REJECTED`) before accessing pickup requests.

### ♻️ Scrap Booking & Doorstep Settlement Lifecycle
* **Booking Lifecycle:** `REQUESTED` ➔ `ACCEPTED` ➔ `PICKUP_IN_PROGRESS` ➔ `COMPLETED` (or `CANCELLED`/`REJECTED`).
* **Concurrency Conflict Prevention:** Database-level locking prevents multiple shopkeepers from accepting the same booking simultaneously.
* **Doorstep Weighing & Server-Side Math:** Exact line-item computation using `BigDecimal` (`weight × unitPrice - deductions = netAmount`) with scale-2 half-up rounding.
* **Flexible Settlement Options:** Cash, UPI (with transaction reference), or Digital Scrap Coupon.
* **Single Settlement Guarantee:** Strict database checks prevent duplicate settlement payouts for any booking.

### 🎟️ Coupon Wallet & Loyalty Rewards
* **Digital Coupon Wallet:** When settled via coupon, unique alphanumeric vouchers (`CPN-XXXXXX`) are issued.
* **Partial Coupon Redemption:** Real-time formula calculates `discount = min(orderPayable, availableCouponBalance)`.
* **Immutable Coupon Ledger:** FIFO balance deductions logged in an append-only audit trail (`CouponLedger`).
* **Automated Loyalty Points:** Configurable policy (default: 1 point per ₹10 scrap sold) awarded exactly once upon scrap completion.

### 🛒 Recycled Marketplace
* **Sustainable Catalog:** Filter by category, price, and stock availability.
* **Persistent Cart:** Session and database-synced shopping cart.
* **Atomic Checkout:** Server-side price recalculation, coupon wallet application, order placement, and atomic stock decrements.

### 🛡️ Administrative Portal
* **Live KPI Metrics:** Real-time platform statistics (total scrap collected in kg, total payout value, active customers, verified shops, marketplace revenue).
* **Shopkeeper KYC Approvals:** Quick one-click approval or rejection of onboarding applications.
* **Dynamic Material Rate Cards:** Real-time updates to standard scrap prices (Paper, Plastic, Metals, E-Waste, etc.).
* **Marketplace Inventory Manager:** Add, edit, and toggle active stock for recycled products.
* **Audit Trail:** Read-only log of critical administrative actions.

---

## 2. Technology Stack

| Layer | Technologies Used |
|---|---|
| **Backend Framework** | Spring Boot 3.3.5, Spring MVC, Spring Data JPA, Hibernate 6, Spring Security 6 |
| **Language & Runtime** | Java 17 / 21+ compatible bytecode (`<maven.compiler.release>21</maven.compiler.release>`) |
| **Database** | MySQL 8.0+ (Connector/J 8.0.33) with H2 In-Memory DB for automated unit tests |
| **Security** | Session-based authentication, BCrypt password hashing, CSRF protection, Role hierarchies |
| **Frontend UI** | HTML5, CSS3 (Modern Glassmorphic Responsive Design System), Vanilla JavaScript |
| **Template Engine** | Thymeleaf 3 with Spring Security Dialect (`thymeleaf-extras-springsecurity6`) |
| **Build Tool** | Apache Maven 3.8+ |
| **Testing** | JUnit 5 Jupiter, Mockito, Spring Boot Test |

---

## 3. Architecture & Project Structure

```
ScrapCollectionPlatform/
├── pom.xml                                   # Maven dependencies & build configuration
├── mysql-setup.sql                           # Standalone database initialization script
├── README.md                                 # Complete documentation & user guide
└── src/
    ├── main/
    │   ├── java/com/scrapconnection/
    │   │   ├── ScrapConnectionApplication.java# Spring Boot Application entrypoint
    │   │   ├── config/                       # Security, MVC, and Data Seeder configs
    │   │   │   ├── SecurityConfig.java       # Role-based route authorization & login rules
    │   │   │   ├── CustomAuthenticationSuccessHandler.java # Dynamic role dashboard router
    │   │   │   ├── DataInitializer.java      # Seed demo users, categories, products
    │   │   │   └── GlobalExceptionHandler.java# Centralized 403, 404, 409, 500 error mapping
    │   │   ├── controller/                   # Spring MVC Web Controllers
    │   │   │   ├── HomeController.java        # Landing page, about, dynamic rates
    │   │   │   ├── AuthController.java        # Login, registration, OTP validation
    │   │   │   ├── CustomerController.java    # Book scrap, coupon wallet, loyalty view
    │   │   │   ├── ShopkeeperController.java  # Accept requests, weigh scrap, settlements
    │   │   │   ├── MarketplaceController.java # Recycled goods catalog, cart, checkout
    │   │   │   └── AdminController.java        # KPIs, shop approvals, rate management
    │   │   ├── entity/                       # 22 JPA Entities with JPA relationships
    │   │   │   ├── User.java, CustomerProfile.java, ShopkeeperProfile.java
    │   │   │   ├── Address.java, OtpVerification.java
    │   │   │   ├── MaterialCategory.java, MaterialPrice.java
    │   │   │   ├── ScrapBooking.java, BookingMaterial.java, BookingStatusHistory.java
    │   │   │   ├── Settlement.java, Coupon.java, CouponLedger.java
    │   │   │   ├── LoyaltyAccount.java, LoyaltyTransaction.java
    │   │   │   ├── Product.java, CartItem.java, CustomerOrder.java, OrderItem.java
    │   │   │   ├── OrderPayment.java, AdminAuditLog.java
    │   │   ├── enums/                        # Strongly-typed domain enumerations
    │   │   ├── repository/                   # 18 Spring Data JPA repositories
    │   │   └── service/                      # Transactional business logic layer
    │   │       ├── UserService.java, OtpService.java, BookingService.java
    │   │       ├── SettlementService.java, CouponService.java, LoyaltyService.java
    │   │       ├── MarketplaceService.java, AdminService.java, AuditService.java
    │   └── resources/
    │       ├── application.properties        # App properties, DB config, OTP settings
    │       ├── schema.sql                    # Production DDL schema
    │       ├── data.sql                      # Production baseline seed data
    │       ├── static/
    │       │   ├── css/style.css             # Unified CSS3 design system & animations
    │       │   └── js/main.js                # Weighing calculators & checkout preview
    │       └── templates/                    # Thymeleaf UI templates
    │           ├── index.html, about.html    # Public pages
    │           ├── fragments/                # Reusable navbar, footer, alerts
    │           ├── auth/                     # Login, Registration, OTP screens
    │           ├── customer/                 # Customer dashboard, booking, wallet
    │           ├── shopkeeper/               # Shop dashboard, weighing, settlements
    │           ├── marketplace/              # Catalog, details, cart, checkout
    │           ├── admin/                    # KPI dashboard, approvals, rates, inventory
    │           └── error/                    # 403, 404, 500 custom error pages
    └── test/
        ├── java/com/scrapconnection/         # 17 Automated JUnit 5 test cases
        │   ├── RegistrationAndAuthTest.java
        │   ├── BookingWorkflowTest.java
        │   ├── ScrapSettlementAndLoyaltyTest.java
        │   └── CouponAndMarketplaceTest.java
        └── resources/application.properties  # Isolated H2 in-memory test configuration
```

---

## 4. Prerequisites

Before running the application, make sure you have the following installed on your machine:

1. **Java Development Kit (JDK):** JDK 17, JDK 21, or newer.
   * Verify in command prompt: `java -version`
2. **Apache Maven:** 3.8+ (bundled inside Spring Tool Suite).
   * Verify in command prompt: `mvn -version`
3. **MySQL Server:** Version 8.0 or newer running locally on port `3306`.
   * Verify service status: Open Windows Services (`services.msc`) and check `MySQL80` is **Running**.
4. **IDE (Recommended):** **Spring Tool Suite (STS) 4**, Eclipse with Spring Tools, or IntelliJ IDEA.

---

## 5. Database Setup (MySQL Command Line)

### Step 5.1: Open the MySQL Command Line Client
Open the **MySQL 8.0 Command Line Client** from your Windows Start Menu, or open PowerShell/Command Prompt and run:
```bash
mysql -u root -p
```
Enter your MySQL root password when prompted.

### Step 5.2: Execute Database Initialization
Run the following SQL commands directly in the MySQL prompt:
```sql
CREATE DATABASE IF NOT EXISTS scrap_connection
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE scrap_connection;

SELECT 'Database scrap_connection is ready!' AS Status;
```

Alternatively, you can run the provided script from your terminal:
```bash
cd c:\Users\Chakr\OneDrive\Desktop\ScrapCollectionPlatform
mysql -u root -p < mysql-setup.sql
```

> **Note on Tables and Seed Data:**  
> When the Spring Boot application starts, Hibernate will automatically verify and create all required tables (`spring.jpa.hibernate.ddl-auto=update`), and `DataInitializer.java` will automatically insert all required seed categories, rates, recycled products, and test accounts.

---

## 6. Importing & Running in Spring Tool Suite (STS)

Follow these steps to import and run the project in STS:

### Step 6.1: Import into STS
1. Open **Spring Tool Suite (STS)**.
2. Select **File ➔ Import...** from the top menu bar.
3. In the Import wizard, expand **Maven** and choose **Existing Maven Projects**, then click **Next**.
4. In the **Root Directory** field, click **Browse...** and select:
   `c:\Users\Chakr\OneDrive\Desktop\ScrapCollectionPlatform`
5. Ensure `pom.xml` is checked in the Projects list, and click **Finish**.
6. Wait 15–30 seconds for STS to resolve dependencies and build the workspace.

### Step 6.2: Configure Database Password (If Needed)
Open `src/main/resources/application.properties` in STS.  
Verify lines 11–13:
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/scrap_connection?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:root}
```
* If your MySQL root password is **not** `root`, change `spring.datasource.password=YOUR_PASSWORD` or define an environment variable `DB_PASSWORD`.

### Step 6.3: Run the Application
1. In the **Package Explorer** or **Project Explorer**, locate the project: `ScrapCollectionPlatform`.
2. Right-click on the project root folder ➔ **Run As** ➔ **Spring Boot App**.
   *(Alternatively, expand `src/main/java/com/scrapconnection`, right-click `ScrapConnectionApplication.java`, and select **Run As ➔ Java Application**).*
3. Open the **Console** tab in STS. You should see the Spring Boot banner, table creation logs, and seed messages:
   ```
   [DataInitializer] Seeding initial platform data...
   [DataInitializer] Seeded 8 scrap material categories with standard rates.
   [DataInitializer] Seeded 6 recycled marketplace products.
   [DataInitializer] Seeded demo accounts (Admin, Customer, Approved Shopkeeper, Pending Shopkeeper).
   Started ScrapConnectionApplication in X.XXX seconds
   ```
4. Open your web browser and navigate to:
   👉 **`http://localhost:8080`**

---

## 7. Running via Command Line / Terminal

If you prefer using PowerShell, Command Prompt, or terminal:

```bash
# Navigate to project directory
cd c:\Users\Chakr\OneDrive\Desktop\ScrapCollectionPlatform

# Run the Spring Boot application using Maven wrapper / Maven
mvn spring-boot:run
```

To run with custom MySQL credentials without editing `application.properties`:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.password=YourActualPassword"
```

---

## 8. Pre-Seeded Demo Credentials

The platform initializes with pre-configured accounts for testing each role:

| Role | Mobile Number | Password | Account Status | Capabilities |
|---|---|---|---|---|
| **Admin** | `9999999999` | `Admin@1234` | `ACTIVE` | System KPIs, Shopkeeper KYC approvals, Scrap price management, Product inventory control, Audit log tracking |
| **Customer** | `9876543210` | `Customer@123` | `ACTIVE` | Book scrap pickup, View status timeline, Digital coupon wallet, Loyalty rewards balance, Recycled marketplace ordering |
| **Approved Shopkeeper** | `9123456789` | `Shop@1234` | `ACTIVE` (KYC: `APPROVED`) | Browse open pickup requests, Accept bookings (preventing double pickup), Doorstep scrap weighing calculator, Issue Cash/UPI/Coupon settlements |
| **Pending Shopkeeper** | `9811122233` | `Pending@123` | `ACTIVE` (KYC: `PENDING`) | Demonstrates pending KYC barrier — cannot view or accept scrap requests until approved by Admin |

> 💡 **Quick Login Assistant:**  
> The Login page (`/login`) includes interactive **"Demo Credentials"** buttons that will auto-fill the form with any selected role's credentials for fast testing.

---

## 9. End-to-End User Journeys

### Journey 1: Customer Books Scrap & Shopkeeper Settles Doorstep Payment
1. Log in as **Customer** (`9876543210` / `Customer@123`).
2. Go to **"Book Scrap Pickup"** from the navigation bar or dashboard.
3. Select recyclable categories (e.g., Paper, Iron, Plastic), enter estimated weights, choose an available date and time slot, and specify pickup address.
4. Click **"Confirm Scrap Request"**. The booking enters the `REQUESTED` state.
5. Log out and log in as **Approved Shopkeeper** (`9123456789` / `Shop@1234`).
6. Click **"Pickup Requests"**. You will see the new booking.
7. Click **"Accept Pickup"**. Concurrency checks verify no other shop has accepted this request, transitioning it to `ACCEPTED`.
8. Go to **"Active Pickups"** and click **"Start Pickup"** ➔ enters `PICKUP_IN_PROGRESS`.
9. Click **"Weigh & Settle"**.
10. The interactive weighing table auto-populates category market rates:
    * Enter verified weights (e.g., Paper: 25.5 kg, Iron: 12.0 kg).
    * Enter any deductions/adjustments (e.g., ₹10 moisture deduction).
    * Watch the client-side JavaScript update the Gross Total, Deductions, and Net Payable in real-time.
11. Choose **Settlement Method**:
    * **CASH**: Immediate cash receipt logged.
    * **UPI**: Enter UPI Reference ID (e.g., `UPI/2026/889911`).
    * **COUPON**: Generates a digital coupon code with 90-day validity deposited into the customer's wallet.
12. Click **"Complete Settlement"**.
13. Log back in as **Customer**:
    * View the **Bookings** page: Booking is now `COMPLETED` with an itemized payment receipt.
    * Check **Coupons**: If coupon settlement was selected, the coupon appears in the wallet.
    * Check **Loyalty Points**: Customer earned **1 point per ₹10** scrap value (e.g., ₹500 scrap = 50 loyalty points).

---

### Journey 2: Marketplace Order & Coupon Wallet Redemption
1. Log in as **Customer** (`9876543210` / `Customer@123`).
2. Navigate to **"Marketplace"** in the top navigation bar.
3. Browse products made from recycled materials (e.g., *Recycled Paper Notebooks*, *Upcycled Plastic Planters*, *Reclaimed Aluminium Water Bottles*).
4. Select quantity and click **"Add to Cart"**.
5. Go to **Cart** (`/marketplace/cart`) and click **"Proceed to Checkout"**.
6. On the **Checkout** page:
    * The server inspects the customer's available coupon wallet balance.
    * Toggle **"Apply Scrap Coupon Wallet Discount"**.
    * The system computes `discount = min(orderPayable, availableCouponBalance)` and reduces the final total.
7. Enter delivery address and choose payment method (COD or Online).
8. Click **"Place Order"**.
9. Order placed successfully:
    * Product inventory decrements atomically.
    * The redeemed coupon amount is deducted from the customer's wallet with an immutable FIFO ledger entry.
    * View the order summary in **"My Orders"**.

---

### Journey 3: Admin Review, Scrap Rates & Shopkeeper Approval
1. Log in as **Admin** (`9999999999` / `Admin@1234`).
2. Explore the **Admin Dashboard**:
    * Live statistics showing Total Weight Collected, Platform Scrap Value, Active Users, and Marketplace Sales.
3. Click **"Shopkeeper Approvals"**:
    * Notice shopkeeper **Pending Scrap Traders** (`9811122233`) in `PENDING` status.
    * Review shop details (business name, license, address).
    * Click **"Approve"**. Status transitions to `APPROVED`, granting that shopkeeper full access to open bookings.
4. Click **"Scrap Rates"**:
    * View standard rates per kg.
    * Update rates (e.g., increase Copper from ₹420/kg to ₹450/kg).
    * All subsequent weighing calculations will immediately use the updated rate.
5. Click **"Recycled Products"**:
    * Add new recycled products or toggle inventory stock status.
6. Click **"Audit Logs"**:
    * View timestamped records of shopkeeper approvals, rate modifications, and critical system events.

---

## 10. Automated Test Suite (17 Comprehensive Tests)

The application includes an extensive test suite built using **JUnit 5**, **Mockito**, and **Spring Boot Test**. The tests validate all 17 critical business workflows without requiring an external MySQL server by using an isolated in-memory **H2** database.

### Running Tests in Terminal:
```bash
mvn test
```

### Running Tests in STS:
1. In the STS Package Explorer, expand `src/test/java/com/scrapconnection`.
2. Right-click on the package or any test class ➔ **Run As** ➔ **JUnit Test**.

### Test Suite Breakdown:

#### 1. `RegistrationAndAuthTest` (4 Tests)
* `testCustomerRegistrationWithOtpVerification`: Validates dual customer registration, OTP delivery in development mode, and successful mobile activation.
* `testShopkeeperRegistrationPendingState`: Validates shopkeeper onboarding initialized to `PENDING` status.
* `testRateLimitingOnOtpRequests`: Asserts that requesting more than 3 OTPs within 10 minutes throws rate-limiting exceptions.
* `testOtpExpirationAndInvalidAttempts`: Validates that invalid attempts decrement allowance and expired OTPs are rejected.

#### 2. `BookingWorkflowTest` (4 Tests)
* `testCreateScrapBookingLifecycle`: Validates scrap booking creation with multiple material items in `REQUESTED` state.
* `testShopkeeperAcceptBooking`: Tests successful booking acceptance transitioning status to `ACCEPTED`.
* `testDoubleAcceptanceConflictPrevention`: Asserts concurrency conflict prevention when two shopkeepers attempt to accept the same booking.
* `testUnapprovedShopkeeperCannotAcceptBooking`: Confirms that `PENDING` or `REJECTED` shopkeepers are barred from accepting customer pickups.

#### 3. `ScrapSettlementAndLoyaltyTest` (5 Tests)
* `testDoorstepWeighingCalculationAndSettlement`: Asserts server-side calculation of `weight * rate - deductions = netAmount` with `BigDecimal`.
* `testPreventDuplicateSettlement`: Confirms duplicate settlement attempts throw `ConflictException`.
* `testCouponSettlementCreatesCouponAndLedger`: Verifies that coupon settlement creates a valid coupon and records an audit entry.
* `testLoyaltyPointsAwardedOnCompletion`: Tests loyalty point calculation (1 pt per ₹10 scrap value).
* `testLoyaltyPointsAwardedOnlyOnce`: Confirms idempotency — loyalty points cannot be awarded more than once for the same booking.

#### 4. `CouponAndMarketplaceTest` (4 Tests)
* `testCouponPartialRedemptionCalculation`: Tests partial redemption logic (`min(orderPayable, availableCouponBalance)`).
* `testMarketplacePersistentCart`: Tests adding and removing items from a customer's persistent cart.
* `testAtomicCheckoutInventoryDecrement`: Validates that stock decrements properly upon order placement and out-of-stock orders are blocked.
* `testCheckoutWithCouponWalletDiscount`: Tests end-to-end checkout with coupon discounts and audit ledger balance deduction.

**Result: 17 Tests Run, 0 Failures, 0 Errors — 100% Pass Rate.**

---

## 11. Configuration Reference

All application properties are configured in `src/main/resources/application.properties`:

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port for the web server |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/scrap_connection...` | JDBC Connection URL |
| `spring.datasource.username` | `root` | MySQL Database Username |
| `spring.datasource.password` | `root` | MySQL Database Password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Automatically synchronizes entity schema |
| `app.sms.mode` | `development` | Set to `development` to display OTP in UI/logs, or `production` for real SMS gateway |
| `app.otp.expiry-minutes` | `5` | OTP validity window in minutes |
| `app.otp.max-attempts` | `5` | Allowed incorrect OTP attempts before invalidation |
| `app.otp.rate-limit-minutes`| `10` | Rate limiting sliding window duration |
| `app.otp.max-requests-per-window` | `3` | Maximum OTP request calls per window |
| `app.loyalty.points-per-rupees` | `10` | Rupees of scrap value required to earn 1 loyalty point |
| `app.coupon.validity-days` | `90` | Lifetime of newly issued scrap coupons in days |

---

## 12. Troubleshooting & FAQ

### Q1: Port 8080 is already in use (`PortInUseException`)
* **Cause:** Another program (such as Apache Tomcat, Jenkins, or an earlier running instance) is using port 8080.
* **Fix 1 (Change port):** In `application.properties`, set `server.port=8081` and access via `http://localhost:8081`.
* **Fix 2 (Kill existing process on Windows):**
  ```powershell
  netstat -ano | findstr :8080
  taskkill /PID <PID_NUMBER> /F
  ```

### Q2: `Access denied for user 'root'@'localhost'`
* **Cause:** The MySQL password in `application.properties` does not match your MySQL server password.
* **Fix:** Update line 13 of `src/main/resources/application.properties`:
  ```properties
  spring.datasource.password=YourActualMySQLPassword
  ```

### Q3: How do I test OTP verification during Customer Registration?
* Because `app.sms.mode=development` is enabled, whenever an OTP is requested:
  1. The OTP is **printed in clear text in the STS console**:
     `[DEVELOPMENT SMS SIMULATOR] SMS dispatched to 9876543210 -> Your Scrap Connection verification code is: 123456 (Valid for 5 mins)`
  2. The OTP is also **conveniently shown in a banner on the OTP entry page** so you can easily copy and paste it during evaluation.

### Q4: STS shows red compilation errors on import
* **Fix:** Right-click the project ➔ **Maven** ➔ **Update Project...** (or press `Alt + F5`). Ensure **"Force Update of Snapshots/Releases"** is checked and click **OK**.

---

## License & Authorship

Developed as a full-stack academic capstone project demonstrating Spring Boot enterprise architecture, secure role-based access control, relational database design with MySQL, and responsive UI engineering.
