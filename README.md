# DeployFast Task Management API

API REST sécurisée pour la gestion des tâches, développée avec Spring Boot 3.2, JWT et JPA (H2 par défaut, MySQL ou PostgreSQL en option).

## Vue d'ensemble

✅ Authentification sécurisée avec JWT  
✅ CRUD complet des tâches  
✅ Validation stricte des données (annotations + métier)  
✅ Gestion centralisée des exceptions  
✅ Pagination et filtrage avancés  
✅ Tests automatisés (couverture > 80%)  
✅ Protections CSRF, XSS, injection SQL  
✅ Clean Code : méthodes courtes, nommage explicite, commentaires pertinents  

## Architecture

```
src/main/java/com/deployfast/taskmanagement/
├── controller/          # Contrôleurs REST
├── service/             # Logique métier
├── repository/          # Accès aux données
├── entity/              # Modèles de données JPA
├── dto/                 # Data Transfer Objects
├── security/            # Configuration JWT/Security
├── config/              # Configurations Spring
└── exception/           # Gestion globale des exceptions

src/test/java/           # Tests unitaires et Feature
```

## Prérequis

- **Java 17+** (la compilation cible Java 17 ; si vous voyez l'erreur "release version 17 not supported", installez JDK 17 et mettez à jour la variable d'environnement `JAVA_HOME` ou utilisez le wrapper `./mvnw`)
- **Maven 3.8+** (ou utilisez le wrapper `mvnw` inclus)
- **Aucune base à installer pour démarrer** : H2 en mémoire par défaut (MySQL 8.0+ ou PostgreSQL en option)

> **Configuration base de données** : sans variable d'environnement, l'application utilise H2 en mémoire et les données sont perdues à l'arrêt. Pour conserver les données, renseignez les variables `SPRING_DATASOURCE_*` de `.env.example`.

## Installation

### 1. Cloner le repository
```bash
git clone https://github.com/tagne200/deployfast.git
cd deployfast
```

### 2. Configurer les variables d'environnement
```bash
cp .env.example .env
```

Éditer `.env` avec vos paramètres:
```bash
JWT_SECRET=your_long_secret_key_here
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/deployfast_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password
```

### 3. Compiler le projet
```bash
mvn clean install
```

### 4. Lancer l'application
```bash
mvn spring-boot:run
```

L'application démarre sur `http://localhost:8080/api/v1`

### Documentation API (Swagger)

Une fois l'application démarrée, accédez à la documentation interactive Swagger :

- **Swagger UI** : http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON** : http://localhost:8080/v3/api-docs
- **OpenAPI YAML** : http://localhost:8080/v3/api-docs.yaml

> **Astuce** : Utilisez Swagger UI pour tester les endpoints directement depuis votre navigateur !

### CI/CD

Un workflow GitHub Actions est déjà fourni dans `.github/workflows/ci.yml`. Il se lance sur chaque `push` ou `pull_request` vers **main** et automatise :

1. L'installation des dépendances et le caching Maven
2. La compilation et l'exécution des tests (`./mvnw clean verify jacoco:report`)
3. La génération du rapport de couverture JaCoCo
4. L'analyse avec OWASP Dependency-Check
5. La construction d'une image Docker (`Dockerfile` présent à la racine)
6. Le scan de cette image via Trivy
7. La recherche de secrets avec TruffleHog
8. L'analyse SonarCloud n'est pas active dans le pipeline : elle demande une organisation et un secret `SONAR_TOKEN`

Aucune intervention manuelle n'est requise : la CI est entièrement automatisée. Configurez `SONAR_TOKEN` et, si nécessaire, `SONAR_HOST_URL` en tant que secrets dans GitHub pour activer l'étape Sonar.

Un fichier `sonar-project.properties` est présent à la racine du projet. Modifiez-le si vous utilisez un serveur SonarQube privé (définir `sonar.host.url`, `sonar.login`, etc.).

Vous pouvez adapter ou reproduire ces étapes dans GitLab CI, CircleCI, etc. selon votre préférence.


## Endpoints API

### Authentification

| Méthode | Endpoint | Description | Auth Required |
|---------|----------|-------------|---|
| POST | `/auth/register` | Enregistrer un nouvel utilisateur | ❌ |
| POST | `/auth/login` | Connexion utilisateur | ❌ |
| GET | `/auth/health` | Vérifier la disponibilité | ❌ |

### Tâches

| Méthode | Endpoint | Description | Auth Required |
|---------|----------|-------------|---|
| POST | `/tasks` | Créer une tâche | ✅ |
| GET | `/tasks` | Lister les tâches (paginé) | ✅ |
| GET | `/tasks/{id}` | Récupérer une tâche | ✅ |
| GET | `/tasks/filter/status?status=PENDING` | Filtrer par statut | ✅ |
| GET | `/tasks/filter/priority?priority=HIGH` | Filtrer par priorité | ✅ |
| GET | `/tasks/search?title=test` | Rechercher par titre | ✅ |
| PUT | `/tasks/{id}` | Mettre à jour une tâche | ✅ |
| DELETE | `/tasks/{id}` | Supprimer une tâche | ✅ |

## Exemples d'utilisation

