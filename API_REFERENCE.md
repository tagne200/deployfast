# Référence des dépendances et endpoints API

## Dépendances Maven

### Spring Boot Framework
| Dépendance | Version | Rôle |
|-----------|---------|------|
| spring-boot-starter-web | 3.2.0 | API REST, serveur Tomcat intégré |
| spring-boot-starter-security | 3.2.0 | Security, authentication provider |
| spring-boot-starter-data-jpa | 3.2.0 | ORM Hibernate, repositories |
| spring-boot-starter-validation | 3.2.0 | Validation annotations (@Valid, @NotNull, etc.) |
| spring-security-test | 3.2.0 | Tests authentification |

### JWT & Authentication
| Dépendance | Version | Rôle |
|-----------|---------|------|
| jjwt-api | 0.12.3 | JWT token generation API |
| jjwt-impl | 0.12.3 | JWT implementation |
| jjwt-jackson | 0.12.3 | Jackson serialization for JWT |

### Database
| Dépendance | Version | Rôle |
|-----------|---------|------|
| h2 | Latest | In-memory DB (développement) |
| postgresql | 42.7.0 | Driver PostgreSQL (production) |

### Utilities
| Dépendance | Version | Rôle |
|-----------|---------|------|
| lombok | 1.18.30 | Boilerplate reduction (@Getter, @Setter, @Builder) |
| mapstruct | 1.5.5.Final | Entity ↔ DTO mapping (optionnel) |
| commons-lang3 | 3.13+ | Utility methods (StringUtils, etc.) |

### Testing
| Dépendance | Version | Rôle |
|-----------|---------|------|
| spring-boot-starter-test | 3.2.0 | JUnit5, Mockito, AssertJ |
| junit-jupiter-api | Latest | JUnit 5 core API |
| mockito-core | Latest | Mocking framework |
| mockito-junit-jupiter | Latest | Mockito JUnit 5 integration |
| rest-assured | Latest | API testing DSL |

### Code Coverage & Quality
| Dépendance | Version | Rôle |
|-----------|---------|------|
| jacoco-maven-plugin | 0.8.10 | Code coverage (couverture > 80%) |

### Logging
| Dépendance | Version | Rôle |
|-----------|---------|------|
| spring-boot-starter-logging | 3.2.0 | SLF4J + Logback |

---

## API REST Endpoints

### 🔐 Authentification (Public)

#### 1. Enregistrer un nouvel utilisateur
```
POST /api/v1/auth/register
Content-Type: application/json

Request Body:
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "enabled": true
  }
}

Errors:
- 400 Bad Request: Validation failed
- 409 Conflict: Email/username already exists
```

#### 2. Connexion utilisateur
```
POST /api/v1/auth/login
Content-Type: application/json

Request Body:
{
  "emailOrUsername": "john@example.com",
  "password": "SecurePass123!"
}

Response: 200 OK
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": { ... }
}

Errors:
- 400 Bad Request: Invalid input
- 401 Unauthorized: Invalid credentials
```

#### 3. Vérifier la disponibilité
```
GET /api/v1/auth/health

Response: 200 OK
"DeployFast Task Management API is running!"
```

---

### ✅ Gestion des tâches (Authentifiées)

Tous les endpoints `/api/v1/tasks/**` nécessitent:
```
Authorization: Bearer <accessToken>
```

#### 4. Créer une tâche
```
POST /api/v1/tasks
Content-Type: application/json
Authorization: Bearer <accessToken>

Request Body:
{
  "title": "Développer API REST",
  "description": "Implémenter CRUD pour tâches",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2024-12-31T23:59:59"
}

Response: 201 Created
{
  "id": 1,
  "title": "Développer API REST",
  "description": "Implémenter CRUD pour tâches",
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2024-12-31T23:59:59",
  "userId": 1,
  "createdAt": "2024-01-15T10:30:45.123Z",
  "updatedAt": "2024-01-15T10:30:45.123Z",
  "completedAt": null
}

Errors:
- 400 Bad Request: Validation failed
- 401 Unauthorized: Missing/invalid token
```

#### 5. Lister toutes les tâches (paginées)
```
GET /api/v1/tasks?page=0&size=10
Authorization: Bearer <accessToken>

Query Parameters:
- page: int (défaut: 0) - Numéro de page (0-indexed)
- size: int (défaut: 10) - Nombre d'items par page

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "title": "Développer API REST",
      "status": "PENDING",
      "priority": "HIGH",
      ...
    },
    ...
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}

Errors:
- 401 Unauthorized
```

#### 6. Récupérer une tâche par ID
```
GET /api/v1/tasks/{taskId}
Authorization: Bearer <accessToken>

Path Parameters:
- taskId: long - ID de la tâche

Response: 200 OK
{
  "id": 1,
  "title": "Développer API REST",
  ...
}

Errors:
- 401 Unauthorized
- 404 Not Found: Task not found
```

#### 7. Filtrer par statut
```
GET /api/v1/tasks/filter/status?status=PENDING&page=0&size=10
Authorization: Bearer <accessToken>

Query Parameters:
- status: string (PENDING|IN_PROGRESS|COMPLETED|CANCELLED)
- page: int (défaut: 0)
- size: int (défaut: 10)

Response: 200 OK
{
  "content": [
    { "id": 1, "status": "PENDING", ... },
    ...
  ],
  "pageNumber": 0,
  ...
}

Errors:
- 400 Bad Request: Invalid status
- 401 Unauthorized
```

