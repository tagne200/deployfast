# Quick Start Guide - 5 minutes

Démarrez l'application en 5 minutes ! ⚡

## Prérequis

- ✅ Java 17 (vérifier: `java -version`)
- ✅ Maven 3.8+ (vérifier: `mvn -version`)
- ✅ **MySQL 8.0+** avec base `deployfast` (phpMyAdmin recommandé)

## Installation rapide

### Étape 1: Cloner
```bash
git clone <repository> && cd deployfast-task-management-api
```

### Étape 2: Compiler
```bash
mvn clean install
# Prend ~2-3 minutes la première fois (téléchargement dépendances)
```

### Étape 3: Lancer
```bash
mvn spring-boot:run
```

**Succès?** Vous devriez voir:
```
...
Started TaskManagementApplication in 3.245 seconds
```

### Étape 4: Tester
```bash
curl http://localhost:8080/api/v1/auth/health
# Réponse: "DeployFast Task Management API is running!"
```

## Premier test API (30 secondes)

### Enregistrer un utilisateur
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demouser",
    "email": "demo@example.com",
    "password": "Demo@1234",
    "firstName": "Demo",
    "lastName": "User"
  }'
```

Copier le `accessToken` de la réponse.

### Créer une tâche
```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "title": "Ma première tâche",
    "description": "C'"'"'est parti!",
    "status": "PENDING",
    "priority": "HIGH",
    "dueDate": "2024-12-31T23:59:59"
  }'
```

### Récupérer les tâches
```bash
curl -X GET "http://localhost:8080/api/v1/tasks?page=0&size=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 🎯 Tester l'API avec Swagger UI

Au lieu d'utiliser `curl`, vous pouvez tester l'API de manière interactive avec Swagger UI :

### Accès à Swagger
1. **Ouvrez votre navigateur**
2. **Allez à** : http://localhost:8080/swagger-ui/index.html

### Premiers pas dans Swagger
1. **Authentification** :
   - Cliquez sur `POST /auth/register`
   - Cliquez sur "Try it out"
   - Remplissez le formulaire avec vos données
   - Cliquez sur "Execute" → Copiez le `accessToken`

2. **Autorisation JWT** :
   - Cliquez sur le bouton "Authorize" (🔒) en haut
   - Entrez : `Bearer YOUR_ACCESS_TOKEN`
   - Cliquez sur "Authorize"

3. **Tester les endpoints** :
   - `POST /tasks` - Créer une tâche
   - `GET /tasks` - Lister vos tâches
   - `PUT /tasks/{id}` - Modifier une tâche
   - `DELETE /tasks/{id}` - Supprimer une tâche

> **Avantages** : Interface graphique, auto-complétion, historique des requêtes, validation des schémas !

## Configuration de l'IDE

### VS Code
```bash
# Extension Java recommandée
# - Extension Pack for Java (Microsoft)
# - Spring Boot Extension Pack (Pivotal Software)

# Fichier launch.json suggéré (F5 pour debug):
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "mainClass": "com.deployfast.taskmanagement.TaskManagementApplication",
      "preLaunchTask": "maven: compile"
    }
  ]
}
```

### IntelliJ IDEA
```bash
# File → Open → Sélectionner le répertoire du projet
# IntelliJ détecte automatiquement le Maven pom.xml
# Run → Run 'TaskManagementApplication'
```

## Commandes essentielles

```bash
# Compiler uniquement (sans test)
mvn clean compile

# Lancer les tests
mvn test

# Lancer 1 test spécifique
mvn test -Dtest=AuthServiceTest#testLoginSuccess

# Voir la couverture
mvn test jacoco:report
# Ouvrir target/site/jacoco/index.html

# Build JAR (produit target/taskmanagement-api-1.0.0.jar)
mvn clean package

# Lancer le JAR
java -jar target/taskmanagement-api-1.0.0.jar

# Supprimer les fichiers compilés
mvn clean
```

## Problèmes courants

### ❌ "Port 8080 already in use"
```bash
# Tuer le processus existant
lsof -ti:8080 | xargs kill -9
# Ou changer le port: mvn spring-boot:run -Dport=8081
```

### ❌ "Maven not found"
```bash
# Installer Maven via Homebrew (macOS)
brew install maven

# Ou télécharger: https://maven.apache.org/
```

### ❌ "Java 17 not found"
```bash
# Vérifier la version: java -version
# Installer Java 17 via:
# - SDKMAN: curl -s "https://get.sdkman.io" | bash
# - Homebrew: brew install openjdk@17
# - Télécharger: https://adoptium.net/
```

### ❌ Les tests échouent
```bash
# Vérifier H2 (en-mémoire) disponible
mvn dependency:list | grep h2

# Nettoyer et relancer
mvn clean test -U
```

### ❌ Les endpoints retournent 401
```bash
# ✓ Vérifier que le token est envoyé
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/v1/tasks

# ✓ Vérifier que le token n'est pas expiré (24h)

# ✓ Les endpoints publics (pas besoin de token)
- POST /auth/register
- POST /auth/login
- GET /auth/health
```

## Structure du code

```
main/java/com/deployfast/taskmanagement/
├── controller/      ← Endpoints REST
├── service/         ← Logique métier
├── repository/      ← Accès DB
├── entity/          ← Modèles
├── dto/             ← Objets transfert
├── security/        ← Auth JWT
├── config/          ← Configuration
└── exception/       ← Gestion erreurs

test/java/...       ← Tests unitaires/intégration
└── resources/yml    ← Config test
```

## Développer une nouvelle fonctionnalité

### 1. Créer l'Entity
```java
@Entity
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Task task;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    // Auto-generated createdAt/updatedAt
}
```

### 2. Créer le Repository
```java
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskId(Long taskId);
}
```

### 3. Créer le Service
```java
@Service
public class CommentService {
    public CommentDto createComment(Long taskId, String content) {
        // Logique métier
    }
}
```

### 4. Créer le Controller
```java
@RestController
@RequestMapping("/tasks/{taskId}/comments")
public class CommentController {
    @PostMapping
    public ResponseEntity<CommentDto> create(...) { }
}
```

### 5. Ajouter les tests
```java
@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Test
    void testCreateCommentSuccess() { }
}
```

## Environment variables (optionnel)

```bash
# Copier .env.example vers .env
cp .env.example .env

# Éditer .env
export JWT_SECRET="votre-clé-secrète"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/mydb"

# Lancer avec les variables
mvn spring-boot:run
```

## Documentation supplémentaire

- 📖 **README.md**: Usage complet de l'API
- 🏗️ **ARCHITECTURE.md**: Design détaillé
- 🤝 **CONTRIBUTING.md**: Guidelines de contribution
- ⚙️ **MAVEN_COMMANDS.md**: Commandes Maven
- 📋 **IMPLEMENTATION_SUMMARY.md**: Résumé du projet

## Support

Pour des questions:
1. Consulter la documentation ci-dessus
2. Vérifier les tests existants (exemples)
3. Ouvrir une issue GitHub

---

**Félicitations!** 🎉 Vous êtes prêt à développer !

Prochaine étape: Créer une branche et développer une nouvelle fonctionnalité 🚀
