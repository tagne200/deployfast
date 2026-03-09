# Optimisations et Performance

Guide des optimisations et ajustements pour améliorer les performances de l'application.

## 🚀 Optimisations rapides (implémentable en 2h)

### 1. Caching des tâches
```java
// Dans TaskService
@Cacheable(value = "tasks", key = "#userId + '-' + #pageable.pageNumber")
public Page<TaskDto> getAllTasks(Long userId, Pageable pageable) {
    // ...
}

// Dans application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=10m
```

**Impact**: Réduit requêtes DB pour lectures répétées  
**Complexité**: Faible  
**Bénéfice**: ~50% plus rapide pour listes répétées

### 2. Lazy Loading des relations
```java
// Entity User
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Task> tasks;
```

**Impact**: Réduit la taille des objets en mémoire  
**Complexité**: Faible  
**Bénéfice**: Moins de mémoire consommée

### 3. Indexing base de données
```sql
-- Dans initscript ou migration
CREATE INDEX idx_tasks_user_created ON tasks(user_id, created_at DESC);
CREATE INDEX idx_tasks_due_date ON tasks(due_date) WHERE status != 'COMPLETED';
```

**Impact**: Accélère les requêtes filtrant par user + date  
**Complexité**: Faible  
**Bénéfice**: ~5-10x plus rapide pour certaines requêtes

### 4. Pagination côté requête
```java
// TaskRepository
@Query("""
    SELECT new com.deployfast.taskmanagement.dto.TaskDto(...) 
    FROM Task t 
    WHERE t.user.id = :userId
    ORDER BY t.createdAt DESC
""")
Page<TaskDto> findTaskDTOs(Long userId, Pageable pageable);
```

**Impact**: Retorner DTOs au lieu d'entities (moins lourd)  
**Complexité**: Faible  
**Bénéfice**: Réduction taille requêtes API

---

## ⚡ Optimisations avancées (implémentable en 1 jour)

### 1. Redis Distributed Cache
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 60000
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10 minutes
```

**Impact**: Cache distribué (multi-instances)  
**Complexité**: Moyen  
**Bénéfice**: Scalabilité horizontale, persistent cache

### 2. Asynchronous Processing
```java
// Dans TaskService
@Async
public CompletableFuture<TaskDto> createTaskAsync(CreateTaskRequest request) {
    return CompletableFuture.completedFuture(createTask(request));
}

// Configuration
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
```

**Impact**: Requêtes long-running ne bloquent pas  
**Complexité**: Moyen  
**Bénéfice**: Meilleure latence API

### 3. Connection Pool Optimization
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Défaut: 10
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 20000
      auto-commit: true
```

**Impact**: Gestion optimale des connexions DB  
**Complexité**: Faible  
**Bénéfice**: Meilleure concurrence

### 4. Query Performance Optimization
```java
// AVANT: N+1 query problem
@Override
public Page<TaskDto> getAllTasks(Pageable pageable) {
    return taskRepository.findByUserId(userId, pageable)  // 1 query
        .map(task -> convertToDTO(task));                  // N queries!
}

// APRÈS: Utiliser fetch join
@Query("""
    SELECT DISTINCT t FROM Task t 
    LEFT JOIN FETCH t.user 
    WHERE t.user.id = :userId
""")
Page<Task> findWithUser(Long userId, Pageable pageable);
```

**Impact**: Évite N+1 queries  
**Complexité**: Faible  
**Bénéfice**: 10-100x plus rapide

---

## 🏛️ Optimisations d'architecture (1-2 semaines)

### 1. API Gateway (Kong, Nginx)
```nginx
upstream api {
    server localhost:8080;
    server localhost:8081;
    server localhost:8082;
}

server {
    listen 80;
    server_name api.myapp.com;
    
    location / {
        proxy_pass http://api;
        proxy_cache_path /tmp/nginx_cache levels=1:2 keys_zone=api_cache:10m;
        proxy_cache api_cache;
        proxy_cache_valid 200 10m;
        
        # Rate limiting
        limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
        limit_req zone=api_limit burst=20 nodelay;
    }
}
```

**Impact**: Load balancing, rate limiting, caching central  
**Complexité**: Moyen  
**Bénéfice**: Scalabilité, protection contre abuse

### 2. Database Replication
```
┌─────────────────────┐
│  Primary Database   │
│ (read/write)        │
└──────────┬──────────┘
           │ Replication
           ▼
┌──────────────────────────┐
│ Replica 1   │ Replica 2  │
│ (read-only) │ (read-only)│
└──────────────────────────┘
```

Configuration:
```java
@Configuration
public class RoutingConfig {
    @Bean
    public DataSource dataSource() {
        // Write to primary, read from replicas
    }
}
```

**Impact**: Scalabilité lectures, haute disponibilité  
**Complexité**: Élevé  
**Bénéfice**: 3-10x plus de capacité lecture

### 3. Elasticsearch for Search
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

