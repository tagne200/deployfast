# Architecture - DeployFast Task Management API

## Document de conception détaillée

### 1. Vue d'ensemble architecturale

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (Web/Mobile)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP/REST + JWT
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway Layer                            │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              Spring Security + JWT Filter                 ││
│  │         (Authentification, Autorisation, CORS)            ││
│  └─────────────────────────────────────────────────────────────┘│
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                    Controller Layer                              │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │   AuthController     │  │     TaskController              │ │
│  │  - register()        │  │  - createTask()                 │ │
│  │  - login()           │  │  - getAllTasks()                │ │
│  │  - health()          │  │  - getTaskById()                │ │
│  │                      │  │  - updateTask()                 │ │
│  │ Validation:          │  │  - deleteTask()                 │ │
│  │  @Valid RequestBody  │  │  - searchTasks()                │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└──────────────────────┬──────────────────────────────────────────┘
                       │ DTO Objects (Input/Output)
┌──────────────────────▼──────────────────────────────────────────┐
│                    Service Layer                                 │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │   AuthService        │  │     TaskService                 │ │
│  │  - register()        │  │  - createTask()                 │ │
│  │  - login()           │  │  - getTaskById()                │ │
│  │  - validatePassword  │  │  - getAllTasks()                │ │
│  │                      │  │  - updateTask()                 │ │
│  │ Business Logic &     │  │  - deleteTask()                 │ │
│  │ Validation Métier    │  │  - searchTasks()                │ │
│  │                      │  │  - getTasksByStatus()           │ │
│  │ Exception Handling   │  │  - getTasksByPriority()         │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└──────────────────────┬──────────────────────────────────────────┘
                       │ Entity Objects
┌──────────────────────▼──────────────────────────────────────────┐
│                 Repository Layer (JPA)                           │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │   UserRepository     │  │     TaskRepository              │ │
│  │  - findByEmail()     │  │  - findByUserId()               │ │
│  │  - findByUsername()  │  │  - findByStatus()               │ │
│  │  - findByEmail       │  │  - findByPriority()             │ │
│  │    OrUsername()      │  │  - searchTasks()                │ │
│  │  - existsByEmail()   │  │  - findOverdueTasks()           │ │
│  │  - existsByUsername()│  │                                 │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
│                                                                   │
│  Hiberate ORM + Spring Data JPA abstraction                      │
└──────────────────────┬──────────────────────────────────────────┘
                       │ SQL Queries (Paramétrées)
┌──────────────────────▼──────────────────────────────────────────┐
│                   Database Layer                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    PostgreSQL / H2                         ││
│  │  ┌──────────────┐  ┌──────────────────┐                   ││
│  │  │    users     │  │     tasks        │                   ││
│  │  ├──────────────┤  ├──────────────────┤                   ││
│  │  │ id (PK)      │  │ id (PK)          │                   ││
│  │  │ username     │  │ title            │                   ││
│  │  │ email        │  │ description      │                   ││
│  │  │ password     │  │ status           │                   ││
│  │  │ role         │  │ priority         │                   ││
│  │  │ created_at   │  │ due_date         │                   ││
│  │  └──────────────┘  │ user_id (FK)     │                   ││
│  │                    │ created_at       │                   ││
│  │                    └──────────────────┘                   ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 2. Flux d'authentification JWT

