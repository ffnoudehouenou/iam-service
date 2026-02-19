# IAM Service

Microservice d'authentification et d'autorisation utilisant Keycloak et Spring Boot.

## Prérequis
- Docker & Docker Compose
- Java 17
- Maven

## Lancement en local (Docker)
1. Construire et démarrer les services :

```bash
docker-compose up --build -d
```

2. Accéder à Keycloak : http://localhost:8080 (admin/admin)
3. Service API : http://localhost:8081

## Variables d'environnement
- `KEYCLOAK_URL` (default: http://keycloak:8080)
- `KEYCLOAK_REALM` (default: company-realm)
- `KEYCLOAK_CLIENT_ID` (default: iam-client)
- `KEYCLOAK_CLIENT_SECRET`
- `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATASOURCE_URL`

## Endpoints principaux
- `POST /api/v1/auth/login` - Obtenir token
- `POST /api/v1/auth/refresh` - Rafraîchir token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/users` - Gérer utilisateurs (admin)
- `GET /api/v1/admin/roles` - Gérer rôles (admin)

## Build

```
mvn clean package -DskipTests
```

## Notes
- Keycloak admin client est configuré via `KeycloakConfig`.
- Audit logs stockés en base Postgres locale (`iam-db` dans docker-compose).

