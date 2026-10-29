# GameShop 🎮

A full-stack gaming café booking system built with **Java, Spring Boot, MySQL, and React**.

## Features

- Browse available gaming stations and hourly rates
- Book gaming sessions
- Check station availability
- Admin login and dashboard
- Confirm or cancel bookings
- Manage gaming stations
- Assign available stations to confirmed bookings

## Tech Stack

**Frontend**
- React
- Vite
- JavaScript
- CSS

**Backend**
- Java 21
- Spring Boot
- Spring Security
- JDBC / JdbcTemplate

**Database**
- MySQL

## Project Structure

```text
GameShop/
├── gameshop-api/       # Spring Boot backend
├── gameshop-frontend/  # React frontend
└── .gitignore
```

## Run Locally

### 1. Prerequisites

- Java 21
- MySQL
- Node.js and npm

### 2. Configure the database

Create the MySQL database named `gameshop` and configure the required database environment variables:

- `GAMESHOP_DB_URL`
- `GAMESHOP_DB_USERNAME`
- `GAMESHOP_DB_PASSWORD`

Use your existing database schema
