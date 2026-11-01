# Documentation technique — DeployFast Task Management API

## 1. Présentation

API REST Spring Boot pour gérer des tâches, avec authentification JWT, validation, pagination et filtres.

## 2. Stack technique

| Domaine | Technologie |
|---|---|
| Langage | Java 17 (cible Maven) |
| Framework | Spring Boot, Spring Security, Spring Data JPA |
| Authentification | JWT (JJWT) |
| Base de données | H2 par défaut ; pilotes MySQL et PostgreSQL inclus |
| Mapping et code | MapStruct, Lombok |
| Documentation d'API | springdoc-openapi |
| Tests | JUnit 5, Mockito, Rest Assured, JaCoCo |

## 3. Architecture

```text
src/main/java/com/deployfast/taskmanagement/
├── config/      Configuration CORS, OpenAPI et sécurité
├── controller/  Routes REST
├── dto/         Objets d'entrée et de sortie
├── entity/      Entités JPA et énumérations
├── exception/   Exceptions et gestion globale des erreurs
├── repository/  Accès aux données
├── security/    Filtre et fournisseur JWT
└── service/     Logique métier
```

## 4. Configuration

Le fichier `src/main/resources/application.yml` utilise par défaut une base H2 en mémoire : les données sont perdues à l'arrêt de l'application. L'API écoute sur le port `8080`, sous le préfixe `/api/v1`.

Variables documentées dans `.env.example` :

| Variable | Rôle |
|---|---|
| `JWT_SECRET` | Secret de signature des tokens |
| `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION` | Durées de validité |
| `SPRING_DATASOURCE_URL` | URL de la base |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | Pilote de la base |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Identifiants de la base |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Stratégie de gestion du schéma |
| `LOGGING_LEVEL_ROOT`, `LOGGING_LEVEL_COM_DEPLOYFAST` | Niveaux de logs |

Utilisez un secret JWT long et propre à chaque environnement.

## 5. Routes de l'API

Toutes les routes sont préfixées par `/api/v1`.

### Authentification

| Méthode | Route | Description |
|---|---|---|
| POST | `/auth/register` | Créer un compte |
| POST | `/auth/login` | Se connecter |
| GET | `/auth/health` | Vérifier l'état du service |

### Tâches

| Méthode | Route | Description |
|---|---|---|
| POST | `/tasks` | Créer une tâche |
| GET | `/tasks` | Lister les tâches |
| GET | `/tasks/{taskId}` | Lire une tâche |
| GET | `/tasks/filter/status` | Filtrer par statut |
| GET | `/tasks/filter/priority` | Filtrer par priorité |
| GET | `/tasks/search` | Rechercher des tâches |
| PUT | `/tasks/{taskId}` | Modifier une tâche |
| DELETE | `/tasks/{taskId}` | Supprimer une tâche |

Le détail des paramètres se trouve dans [API_REFERENCE.md](../API_REFERENCE.md).

## 6. Lancement local

```bash
./mvnw spring-boot:run
```

Sous Windows, utilisez `mvnw.cmd`.

```bash
./mvnw clean verify
```

Cette commande compile le projet et exécute les tests.

## 7. Docker

```bash
docker build -t deployfast-taskmanagement .
docker run -p 8080:8080 -e JWT_SECRET=VOTRE_SECRET deployfast-taskmanagement
```

## 8. Pipeline CI/CD

Le workflow `.github/workflows/ci.yml` se déclenche sur un push ou une pull request vers `main`. Il enchaîne les tests et JaCoCo, OWASP Dependency-Check, la construction de l'image Docker, sa publication sur Docker Hub (push sur `main` uniquement), le scan Trivy, la recherche de secrets et l'analyse SonarCloud.

Il utilise notamment les secrets `DOCKER_USERNAME` et `DOCKER_TOKEN`.

## 9. Points d'attention

- `README.md` mentionne à la fois MySQL et PostgreSQL, alors que la configuration par défaut utilise H2. Référez-vous à la section 4.
- Le Dockerfile utilise une image JDK 21, alors que le projet cible Java 17.

## 10. Autres documents

[ARCHITECTURE.md](../ARCHITECTURE.md), [QUICK_START.md](../QUICK_START.md), [MAVEN_COMMANDS.md](../MAVEN_COMMANDS.md), [OPTIMIZATION_GUIDE.md](../OPTIMIZATION_GUIDE.md), [CONTRIBUTING.md](../CONTRIBUTING.md) et [INDEX.md](../INDEX.md).
