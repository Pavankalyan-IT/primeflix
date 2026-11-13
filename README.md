# PrimeFlix

PrimeFlix is a Java 21 / Spring Boot video-catalog application with a responsive HTML, CSS, and vanilla JavaScript frontend.

## Requirements

- Java 21
- Maven
- MySQL 8
- A Gmail account with 2-Step Verification and a Gmail App Password (for real email delivery)

## Configure IntelliJ

Open **Run → Edit Configurations**, select the Spring Boot application, and add environment variables. Do not put real credentials in this file or commit them.

Required:

| Variable | Purpose |
| --- | --- |
| `DB_USERNAME` | MySQL application username |
| `DB_PASSWORD` | MySQL application password |
| `JWT_SECRET` | Random signing secret of at least 32 bytes |
| `MAIL_USERNAME` | Gmail account that sends verification and reset emails |
| `MAIL_PASSWORD` | Gmail App Password for that sender account |

Optional:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/pulsescreen_video` | MySQL connection URL |
| `FRONTEND_URL` | `http://localhost:8088` | Public base URL used in email links |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8088` | Allowed frontend origin when frontend is hosted separately |
| `ADMIN_EMAIL` | Empty | Email address to provision/promote as the initial admin |
| `ADMIN_PASSWORD` | Empty | Password for the initial admin; must be 12–72 UTF-8 bytes |

`ADMIN_EMAIL` is the administrator login. `MAIL_USERNAME` is the email sender. They may be the same or different. If an account with `ADMIN_EMAIL` already exists, bootstrap promotes and verifies it but does not change its password.

For Gmail, enable 2-Step Verification, create an App Password for the sending account, and put that generated value in `MAIL_PASSWORD`. It is not the Gmail login password or the PrimeFlix admin password. Restart the application after changing IntelliJ environment variables.

The email links use `FRONTEND_URL`; `localhost` links only open on the same computer that runs the application.

## Run

1. Create the `pulsescreen_video` database and a least-privilege MySQL application user.
2. Configure the environment variables above.
3. Run `mvn spring-boot:run`, or start `com.netflixclone.Application` from IntelliJ.
4. Open [http://localhost:8088](http://localhost:8088).
5. Sign up with a real recipient email, verify it, then sign in. An administrator can create titles and publish them for members.

Run automated tests with `mvn test`.

## Implemented

- Member registration, email verification, sign-in, password reset, password change, and profile display.
- JWT authentication through the `Authorization: Bearer` header. The browser keeps the token in `sessionStorage`; this is convenient for the same-origin demo, but JavaScript injection could expose it.
- Published catalog with server-side title search, categories, pagination, and a featured carousel.
- Video playback with range streaming, watchlist, and player controls.
- Admin statistics, paginated user management, member creation, and video upload/edit/preview/publish/unpublish/delete.
- Backend role enforcement, validation, media file size/type checks, and tests.

## Before production use

- Confirm the live database is MySQL 8. The development database previously reported by the project was MySQL 5.5, which is not the target version.
- Configure Gmail and verify that a real verification and password-reset email arrive. Automated tests do not verify external Gmail delivery.
- Back up existing data before schema changes. Hibernate is currently set to `ddl-auto=update`; production deployments should use a reviewed migration tool such as Flyway.
- Replace all credentials and secrets that have been exposed. Use new, unique values and keep them outside source control.
- Set `FRONTEND_URL` to the deployed HTTPS address before sending email links to users outside the local machine.