```
┌────────────────────────────────────────────────────────────────┐
│ CLIENT - ENREGISTREMENT & CONNEXION                             │
└────────────────┬─────────────────────────────────────────────────┘
                 │
     POST /auth/register ou /auth/login (credentials)
                 │
┌────────────────▼─────────────────────────────────────────────────┐
│ AUTHENTICATION SERVICE                                           │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │ 1. Valider les paramètres                                   │ │
│ │ 2. Rechercher l'utilisateur en base                         │ │
│ │ 3. BCrypt.matches(password, hashedPassword)                 │ │
│ │ 4. Si succès → créer l'authentification                     │ │
│ │ 5. Générer les tokens JWT                                   │ │
│ └──────────────────────────────────────────────────────────────┘ │
└────────────────┬─────────────────────────────────────────────────┘
                 │
     ┌───────────┴──────────────────────┐
     │                                  │
     ▼                                  ▼
ACCESS TOKEN (24h)              REFRESH TOKEN (7j)
├─ Header                        ├─ Header
│  alg: HS512                    │  alg: HS512
│                                │
├─ Payload                       ├─ Payload
│  sub: username                 │  sub: username
│  iat: issued_at                │  iat: issued_at
│  exp: expires_in               │  exp: expires_in
│  type: access                  │  type: refresh
│                                │
└─ Signature                     └─ Signature
   signed with JWT_SECRET           signed with JWT_SECRET

┌────────────────────────────────────────────────────────────────┐
│ CLIENT                                                           │
│ Stocke accessToken + refreshToken                               │
│ Envoie accessToken dans Header Authorization: Bearer <token>   │
└────────────────────────────────────────────────────────────────┘
                 │
     Chaque requête vers /tasks endpoints
                 │
┌────────────────▼─────────────────────────────────────────────────┐
│ JWT AUTHENTICATION FILTER                                        │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │ 1. Extraire token du header Authorization                   │ │
│ │ 2. Valider la signature JWT                                 │ │
│ │ 3. Verifier l'expiration                                    │ │
│ │ 4. Extraire le username du payload                          │ │
│ │ 5. Charger les authorities (rôles)                          │ │
│ │ 6. Créer Authentication et le setter dans SecurityContext   │ │
│ └──────────────────────────────────────────────────────────────┘ │
└────────────────┬─────────────────────────────────────────────────┘
                 │
         ┌───────┴────────┐
         │                │
    Valide ✓          Invalide ✗
         │                │
         ▼                ▼
   Accès autorisé    401 Unauthorized
   Endpoint appelé   + ErrorResponse JSON
```

### 3. Modèle de données

```sql
/* Table Users */
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  /* BCrypt Hash */
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20) NOT NULL,       /* ENUM: ADMIN, USER */
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

/* Table Tasks */
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,           /* ENUM: PENDING, IN_PROGRESS, COMPLETED, CANCELLED */
    priority VARCHAR(20) NOT NULL,         /* ENUM: LOW, MEDIUM, HIGH, CRITICAL */
    due_date TIMESTAMP,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
```

### 4. Validation des données

```
COUCHE 1: VALIDATION PAR ANNOTATIONS (@Valid)
├─ LoginRequest
│  ├─ @NotBlank emailOrUsername (3-100 chars)
│  └─ @NotBlank @Size(8-128) password
├─ RegisterRequest
│  ├─ @NotBlank @Size(3-50) @Pattern username
│  ├─ @Email email
│  ├─ @NotBlank @Size(8-128) @Pattern password (security rules)
│  ├─ @NotBlank @Size(1-50) firstName
│  └─ @NotBlank @Size(1-50) lastName
└─ CreateTaskRequest
   ├─ @NotBlank @Size(3-200) title
   ├─ @Size(0-2000) description
   ├─ @NotNull status (enum validation)
   ├─ @NotNull priority (enum validation)
   └─ @Future dueDate

COUCHE 2: VALIDATION MÉTIER (Service)
├─ AuthService
│  ├─ validatePasswordStrength()
│  │  ├─ ≥8 caractères
│  │  ├─ ≥1 majuscule [A-Z]
│  │  ├─ ≥1 minuscule [a-z]
│  │  ├─ ≥1 chiffre [0-9]
│  │  └─ ≥1 caractère spécial [@$!%*?&]
│  ├─ Vérifier email/username unique
│  └─ Vérifier identifiants valides
├─ TaskService
│  ├─ Valider enum values (status, priority)
│  ├─ Vérifier date limite future
│  └─ Vérifier que la tâche appartient à l'utilisateur

COUCHE 3: VALIDATION BASE DE DONNÉES (Constraints)
├─ UNIQUE(email), UNIQUE(username)
├─ CHECK ON role IN ('ADMIN', 'USER')
├─ CHECK ON status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
├─ CHECK ON priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
└─ FOREIGN KEY user_id REFERENCES users(id) ON DELETE CASCADE

RÉPONSES D'ERREUR
┌─ 400 Bad Request (Validation)
│  ├─ Field errors (annotations)
│  └─ Business validation (métier)
├─ 401 Unauthorized
│  ├─ Token invalide/expiré
│  └─ Authentification échouée
├─ 409 Conflict
│  ├─ Email/Username existant
│  └─ Ressource duplicate
└─ 404 Not Found
   └─ Tâche/Utilisateur n'existe pas
```

