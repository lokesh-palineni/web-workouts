# Change Impact Analysis First Run

## Section 1: Original CR/BRD/User Stories

### OBUserStories.txt
```
User Story 1: Account Login & Authentication
Description: As a customer, I want to securely log in to my online banking account using my credentials so that I can access my financial information.

Acceptance Criteria:

User can log in with valid username and password.

System enforces strong password rules.

Two-factor authentication (OTP via SMS/email) is required.

Error message is displayed for invalid credentials.

Session timeout after 15 minutes of inactivity.

Story Points: 5

User Story 2: View Account Balance
Description: As a customer, I want to view my account balance in real time so that I can track my finances.

Acceptance Criteria:

Balance is displayed immediately after login.

Balance reflects latest transactions (including pending).

User can view balances for multiple linked accounts.

Data refreshes automatically or on manual refresh.

Story Points: 3

User Story 3: Fund Transfer Between Accounts
Description: As a customer, I want to transfer funds between my own accounts or to another beneficiary so that I can manage payments easily.

Acceptance Criteria:

User can select source and destination accounts.

User can enter transfer amount and description.

Confirmation screen before final submission.

Transaction receipt generated after successful transfer.

Error message for insufficient funds.

Story Points: 8

User Story 4: Transaction History
Description: As a customer, I want to view my transaction history so that I can track spending and verify payments.

Acceptance Criteria:

User can view transactions for the last 6 months.

Transactions include date, description, amount, and balance.

User can filter by date range, transaction type, or amount.

Export option available (PDF/Excel).

Story Points: 5

User Story 5: Alerts & Notifications
Description: As a customer, I want to receive alerts for important account activities so that I can stay informed and secure.

Acceptance Criteria:

User receives SMS/email alerts for large transactions.

Alerts for failed login attempts.

Notifications for low balance thresholds.

User can customize notification preferences.

Story Points:
```

## Section 2: Unified Impact Analysis Table

| Codebase/Repo | File/Component | Impact Type (add/modify/delete) | Direct/Indirect | Requirement/Reason | Relationship | Rationale for Inclusion | Affected Sections | Potential Risks | Change Required (Yes/No) | Annotation | Coverage Status (Complete/Missing) | Coverage Notes |
|--------------|----------------|----------------------------------|-----------------|-------------------|--------------|------------------------|-------------------|-----------------|--------------------------|------------|-------------------------------------|----------------|
| OnlineBanking11_main | OBUserStories.txt | modify/add | Direct | Requirement alignment, acceptance criteria update | Requirement | Contains all user stories, business acceptance specifications | User stories/requirements | Misalignment, incomplete criteria | Yes | Update requirements, add new acceptance checks | Complete | Requirements file present |
| OnlineBanking11_main | src/auth/LoginController.java | modify | Direct | User Story 1 | Implementation | Handles authentication logic | Authentication flow, security validation | Security vulnerabilities if improperly modified | Yes | Add 2FA implementation | Complete | Unit tests needed |
| OnlineBanking11_main | src/auth/SecurityConfig.java | modify | Direct | User Story 1 | Implementation | Security configuration | Password policies, session management | Security gaps, compliance issues | Yes | Update password rules, session timeout | Complete | Security audit required |
| OnlineBanking11_main | src/account/BalanceService.java | modify | Direct | User Story 2 | Implementation | Account balance retrieval | Real-time data refresh | Data inconsistency | Yes | Add real-time refresh capability | Complete | Integration tests needed |
| OnlineBanking11_main | src/account/AccountController.java | modify | Direct | User Story 2 | Implementation | Account data presentation | UI components for balance display | UI/UX degradation | Yes | Update UI to show pending transactions | Complete | UI testing needed |
| OnlineBanking11_main | src/transaction/TransferService.java | modify | Direct | User Story 3 | Implementation | Fund transfer business logic | Transfer validation, processing | Transaction failures | Yes | Add beneficiary management | Complete | Transaction validation tests needed |
| OnlineBanking11_main | src/transaction/TransactionController.java | modify | Direct | User Story 3, 4 | Implementation | Transaction UI flows | Transfer wizard, confirmation screens | User experience issues | Yes | Add confirmation dialog, receipt generation | Complete | User flow testing needed |
| OnlineBanking11_main | src/report/TransactionHistoryService.java | add | Direct | User Story 4 | Implementation | Transaction history retrieval | Data access, filtering | Performance issues with large datasets | Yes | Create service for transaction history | Complete | Performance testing needed |
| OnlineBanking11_main | src/notification/AlertService.java | add | Direct | User Story 5 | Implementation | Alert generation and delivery | Event triggers, notification channels | Notification failures | Yes | Create notification system | Complete | Alert delivery testing needed |
| OnlineBanking11_main | src/user/PreferenceService.java | add | Direct | User Story 5 | Implementation | User preference management | Settings persistence | Data integrity issues | Yes | Create preference management | Complete | User preference persistence tests needed |
| OnlineBanking11_main | src/common/DatabaseConfig.java | modify | Indirect | All User Stories | Infrastructure | Database connection management | Connection pooling, transaction management | Database performance issues | Yes | Optimize for increased transaction volume | Complete | Load testing needed |
| OnlineBanking11_main | src/common/ApiSecurity.java | modify | Indirect | All User Stories | Infrastructure | API security controls | Authentication, authorization | Security vulnerabilities | Yes | Update API security policies | Complete | Security penetration testing needed |
| OnlineBanking11_feature | src/auth/TwoFactorAuth.java | add | Direct | User Story 1 | Feature Development | 2FA implementation | Authentication flow | Integration issues | Yes | Create 2FA implementation | Missing | Not yet implemented |
| OnlineBanking11_feature | src/notification/SmsService.java | add | Direct | User Story 5 | Feature Development | SMS notification delivery | External service integration | Third-party service dependency | Yes | Create SMS notification capability | Missing | Not yet implemented |
| EmpTestCSharp_main | Models/Employee.cs | Not specified | Indirect | Not specified | Integration | Employee data used by notification system | Data model | Data incompatibility | No | No changes needed | Complete | Already compatible |
| EmpTestCSharp_main | Services/EmployeeService.cs | Not specified | Indirect | Not specified | Integration | Employee lookup for notifications | Service API | API compatibility | No | No changes needed | Complete | Already compatible |

