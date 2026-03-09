# Guide de contribution

Bienvenue ! Ce document explique les standards et bonnes pratiques pour contribuer au projet DeployFast Task Management API.

## Code Style

### Conventions de nommage
```java
// ✓ Classes et Interfaces (PascalCase)
public class TaskService { }
public interface UserRepository { }

// ✓ Méthodes et Variables (camelCase)
public void createTask() { }
private String taskTitle;

// ✓ Constantes (UPPER_SNAKE_CASE)
private static final long TIMEOUT_MS = 5000L;
```

### Structure des méthodes
```java
// ✓ BON: Court et lisible
public TaskDto createTask(CreateTaskRequest request) {
    User currentUser = getCurrentUser();
    validateInput(request);
    Task task = new Task(request);
    return taskRepository.save(task);
}

// ✗ MAUVAIS: Trop long, logique complexe
public TaskDto createTask(CreateTaskRequest request) {
    // 50 lignes de logique...
}
```

### Commentaires
```java
// ✓ BON: Explique le POURQUOI
// Vérifier que la date est future pour éviter les tâches rétroactives
if (request.getDueDate().isBefore(LocalDateTime.now())) {
    throw new BusinessValidationException("Date limite invalide");
}

// ✗ MAUVAIS: Explique le QUOI (code auto-explicite)
// Vérifier si dueDate < now
if (request.getDueDate().isBefore(LocalDateTime.now())) {
```

## Bonnes pratiques Java

### Utilisation de Lombok
```java
// ✓ Utiliser @Builder, @Data, @NoArgsConstructor, @AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task { }

// ✗ Pas de getters/setters manuels, utiliser Lombok
```

### Null Safety
```java
// ✓ BON: Utiliser Optional
Optional<User> user = userRepository.findByUsername(username);
user.orElseThrow(() -> new ResourceNotFoundException(...));

// ✗ MAUVAIS: Null checks classiques
User user = userRepository.findByUsername(username);
if (user == null) { }
```

### Logging
```java
// ✓ BON: Logger les opérations importantes
@Slf4j
public class TaskService {
    public void deleteTask(Long taskId) {
        log.info("Suppression de la tâche ID: {}", taskId);
        taskRepository.deleteById(taskId);
    }
}

// ✗ MAUVAIS: System.out.println, pas de log
System.out.println("Deleting task");
```

## Organisation des fichiers

```
src/main/java/com/deployfast/taskmanagement/
├── TaskManagementApplication.java      # Application main
├── config/                             # @Configuration beans
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   └── ...
├── controller/                         # @RestController
│   ├── AuthController.java
│   ├── TaskController.java
│   └── ...
├── service/                            # @Service (logique métier)
│   ├── AuthService.java
│   ├── TaskService.java
│   ├── CustomUserDetailsService.java
│   └── ...
├── repository/                         # @Repository (JPA)
│   ├── UserRepository.java
│   ├── TaskRepository.java
│   └── ...
├── entity/                             # @Entity (modèles JPA)
│   ├── User.java
│   ├── Task.java
│   ├── TaskStatus.java
│   ├── TaskPriority.java
│   └── UserRole.java
├── dto/                                # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── CreateTaskRequest.java
│   ├── TaskDto.java
│   └── ...
├── security/                           # JWT & Spring Security
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── SecurityConfig.java             # Peut aussi être dans config/
└── exception/                          # Exceptions customisées
    ├── ResourceNotFoundException.java
    ├── ResourceAlreadyExistsException.java
    ├── BusinessValidationException.java
    └── GlobalExceptionHandler.java
```

## Tests

### Structure de test
```
src/test/java/com/deployfast/taskmanagement/
├── service/
│   ├── AuthServiceTest.java            # Tests unitaires
│   ├── TaskServiceTest.java
│   └── ...
├── controller/
│   ├── TaskControllerFeatureTest.java  # Tests Feature (intégration)
│   └── ...
└── security/
    └── AuthenticationSecurityTest.java # Tests d'authentification
```

### Conventions de test
```java
// ✓ BON: Nommage clair du test
@Test
@DisplayName("Devrait créer une tâche avec succès")
void testCreateTaskSuccess() {
    // Arrange
    CreateTaskRequest request = /* ... */;
    
    // Act
    TaskDto result = taskService.createTask(request);
    
    // Assert
    assertNotNull(result);
    assertEquals("Test Task", result.getTitle());
}

// ✗ MAUVAIS: Nommage vague
@Test
void test1() {
    // Pas clair ce qu'on teste...
}

// ✗ MAUVAIS: Pas de structure AAA
@Test
void testCreate() {
    result = service.create(request);
    assertEquals("Test Task", result.getTitle());
}
```

