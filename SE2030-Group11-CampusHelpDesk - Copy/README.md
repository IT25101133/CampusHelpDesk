# Campus Help Desk

SE2030 Group 11 — a Spring Boot campus ticketing system for SLIIT. Students open tickets, staff get them by category, and everyone can search the knowledge base first.

## Team modules

| Package | Owner | Responsibility |
| --- | --- | --- |
| `auth` | Nirasha | Authentication & profile |
| `ticket` | Kodagoda | Ticket submission & tracking |
| `category` | Umer | Categorization & assignment |
| `notification` | Arachchi | Communication & notifications |
| `knowledgebase` | Weerasekara | Knowledge base & FAQ |
| `report` | Jayathilaka | Reporting & dashboard |

## Run locally

Requirements: **JDK 17** and **Maven 3.9+**.

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
JDBC URL: `jdbc:h2:file:./data/campus_helpdesk` · user `sa` · empty password.

### Demo accounts

Password for every seeded user: `password`

| Role | Email |
| --- | --- |
| Admin | `admin@sliit.lk` |
| Dept head | `head@sliit.lk` |
| Lecturer | `lecturer@sliit.lk` |
| Staff (Facilities) | `staff@sliit.lk` |
| Staff (IT) | `it.support@sliit.lk` |
| Student | `student@sliit.lk` |

## Layout

```
src/main/java/com/sliit/helpdesk/
  HelpDeskApplication.java
  config/           security, CORS, MVC
  auth/             Nirasha
  ticket/           Kodagoda
  category/         Umer
  notification/     Arachchi
  knowledgebase/    Weerasekara
  report/           Jayathilaka
src/main/resources/
  application.yml
  static/           css, js, images
  templates/        Thymeleaf views
  db/schema.sql
  db/mysql-schema.sql
  db/seed.sql
src/test/java/com/sliit/helpdesk/   one test package per module
docs/               diagrams and sprint log
```

Schema and seed live in `src/main/resources/db/`. Hibernate DDL is off (`ddl-auto: none`); SQL scripts create tables and demo rows.

Tables: `users`, `ticket_categories`, `tickets`, `ticket_attachments`, `ticket_comments`, `notifications`, `kb_categories`, `kb_articles`, `audit_logs`.

To use MySQL Workbench, run `src/main/resources/db/mysql-schema.sql`, then start with `-Dspring-boot.run.profiles=mysql`.

## Tests

```bash
mvn test
```

## Docs

- `docs/er-diagram.png`
- `docs/use-case-diagram.png`
- `docs/class-diagram.png`
- `docs/sprint-log.md`