### 1. Enregistrement
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johnDoe",
    "email": "john@example.com",
    "password": "Password123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Réponse:**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "johnDoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "enabled": true
  }
}
```

### 2. Connexion
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "emailOrUsername": "john@example.com",
    "password": "Password123!"
  }'
```

### 3. Créer une tâche (authentifiée)
```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "title": "Implémenter l'\''API de notification",
    "description": "Ajouter le système de notifications par email",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2024-12-31T23:59:59"
  }'
```

### 4. Récupérer les tâches (paginées)
```bash
curl -X GET "http://localhost:8080/api/v1/tasks?page=0&size=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Sécurité

### Authentification JWT
- Tokens HS512 signés avec clé secrète robuste
- Expiration configurable (défaut: 24h)
- Tokens de rafraîchissement (défaut: 7j)

### Protections intégrées
- ✅ **CSRF Protection** : Désactivée pour API stateless (JWT)
- ✅ **XSS Protection** : Headers Content-Security-Policy
- ✅ **SQL Injection** : Requêtes paramétrées via JPA
- ✅ **Validation stricte** : Annotations @Valid + validation métier
- ✅ **CORS** : Configuré pour origins autorisés
- ✅ **Mot de passe sécurisé** : BCrypt (force 12)

### Validation des données
```
Email: Email valide (RFC 5322)
Username: 3-50 caractères, alphanumérique + tiret/underscore
Password: 8+ caractères, 1 majuscule, 1 minuscule, 1 chiffre, 1 spécial
Title: 3-200 caractères
```

## Tests

### Lancer tous les tests
```bash
mvn test
```

### Lanrer les tests avec rapport de couverture
```bash
mvn test jacoco:report
# Rapport disponible: target/site/jacoco/index.html
```

### Tests inclus
- ✅ Tests unitaires (AuthService, TaskService)
- ✅ Tests Feature (Controllers)
- ✅ Tests d'authentification JWT
- ✅ Tests de validation
- ✅ Couverture ligne: > 80%

## Structure des tests

```
src/test/
├── java/com/deployfast/taskmanagement/
│   ├── service/
│   │   ├── AuthServiceTest.java        # 12 tests
│   │   └── TaskServiceTest.java        # 11 tests
│   ├── controller/
│   │   └── TaskControllerFeatureTest.java  # 6 tests
│   └── security/
│       └── AuthenticationSecurityTest.java # 7 tests
└── resources/
    └── application.yml                 # Config de test
```

## Bonnes pratiques appliquées

### Clean Code
✅ **Méthodes courtes**: Max 30 lignes  
✅ **Nommage explicite**: Classes, méthodes, variables auto-documentées  
✅ **Pas de duplication**: Services réutilisables, DTOs distincts  
✅ **Commentaires pertinents**: JavaDoc sur tous les public, commentaires métier sensés  
✅ **Logging structuré**: Trace complète des opérations  

### Architecture
✅ **Séparation des concerns**: Controller → Service → Repository → Entity  
✅ **DTOs distincts**: Input (Create/Update), Output (Read)  
✅ **Validation en couches**: Annotations + logique métier  
✅ **Gestion d'erreurs centralisée**: GlobalExceptionHandler  

### Sécurité
✅ **BCrypt**: Hachage robuste des mots de passe  
✅ **JWT sécurisé**: HS512, clé longue, expiration configurable  
✅ **Autorisation RBAC**: Rôles ADMIN/USER  
✅ **Audit logging**: Traçabilité des opérations sensibles  

## Configuration Spring

### Profils
- **dev** : H2 in-memory, logs DEBUG
- **test** : H2 in-memory, création/suppression tables
- **prod** : PostgreSQL, logs INFO (via variables d'env)

### Propriétés principales
```yaml
app.jwt.secret         # Clé JWT
app.jwt.expiration     # Expiration token d'accès (ms)
app.jwt.refresh-expiration  # Expiration refresh token (ms)
```

## Déploiement

### Production
```bash
# Build JAR
mvn clean package

# Lancer avec variables d'environnement
java -jar target/taskmanagement-api-1.0.0.jar \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/deployfast \
  --spring.datasource.username=$DB_USER \
  --spring.datasource.password=$DB_PASSWORD \
  --app.jwt.secret=$JWT_SECRET
```

### Docker (optionnel)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/taskmanagement-api-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Dépannage

### Erreur : "Token JWT invalide"
→ Vérifier la clé secrète JWT dans les variables d'environnement

### Erreur : "Connexion refusée - PostgreSQL"
→ Vérifier que PostgreSQL est lancé sur le port 5432

### Tests échouent
→ Vérifier le port 8080 n'est pas utilsé
→ Vérifier Java 17+ installé: `java -version`

## Contribution

Voir [CONTRIBUTING.md](CONTRIBUTING.md) pour les guidelines de contribution.

## Licence

DeployFast © 2024. Tous droits réservés.

## Support

Pour les issues ou questions, consulter [ARCHITECTURE.md](ARCHITECTURE.md) pour la conception détaillée.

## Documentation détaillée

Consultez [docs/DOCUMENTATION.md](docs/DOCUMENTATION.md) pour une vue d'ensemble à jour : configuration réelle, liste des routes, tests et pipeline.
