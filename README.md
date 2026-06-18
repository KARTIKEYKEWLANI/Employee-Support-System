# Customer Support Ticketing System

Full-stack ticketing platform built with React, Spring Boot, and MySQL.

## Project Structure

- `backend/` Spring Boot API
- `frontend/` React (Vite) client

## Backend Setup (Spring Boot + MySQL)

1. Start MySQL and create a database (or let it auto-create):
   - Database: `ticketing_system`
2. Update MySQL credentials in `backend/src/main/resources/application.properties`.
3. Run the backend:
   - If you have Maven installed:
     - `cd backend`
     - `mvn spring-boot:run`

Admin seed user is created automatically on first run:
- Email: `admin@support.com`
- Password: `Admin@123`
Additional seeded users:
- Admin: `support.lead@support.com` / `Admin@123`
- Admin: `ops.manager@support.com` / `Admin@123`
- Agent: `agent.one@support.com` / `Agent@123`
- Agent: `agent.two@support.com` / `Agent@123`

## Frontend Setup (React)

1. Install dependencies:
   - `cd frontend`
   - `npm.cmd install`
2. Run the client:
   - `npm.cmd run dev`

Frontend runs on `http://localhost:5173` by default.

## API Endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /tickets`
- `GET /tickets`
- `GET /tickets/{id}`
- `PUT /tickets/{id}/status`
- `PUT /tickets/{id}/assign`
- `PUT /tickets/{id}/team`
- `PUT /tickets/{id}/queue`
- `GET /tickets/{id}/comments`
- `POST /tickets/{id}/comments`
- `GET /tickets/{id}/rating`
- `POST /tickets/{id}/rating`
- `PUT /tickets/{id}/tags`
- `GET /tickets/tags`
- `POST /tickets/{id}/attachments`
- `GET /tickets/{id}/attachments`
- `GET /attachments/{id}/download`
- `GET /tickets/export`
- `PUT /tickets/{id}/archive`
- `PUT /tickets/{id}/restore`
- `GET /tickets/analytics`
- `GET /users/admins`
- `GET /users/assignable`
- `GET /users`
- `POST /users`
- `PUT /users/{id}/role`
- `PUT /users/{id}/deactivate`
- `PUT /users/{id}/activate`
- `DELETE /users/me`
- `GET /queues`
- `POST /queues`
- `GET /queues/{id}/members`
- `POST /queues/{id}/members/{userId}`
- `DELETE /queues/{id}/members/{userId}`

## Notes

- JWT tokens are returned from login/register and stored in local storage.
- Status transitions are enforced: `OPEN -> IN_PROGRESS -> RESOLVED`.
- Pagination is supported for ticket lists (default page size 8).
- Email notifications are available when `app.mail.enabled=true` and SMTP settings are configured in `backend/src/main/resources/application.properties`.
- Attachments are stored locally under `app.storage.dir`.
- SLA timers are set per priority using `app.sla.low-hours`, `app.sla.medium-hours`, and `app.sla.high-hours`.
- Tags, search filters, attachments, SLA info, and CSV export are available in the admin UI.
- Users are soft-deactivated (not hard deleted) and tickets are archived/restored.
- Roles include `ROLE_ADMIN`, `ROLE_AGENT` (support staff), and `ROLE_USER` (customers).
