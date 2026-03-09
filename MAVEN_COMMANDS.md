# Commandes Maven utiles

## Développement

> **Note :** le POM cible Java 17. Assurez-vous d’utiliser un JDK 17 récent (vérifiez avec `java -version`). L’erreur "release version 17 not supported" signifie que le JDK utilisé est trop ancien ; installez JDK 17 ou modifiez les propriétés `<java.version>`, `<maven.compiler.source>` et `<maven.compiler.target>` dans `pom.xml` pour correspondre à votre JDK.

### Compiler le projet
```bash
mvn clean compile
```

### Exécuter l'application
```bash
mvn spring-boot:run
```

### Lancer les tests
```bash
mvn clean test
```

### Tests spécifiques
```bash
# Tester une classe spécifique
mvn test -Dtest=AuthServiceTest

# Tester une méthode spécifique
mvn test -Dtest=AuthServiceTest#testRegisterSuccess
```

### Générer le rapport JaCoCo (couverture)
```bash
mvn clean test jacoco:report
# Consulter: target/site/jacoco/index.html
```

## Package & Build

### Créer un JAR exécutable
```bash
mvn clean package
```

### Exécuter le JAR
```bash
java -jar target/taskmanagement-api-1.0.0.jar
```

### Sauter les tests lors du build
```bash
mvn clean package -DskipTests
```

## Nettoyage

### Supprimer les fichiers compilés
```bash
mvn clean
```

### Nettoyer et compiler
```bash
mvn clean install
```

## Dépendances

### Lister les dépendances du projet
```bash
mvn dependency:list
```

### Voir l'arbre de dépendances
```bash
mvn dependency:tree
```

### Checker les mises à jour
```bash
mvn versions:display-dependency-updates
```

### Voir les vulnérabilités CVE (optionnel)
```bash
mvn org.owasp:dependency-check-maven:check
```

## Validation

### Formater le code (optionnel - besoin de plugin)
```bash
mvn spotless:apply
```

### Analyser le code (Sonatype)
```bash
mvn sonar:sonar
```

### Sonar en local avec couverture
```bash
mvn clean verify sonar:sonar \
    -Dsonar.host.url=https://sonarcloud.io \
    -Dsonar.login=${SONAR_TOKEN}
```

### Pipeline CI/CD (GitHub Actions)
Le projet comporte un workflow prêt à l'emploi dans `.github/workflows/ci.yml` qui :

1. Compile et exécute les tests (y compris JaCoCo)
2. Lance OWASP Dependency-Check
3. Construit et analyse l'image Docker via Trivy
4. Recherche de secrets avec TruffleHog
5. Lance l'analyse Sonar et attend la quality gate

Aucun des éléments ci-dessus ne nécessite d'intervention manuelle ; il suffit de pousser ou ouvrir une PR sur `main`.

## Troubleshooting

### Vider le cache Maven local
```bash
rm -rf ~/.m2/repository
mvn clean install
```

### Forcer le téléchargement des dépendances
```bash
mvn clean dependency:resolve
```

### Vérifier la version Java
```bash
mvn -version
```

## Aide générale

### Lister tous les goals Maven
```bash
mvn help:describe -Dplugin=spring-boot-maven-plugin
```

### Exécuter sans interaction
```bash
mvn -B clean test
```
