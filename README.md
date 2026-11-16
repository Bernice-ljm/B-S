# Enterprise Approval System

This repository contains the enterprise approval system for the BS Software Design course.

## Prerequisites

- Java 21
- Node.js 22 or later
- npm 10 or later

Database and container prerequisites are introduced in stage 1B.

## Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The public health endpoint is `http://localhost:8080/actuator/health`. Other backend routes are denied until authentication is implemented in a later stage.

Run the backend checks with:

```powershell
cd backend
.\mvnw.cmd verify
```

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

The Vite development server proxies `/actuator` to the backend. Build the production bundle with `npm run build`.

Stage 1A provides only the runnable project skeleton. It does not implement authentication, applications, workflow, or other business features.