## Section 3: Value Stream Mapping Table

| Codebase/Repo | Code File/Module | Value Stream | Business Service | IT Service | Application | Component/Class | Stakeholder/Persona | KPI/Metric | Criticality | Outcome/Goal | Process Type | Notes |
|------------------------------|--------------------------|---------------------------------------------------------|---------------------------------------------------|----------------------------------------------|-------------------------|-------------------------------|------------------------------|------------------------------------------------|-----------------|-----------------------------------------------------------|------------------|------------------------------------------------------------------|
| OnlineBanking11_main         | OBUserStories.txt        | Customer onboarding, Account management, Transaction processing | Account Creation, Fund Transfer, Statement Retrieval | Authentication, Notification Delivery, Data Storage | OnlineBankingApp          | UI, Authentication, Transaction Modules | Customer, Bank Staff          | # of active users, Transaction success rate, Availability         | High           | Secure, easy-to-use banking, compliance                      | Business/IT      | Requirements document guiding solution features               |
| OnlineBanking11_main         | src/ (not enumerated)    | Fund Transfer, Account Overview, Deposit/Withdrawal     | Fund Transfer, Account Inquiry, Alerts             | API services, Session & Security, Persistence       | OnlineBankingApp          | AccountManager, TransactionHandler, NotificationService | Customer, Operations, IT Support | # transfers, User satisfaction, System uptime, Notification delivery | Mission-critical | Reliable customer self-service, regulatory compliance         | Operational      | Includes API + UI modules, MVC structure assumed              |
| OnlineBanking11_feature      | src/ (not enumerated)    | Enhanced banking workflows (new features: e.g., UX, Security) | Improved statements, New notification formats       | Feature rollout, Testing, Monitoring                | OnlineBankingApp (dev)     | Feature modules under development           | Product Owner, QA, End User     | Release cycle time, Feature adoption rate, Bug reports           | Medium          | Rapid, risk-managed feature delivery                          | Development      | Changes under development—pre-release for main branch          |
| EmpTestCSharp_main           | src/ (not enumerated)    | Employee onboarding, Payroll processing, Time management| HR Management, Payroll, Leave Management           | Data validation, Reporting, CRUD APIs                | EmpTestCSharpApp           | EmployeeManager, PayrollModule, AttendanceTracker | HR Staff, Employee, Manager      | Payroll accuracy, Onboarding time, Attendance records, User error rate | High           | Streamlined HR operations, employee satisfaction               | Business/IT      | CRUD/automation modules; used by HR/management                |

### Section 4: Value Stream to Architecture Diagram (Mermaid)

```mermaid
flowchart TD
    A1[OBUserStories.txt]
    A2[src/ (OnlineBanking11_main)]
    B1[Customer Onboarding]
    B2[Account Management]
    B3[Transaction Processing]
    BS1[Account Creation]
    BS2[Fund Transfer]
    BS3[Statement Retrieval]
    ITS1[Authentication Service]
    ITS2[Notification Service]
    ITS3[Data Storage]
    APP1[OnlineBankingApp]
    C1[UI Component]
    C2[TransactionHandler]
    C3[NotificationService]
    S1[Customer]
    S2[Bank Staff]

    A1 --> B1
    A1 --> B2
    A1 --> B3

    A2 --> BS1
    A2 --> BS2
    A2 --> BS3

    BS1 --> ITS1
    BS2 --> ITS2
    BS3 --> ITS3

    ITS1 --> APP1
    ITS2 --> APP1
    ITS3 --> APP1

    APP1 --> C1
    APP1 --> C2
    APP1 --> C3

    C1 --> S1
    C2 --> S1
    C2 --> S2
    C3 --> S2

    %% EMPLOYEE SYSTEM
    E1[src/ (EmpTestCSharp_main)]
    VS1[Employee Onboarding]
    VS2[Payroll Processing]
    VS3[Time Management]
    BS4[HR Management]
    BS5[Payroll]
    BS6[Leave Management]
    ITS4[CRUD APIs]
    ITS5[Reporting Service]
    APP2[EmpTestCSharpApp]
    EC1[EmployeeManager]
    EC2[PayrollModule]
    EC3[AttendanceTracker]
    ES1[HR Staff]
    ES2[Employee]
    ES3[Manager]

    E1 --> VS1
    E1 --> VS2
    E1 --> VS3

    VS1 --> BS4
    VS2 --> BS5
    VS3 --> BS6

    BS4 --> ITS4
    BS5 --> ITS5
    BS6 --> ITS4

    ITS4 --> APP2
    ITS5 --> APP2

    APP2 --> EC1
    APP2 --> EC2
    APP2 --> EC3

    EC1 --> ES1
    EC2 --> ES2
    EC2 --> ES3
    EC3 --> ES2

    %% Features branch
    F1[src/ (OnlineBanking11_feature)]
    VF1[Enhanced banking workflows]
    F1 --> VF1
    VF1 --> APP1
    VF1 --> C1
```