### 5. Sécurité

```
AUTHENTIFICATION
├─ JWT (JSON Web Tokens)
│  ├─ Algorithm: HS512 (HMAC SHA-512)
│  ├─ Secret: 64+ chars (configurable)
│  ├─ Access Token: 24h
│  └─ Refresh Token: 7j
│
└─ Spring Security
   ├─ UserDetailsService custom → charger User de DB
   └─ JwtAuthenticationFilter → extraire et valider JWT

AUTORISATION
├─ Role-Based Access Control (RBAC)
│  ├─ Anonymous: /auth/register, /auth/login, /auth/health
│  ├─ USER: /tasks/*, /users/*
│  └─ ADMIN: /admin/*
│
└─ Owner Validation: Vérifier que l'utilisateur = propriétaire de la tâche

PROTECTIONS CONTRE ATTAQUES
├─ CSRF: Désactivée (API stateless JWT)
├─ XSS: Content-Security-Policy header
├─ SQL Injection: Requêtes paramétrées JPA (@Query avec :param)
├─ Brute Force: Time-based BCrypt (force 12)
├─ Token Hijacking: HTTPS obligatoire (production)
└─ CORS: Whitelist des origins

MOT DE PASSE
├─ Hachage: BCrypt (force score 12)
├─ Jamais stocké en clair
├─ Validation: 8+ chars, uppercase, lowercase, digit, special char
└─ Rejet si faible

HEADERS SÉCURITÉ
├─ Authorization: Bearer <token>
├─ Content-Security-Policy: default-src 'self'
├─ X-Content-Type-Options: nosniff
├─ X-Frame-Options: SAMEORIGIN
└─ Strict-Transport-Security (production)
```

### 6. Gestion des erreurs

```
ARCHITECTURE D'ERREUR
┌─────────────────────────────────────────────────────────────┐
│ GlobalExceptionHandler (@RestControllerAdvice)              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  @ExceptionHandler(ResourceNotFoundException.class)         │
│  ├─ Status: 404 NOT_FOUND                                  │
│  └─ Body: ErrorResponse                                     │
│                                                             │
│  @ExceptionHandler(ResourceAlreadyExistsException.class)    │
│  ├─ Status: 409 CONFLICT                                   │
│  └─ Body: ErrorResponse                                     │
│                                                             │
│  @ExceptionHandler(BusinessValidationException.class)       │
│  ├─ Status: 400 BAD_REQUEST                                │
│  └─ Body: ErrorResponse                                     │
│                                                             │
│  @ExceptionHandler(MethodArgumentNotValidException.class)   │
│  ├─ Status: 400 BAD_REQUEST                                │
│  └─ Body: ValidationErrorResponse (field errors)           │
│                                                             │
│  @ExceptionHandler(BadCredentialsException.class)           │
│  ├─ Status: 401 UNAUTHORIZED                               │
│  └─ Body: ErrorResponse                                     │
│                                                             │
│  @ExceptionHandler(Exception.class)  /* catch-all */       │
│  ├─ Status: 500 INTERNAL_SERVER_ERROR                      │
│  └─ Body: ErrorResponse (message generic)                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

FORMAT RÉPONSE D'ERREUR
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Les données soumises ne sont pas valides",
  "path": "/api/v1/tasks",
  "errorCode": "VALIDATION_FAILED",
  "fieldErrors": {
    "title": "Le titre est requis",
    "priority": "Priorité invalide"
  }
}
```

### 7. Patterns de conception utilisés

```
PATTERNS ARCHITECTURAUX
├─ MVC (Model-View-Controller)
│  ├─ Model: Entity (JPA), DTO
│  ├─ View: JSON (REST)
│  └─ Controller: @RestController
│
├─ Service Layer Pattern
│  └─ Séparer logique métier des contrôleurs
│
├─ Repository Pattern (Data Access Object)
│  └─ Abstraire l'accès aux données (JPA)
│
└─ Dependency Injection
   └─ Spring IoC container

PATTERNS DE PROGRAMMATION
├─ Builder Pattern (via Lombok @Builder)
│  └─ Créer objets complexes (User, Task, etc.)
│
├─ Singleton Pattern
│  ├─ Services (@Service)
│  ├─ Repositories (@Repository)
│  └─ Configuration (@Configuration)
│
├─ Strategy Pattern
│  └─ Différentes stratégies de validation
│
└─ Template Method Pattern
   └─ GlobalExceptionHandler pour gestion d'erreurs
```