```java
@Document(indexName = "tasks")
public class TaskDocument {
    @Id private Long id;
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    private String description;
}

@Repository
public interface TaskSearchRepository extends ElasticsearchRepository<TaskDocument, Long> {
    List<TaskDocument> findByTitleContaining(String title);
}
```

**Impact**: Recherche full-text ultra-rapide  
**Complexité**: Élevé  
**Bénéfice**: Recherche en < 100ms même avec millions d'items

### 4. Message Queue (RabbitMQ/Kafka)
```java
// Asynchronous task processing
@Component
public class TaskEventListener {
    @RabbitListener(queues = "task-created")
    public void onTaskCreated(TaskCreatedEvent event) {
        // Send email, update analytics, etc.
    }
}

@Service
public class TaskService {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public TaskDto createTask(CreateTaskRequest request) {
        TaskDto task = /* ... */;
        rabbitTemplate.convertAndSend("task-created", 
            new TaskCreatedEvent(task));
        return task;
    }
}
```

**Impact**: Découplage des services, processing async  
**Complexité**: Élevé  
**Bénéfice**: Scalabilité, nouvelle fonctionnalités découpées

---

## 📊 Monitoring et Profiling

### 1. Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: taskmanagement-api
```

Endpoints disponibles:
```
GET /actuator/health              - État de l'application
GET /actuator/metrics             - Métriques disponibles
GET /actuator/metrics/{metric}    - Détails métrique
GET /actuator/prometheus          - Prometheus format
```

### 2. Logging structured (JSON)
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```yaml
logging:
  pattern:
    json: |
      {
        "timestamp": "%d{ISO8601}",
        "level": "%level",
        "logger": "%logger",
        "thread": "%thread",
        "message": "%message",
        "stacktrace": "%exception"
      }
```

### 3. JVM Tuning
```bash
# Pour applications avec beaucoup de mémoire
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UnlockDiagnosticVMOptions \
     -XX:G1SummarizeRSetStatsPeriod=1 \
     -jar taskmanagement-api-1.0.0.jar

# Pour applications avec latence critique
java -Xms1g -Xmx1g \
     -XX:+UseConcMarkSweepGC \
     -XX:CMSInitiatingOccupancyFraction=75 \
     -jar taskmanagement-api-1.0.0.jar
```

### 4. Load Testing avec JMeter
```jmeter
# Installer JMeter: https://jmeter.apache.org/
# Créer test plan:
1. Thread Group: 1000 utilisateurs, 10 minutes
2. HTTP Request: GET /api/v1/tasks
3. Response assertions
4. View Results Tree

# Lancer
jmeter -n -t taskmanagement-api-test.jmx -l results.jtl -j jmeter.log
```

---

## 🎯 Checklist Performance

### Avant Prod
- [ ] Activé compression gzip (`server.compression.enabled: true`)
- [ ] Configuré thread pool (`spring.task.execution.pool.*`)
- [ ] Optimisé indexes base de données
- [ ] Testé load avec 10x utilisateurs prévus
- [ ] Configuré file descriptors limits (`ulimit -n 65536`)
- [ ] Activé prepared statements caching
- [ ] Configuré connexion pooling (HikariCP)

### Monitoring Prod
- [ ] Actuator endpoints sécurisés (`/actuator` protégé)
- [ ] Logs centralisés (ELK, Splunk, etc.)
- [ ] Métriques (Prometheus + Grafana)
- [ ] Alertes (CPU, mémoire, DB connections)
- [ ] Uptime monitoring (Pingdom, DataDog)
- [ ] Error tracking (Sentry, Rollbar)

### Optimisations Futures
- [ ] Pagination immédiate (Server-Sent Events)
- [ ] GraphQL pour requêtes flexibles
- [ ] WebSocket pour notifications temps réel
- [ ] CDN pour assets statiques
- [ ] Database sharding si > 1 milliard de lignes

---

## 📈 Métriques clés à surveiller

| Métrique | Valeur idéale | Action |
|----------|--------------|--------|
| Response time (p99) | < 100ms | Analyser les lentes requêtes |
| CPU | < 70% | Scaler horizontalement |
| Memory | < 80% | Augmenter heap / optimiser cache |
| DB Connections | < max*0.8 | Diminuer connection lifetime |
| Error rate | < 0.1% | Investiguer les erreurs |
| Cache hit ratio | > 80% | Augmenter TTL cache |
| Throughput | > 1000 req/s | Scaler / optimiser |

---

## 🔗 Ressources supplémentaires

- Spring Performance Best Practices: https://spring.io/blog
- JVM Tuning Guide: https://docs.oracle.com/javatuning
- PostgreSQL Optimization: https://wiki.postgresql.org/wiki/Performance_Optimization
- Redis Caching: https://redis.io/documentation
- Load Testing: https://jmeter.apache.org/usermanual/

---

**Prochaine étape**: Choisir 1-2 optimisations rapides à implémenter cette semaine! 🚀
