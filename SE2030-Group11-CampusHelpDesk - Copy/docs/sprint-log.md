# Sprint log — SE2030 Group 11

## Sprint 1 — project baseline

**Goal:** Stand up a shared Campus Help Desk codebase so each member can own one package without colliding.

### Done

- Maven Spring Boot 3.4 app (`com.sliit.helpdesk`) with Thymeleaf + Spring Security + JPA + H2.
- Package-per-module layout matching the group assignment.
- Shared `users`, `categories`, `tickets`, `ticket_comments`, `notifications`, `kb_articles`, `faqs` schema and seed data.
- Working flows: register/login/profile, submit/track tickets, category default assignment, in-app notifications, KB/FAQ, dashboard/reports.
- One unit-test package per module plus a context-load test.

### Owners

| Module | Owner | Notes for next sprint |
| --- | --- | --- |
| Authentication & Profile | Nirasha | Email verification, avatar upload |
| Ticket Submission & Tracking | Kodagoda | Attachments, SLA breach highlighting |
| Categorization & Assignment | Umer | Round-robin when default assignee is busy |
| Communication & Notifications | Arachchi | Email/SMS adapters behind `NotificationService` |
| Knowledge Base & FAQ | Weerasekara | Staff article editor UI |
| Reporting & Dashboard | Jayathilaka | Date-range filters and CSV export |

### Demo accounts

All passwords: `password`  
`admin@sliit.lk` · `staff@sliit.lk` · `student@sliit.lk`
