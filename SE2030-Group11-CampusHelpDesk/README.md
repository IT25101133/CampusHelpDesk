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

Requirements: **JDK 17+**, **Maven 3.9+**, and **MySQL** running on `localhost:3306`.

Create the database once (Workbench or MySQL CLI):

```sql
CREATE DATABASE IF NOT EXISTS campus_helpdesk;
```

If `mvn` is not recognized on Windows, add Maven to PATH for that terminal:

```powershell
$env:Path = "C:\Users\user\apache-maven-3.9.9\bin;" + $env:Path
cd E:\SE2030-Group11-CampusHelpDesk
```

Set MySQL credentials if they are not `root` with an empty password:

```powershell
$env:DB_USER = "root"
$env:DB_PASS = "yourpassword"
```

Then start the app:

```powershell
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

### Test login

Created automatically on first start:

| Email | Password | Role |
| --- | --- | --- |
| `test@sliit.lk` | `Test1234` | Student |

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