#### 8. Filtrer par priorité
```
GET /api/v1/tasks/filter/priority?priority=HIGH&page=0&size=10
Authorization: Bearer <accessToken>

Query Parameters:
- priority: string (LOW|MEDIUM|HIGH|CRITICAL)
- page: int
- size: int

Response: 200 OK
{ ... same as status filter ... }

Errors:
- 400 Bad Request: Invalid priority
- 401 Unauthorized
```

#### 9. Rechercher par titre
```
GET /api/v1/tasks/search?title=API&page=0&size=10
Authorization: Bearer <accessToken>

Query Parameters:
- title: string - Titre (partial match, insensitive)
- page: int
- size: int

Response: 200 OK
{
  "content": [
    { "id": 1, "title": "Développer API REST", ... },
    { "id": 3, "title": "Tester API", ... }
  ],
  ...
}

Errors:
- 401 Unauthorized
```

#### 10. Mettre à jour une tâche
```
PUT /api/v1/tasks/{taskId}
Content-Type: application/json
Authorization: Bearer <accessToken>

Path Parameters:
- taskId: long

Request Body (tous les champs optionnels):
{
  "title": "Développer API REST v2",
  "description": "Nouvelle description",
  "status": "IN_PROGRESS",
  "priority": "CRITICAL",
  "dueDate": "2024-12-25T23:59:59"
}

Response: 200 OK
{
  "id": 1,
  "title": "Développer API REST v2",
  ...
}

Errors:
- 400 Bad Request: Invalid data/status
- 401 Unauthorized
- 404 Not Found
```

#### 11. Supprimer une tâche
```
DELETE /api/v1/tasks/{taskId}
Authorization: Bearer <accessToken>

Path Parameters:
- taskId: long

Response: 204 No Content

Errors:
- 401 Unauthorized
- 404 Not Found
```

---

## Valeurs d'énumération

### TaskStatus
```
PENDING       - À faire
IN_PROGRESS   - En cours
COMPLETED     - Terminée
CANCELLED     - Annulée
```

### TaskPriority
```
LOW           - Basse (niveau 1)
MEDIUM        - Moyenne (niveau 2)
HIGH          - Haute (niveau 3)
CRITICAL      - Critique (niveau 4)
```

### UserRole
```
ADMIN         - Accès complet
USER          - Accès limité aux tâches
```

---

## Codes d'erreur HTTP

| Code | Signification | Exemple |
|------|---------------|---------|
| 200 | OK | Requête réussie |
| 201 | Created | Ressource créée |
| 204 | No Content | Suppression réussie |
| 400 | Bad Request | Validation échouée |
| 401 | Unauthorized | Token invalide/absent |
| 409 | Conflict | Ressource existante (email/username) |
| 404 | Not Found | Ressource inexistante |
| 500 | Internal Server Error | Erreur serveur |

---

## Codes d'erreur métier

### Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "status": 400,
  "error": "Erreur de validation",
  "message": "Les données soumises ne sont pas valides",
  "path": "/api/v1/tasks",
  "errorCode": "VALIDATION_FAILED",
  "fieldErrors": {
    "title": "Le titre est requis",
    "priority": "Priorité invalide"
  }
}
```

### Codes métier
```
RESOURCE_NOT_FOUND          - 404: Ressource inexistante
RESOURCE_ALREADY_EXISTS     - 409: Ressource déjà existante
VALIDATION_FAILED           - 400: Validation échouée
AUTHENTICATION_REQUIRED     - 401: Authentification nécessaire
INVALID_CREDENTIALS         - 401: Identifiants incorrects
WEAK_PASSWORD               - 400: Mot de passe faible
INTERNAL_SERVER_ERROR       - 500: Erreur serveur
```

---

## Authentification JWT

### Token Format
```
Authorization: Bearer <accessToken>

Exemple:
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTcwNTMxODY0NSwiZXhwIjoxNzA1NDA1MDQ1fQ.signature...
```

### Token Payload
```json
{
  "alg": "HS512",  // Header
  "sub": "john_doe",
  "iat": 1705318645,    // issued at (Unix timestamp)
  "exp": 1705405045,    // expiration (Unix timestamp)
  "type": "access"      // Token type
}
```

### Expiration
- Access Token: 24 heures
- Refresh Token: 7 jours

---

## Exemples complets (cURL)

### Flux complet utilisateur

```bash
# 1. Enregistrer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "Secure@123",
    "firstName": "Alice",
    "lastName": "Wonder"
  }' | jq '.accessToken' -r > token.txt

# 2. Sauvegarder le token
TOKEN=$(cat token.txt)

# 3. Créer une tâche
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Apprendre Spring Boot",
    "description": "Compléter le cours",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    "dueDate": "2024-12-31T23:59:59"
  }'

# 4. Lister les tâches
curl "http://localhost:8080/api/v1/tasks?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 5. Rechercher
curl "http://localhost:8080/api/v1/tasks/search?title=Spring" \
  -H "Authorization: Bearer $TOKEN"

# 6. Mettre à jour
curl -X PUT http://localhost:8080/api/v1/tasks/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED"
  }'

# 7. Supprimer
curl -X DELETE http://localhost:8080/api/v1/tasks/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

**Dernière mise à jour**: 2024  
**Version API**: 1.0.0