### Couverture de code
- Objectif: **≥ 80%** (configuré dans `pom.xml`)
- Tester les paths heureux et les exceptions
- Vérifier avec: `mvn test jacoco:report`

## Validation des données

### DTOs de requête
```java
// ✓ Valider TOUS les inputs utilisateur
@Data
public class CreateTaskRequest {
    @NotBlank(message = "Le titre est requis")
    @Size(min = 3, max = 200)
    private String title;
    
    @NotNull
    private String status;
}

// ✗ MAUVAIS: Pas de validation
@Data
public class CreateTaskRequest {
    private String title;
    private String status;
}
```

### Logique métier
```java
// ✓ Valider les règles métier dans le service
if (request.getDueDate().isBefore(LocalDateTime.now())) {
    throw new BusinessValidationException("Date invalide");
}

// Ne pas laisser passer des données incohérentes à la DB
```

## Sécurité

### Protection contre injections
```java
// ✓ BON: Requête paramétrée JPA
@Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status = :status")
Page<Task> findByUserAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

// ✗ MAUVAIS: String concatenation (SQL Injection!)
String query = "SELECT * FROM tasks WHERE user_id = " + userId;
```

### Mots de passe
```java
// ✓ BON: Toujours hasher avec BCrypt
String hashedPassword = passwordEncoder.encode(plainPassword);

// ✗ MAUVAIS: Jamais stocker en clair
user.setPassword(plainPassword);
```

### Authentification
```java
// ✓ BON: Utiliser SecurityContext
User currentUser = (User) SecurityContextHolder
    .getContext()
    .getAuthentication()
    .getPrincipal();

// ✗ MAUVAIS: Pas de vérification de propriété
task = taskRepository.findById(taskId); // N'importe qui peut accéder!
```

## Documentation

### JavaDoc
```java
// ✓ BON: Documentation sur les public APIs
/**
 * Crée une nouvelle tâche pour l'utilisateur authentifié.
 * 
 * @param request Les données de création
 * @return La tâche créée
 * @throws BusinessValidationException Si les données ne sont pas valides
 */
public TaskDto createTask(CreateTaskRequest request) { }

// ✗ MAUVAIS: Pas de documentation
public TaskDto createTask(CreateTaskRequest request) { }
```

### Fichiers README/ARCHITECTURE
- Mettre à jour `README.md` si changements majeurs
- Documenter les décisions architecturales dans `ARCHITECTURE.md`
- Ajouter des exemples curl pour nouveaux endpoints

## Processus de contribution

### 1. Fork et clone
```bash
git clone <votre-fork>
cd deployfast-task-management-api
```

### 2. Créer une branche
```bash
git checkout -b feature/user-profile-api
```

### 3. Développer et tester
```bash
# Éditer les fichiers
# Compiler
mvn clean compile

# Tester
mvn test

# Formater le code
```

### 4. Commit avec messages clairs
```bash
# ✓ BON: Message descriptif
git commit -m "Ajouter endpoint pour récupérer le profil utilisateur"

# ✗ MAUVAIS: Message vague
git commit -m "Fix bug"
```

### 5. Pousser et créer une Pull Request
```bash
git push origin feature/user-profile-api
```

Inclure dans le PR:
- Description du changement
- Type de changement (Feature/Fix/Refactor)
- Checklist de vérification:
  - [ ] Tests ajoutés/mis à jour
  - [ ] Code compilé sans erreurs
  - [ ] Documentation mise à jour
  - [ ] Pas de breaking changes

## Checklist avant de soumettre

- [ ] Code compilé sans warnings
- [ ] Tous les tests passent (`mvn test`)
- [ ] Couverture ≥ 80% (`mvn test jacoco:report`)
- [ ] Code formaté (pattern conventions respectées)
- [ ] Commentaires/JavaDoc présents
- [ ] Git history clean (commits logiques)
- [ ] README/ARCHITECTURE.md mis à jour si nécessaire
- [ ] Pas de secrets (API keys, passwords) en dur

## Questions?

Consulter:
- `README.md` pour l'usage
- `ARCHITECTURE.md` pour la conception
- Code existant (référence de style)

---

Merci de contribuer ! 🚀
