# Whydah-UserAdminWebApp

## Purpose
Administration web UI for managing Whydah users, their role mappings, application assignments, and organization memberships. Provides a browser-based interface for administrators to perform user management operations.

## Tech Stack
- Language: Java 21
- Framework: Jersey 1.x, Spring 5.x, Jetty 9.x
- Build: Maven
- Key dependencies: Whydah-Admin-SDK, Jersey, Spring, Jetty

## Architecture
Standalone web application with embedded Jetty server. Connects to UserAdminService for backend operations and optionally requires SSOLoginWebApp and SecurityTokenService for administrator authentication. Users need a UserAdmin role defined in UserIdentityBackend to access the administration interface.

## Key Entry Points
- Web UI for user administration
- `start_service.sh` / `update_service.sh` - Service lifecycle scripts
- `pom.xml` - Maven coordinates: `net.whydah.identity:UserAdminWebApp`

## Development
```bash
# Build
mvn clean install

# Run
java -jar target/UserAdminWebApp-*.jar
```

## Domain Context
Whydah IAM administration frontend. The primary UI for system administrators to manage users, roles, applications, and organizations within a Whydah deployment.