### 8. Cycle de vie d'une requête

```
1. CLIENT ENVOIE REQUÊTE
   POST /api/v1/tasks
   Header: Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   Body: { "title": "...", "status": "PENDING", ... }
                          │
                          ▼
2. SERVLET CONTAINER & SPRING MVC
   ├─ Reçoit la requête HTTP
   └─ Matching route → TaskController.createTask()
                          │
                          ▼
3. SECURITY FILTER CHAIN
   ├─ JwtAuthenticationFilter
   │  ├─ Extrait token du header
   │  ├─ Valide signature JWT
   │  ├─ Charge User de la DB via UserDetailsService
   │  └─ Crée Authentication (SpringSecurity)
   │
   ├─ AuthorizationFilter
   │  ├─ Vérifies @PreAuthorize("isAuthenticated()")
   │  └─ Autorise si authentifié
   └─ → Passe au contrôleur
                          │
                          ▼
4. CONTROLLER (TaskController)
   ├─ Reçoit @Valid CreateTaskRequest
   ├─ Spring valide avec annotations (@NotBlank, @Size, etc.)
   ├─ Si validatio échoue → MethodArgumentNotValidException
   │  → GlobalExceptionHandler → 400 Bad Request
   │
   ├─ Si validation OK → appelle TaskService.createTask()
   └─ → Attend la réponse
                          │
                          ▼
5. SERVICE (TaskService)
   ├─ Récupère utilisateur courant via SecurityContext
   ├─ Valide la logique métier (dates, statuts, etc.)
   ├─ Si erreur → lance BusinessValidationException
   │  → GlobalExceptionHandler → 400 Bad Request
   │
   ├─ Si OK
   │  ├─ Crée Entity Task
   │  ├─ Appelle taskRepository.save(task)
   │  └─ Reçoit Task sauvegardée (avec ID généré)
   │
   └─ Convertit Entity Task en DTO TaskDto
      → Retourne au controller
                          │
                          ▼
6. DATABASE (JPA)
   ├─ Convert Entity Task en SQL INSERT
   ├─ Exécute prepared statement (protection SQL injection)
   ├─ Obtient ID généré (auto_increment)
   ├─ Valide contraintes DB (UNIQUE, CHECK, FK)
   └─ Retourne Entity avec ID

7. CONTROLLER RETOURNE LA RÉPONSE
   ├─ Status: 201 CREATED
   ├─ Body: TaskDto (serializé en JSON)
   └─ Location Header: /api/v1/tasks/1

8. CLIENT REÇOIT RÉPONSE
   {
     "id": 1,
     "title": "...",
     "status": "PENDING",
     ...
   }
```

### 9. Directives de scalabilité

```
ACTUEL (Single Instance)
├─ In-memory: H2 (dev)
├─ Single server: Spring Boot
├─ Auth: JWT (stateless)
└─ Performance: ✓ OK pour < 1000 utilisateurs

SCALING HORIZONTAL (Futur)
├─ Load Balancer (Nginx, AWS ALB)
├─ Multiple instances (Spring Boot)
├─ Distributed Cache (Redis)
│  └─ Cacher tokens/sessions
├─ PostgreSQL (replicated)
└─ Performance: ✓ OK pour > 10,000 utilisateurs

OPTIMISATIONS POSSIBLES
├─ Caching
│  ├─ @Cacheable sur getTasks()
│  ├─ Redis pour tokens JWT
│  └─ Lazy loading des relations
│
├─ Database
│  ├─ Indexes sur (user_id, status, priority)
│  ├─ Batch operations pour bulk inserts
│  └─ Connection pooling (HikariCP)
│
├─ Monitoring
│  ├─ Actuator endpoints
│  ├─ Structured logging (JSON logs)
│  └─ ELK stack (Elasticsearch, Logstash, Kibana)
│
└─ Sécurité
   ├─ Rate limiting (par IP, par user)
   ├─ WAF (Web Application Firewall)
   └─ HTTPS + TLS 1.3
```

## Conclusion

Cette architecture suit les bonnes pratiques Spring Boot modernes avec une sécurité robuste, une validation en multi-couches et une séparation claire des responsabilités pour faciliter la maintenance et l'évolution du système.
