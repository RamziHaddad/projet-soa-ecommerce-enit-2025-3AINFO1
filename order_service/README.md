# 🛍️ Microservice Order Service

## 📖 Vue d'Ensemble

Le **Order Service** est le microservice central de notre architecture e-commerce, agissant comme un **orchestrateur Saga** pour gérer le cycle de vie complet des commandes. Il coordonne la communication synchrone avec les autres microservices pour assurer une transaction distribuée cohérente.

### 🎯 Rôle Principal
- **Orchestrateur Saga** : Coordonne les étapes de création de commande
- **Gestion des transactions distribuées** : Assure la cohérence des données
- **Mécanisme de compensation** : Rollback en cas d'échec

## 🏗️ Architecture

### Patterns Utilisés
- **Saga Pattern (Orchestration)** : Orchestration centralisée des transactions distribuées
- **Circuit Breaker** : Protection contre les échecs en cascade
- **Retry Pattern** : Tentatives de récupération automatique
- **Communication Synchrone** : REST avec Feign Clients

### Stack Technique
- **Java 17** + **Spring Boot 3.4.11**
- **Spring Data JPA** + **PostgreSQL**
- **Spring Cloud OpenFeign** pour les appels REST
- **Resilience4j** pour la résilience
- **Maven** pour la gestion des dépendances

## 🔄 Pipeline de Communication

### Flux Principal de Création de Commande

```Diagram
    participantmermaid
sequence Client
    participant Order Service
    participant Cart Service
    participant Inventory Service
    participant Payment Service
    participant Shipping Service
    participant Communication Service

    Client->>Order Service: POST /api/orders
    Order Service->>Order Service: Initialiser Saga
    Order Service->>Cart Service: Vérifier panier
    Cart Service-->>Order Service: Articles validés
    
    Order Service->>Inventory Service: Réserver stock
    alt Stock disponible
        Inventory Service-->>Order Service: Stock réservé
        Order Service->>Payment Service: Traiter paiement
        alt Paiement réussi
            Payment Service-->>Order Service: Paiement confirmé
            Order Service->>Shipping Service: Créer livraison
            alt Livraison créée
                Shipping Service-->>Order Service: Livraison confirmée
                Order Service->>Communication Service: Envoyer notification
                Communication Service-->>Order Service: Notification envoyée
                Order Service->>Order Service: Finaliser Saga (COMPLETED)
            else Échec livraison
                Shipping Service-->>Order Service: Erreur livraison
                Order Service->>Order Service: Compensation (remboursement + stock)
            end
        else Échec paiement
            Payment Service-->>Order Service: Erreur paiement
            Order Service->>Order Service: Compensation (libération stock)
        end
    else Stock insuffisant
        Inventory Service-->>Order Service: Erreur stock
        Order Service->>Order Service: Compensation (rollback panier)
    end
    
    Order Service-->>Client: Réponse commande
```

### Étapes Détaillées

1. **Initialisation** (Order Service)
   - Validation des données de commande
   - Création de l'état Saga initial
   - Génération du numéro de commande

2. **Vérification Panier** (Cart Service)
   - Récupération des articles du panier
   - Validation de la disponibilité
   - Calcul du montant total

3. **Gestion Stock** (Inventory Service)
   - Vérification de la disponibilité
   - Réservation temporaire du stock
   - Échec → Compensation : Rollback panier

4. **Traitement Paiement** (Payment Service)
   - Pré-authorisation du montant
   - Confirmation du paiement
   - Échec → Compensation : Libération stock

5. **Arrangement Livraison** (Shipping Service)
   - Création de la commande de livraison
   - Attribution du transporteur
   - Échec → Compensation : Remboursement + Stock

6. **Notification** (Communication Service)
   - Envoi email de confirmation
   - Notification mobile
   - Mise à jour statut final

## 📁 Structure du Projet

```
src/
├── main/java/com/onlineshop/order/
│   ├── OrderApplication.java          # Point d'entrée
│   ├── client/                        # Clients Feign
│   │   ├── InventoryServiceClient.java
│   │   ├── PaymentServiceClient.java
│   │   └── ShippingServiceClient.java
│   ├── communication/                 # Stratégies communication
│   │   ├── CommunicationStrategy.java
│   │   └── RestCommunicationStrategy.java
│   ├── config/                        # Configurations
│   │   ├── DatabaseConfig.java
│   │   └── FeignConfig.java
│   ├── controller/                    # Controllers REST
│   │   └── OrderController.java
│   ├── dto/                          # Data Transfer Objects
│   │   ├── request/
│   │   │   ├── OrderRequest.java
│   │   │   ├── InventoryRequest.java
│   │   │   ├── PaymentRequest.java
│   │   │   └── ShippingRequest.java
│   │   └── response/
│   │       ├── OrderResponse.java
│   │       ├── InventoryResponse.java       ├── PaymentResponse.java
│   │       └── Shipping
│   │Response.java
│   ├── exception/                    # Gestion d'exceptions
│   │   ├── CompensationException.java
│   │   ├── SagaException.java
│   │   └── ServiceCommunicationException.java
│   ├── model/                        # Entités JPA
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── OrderStatus.java
│   │   ├── SagaState.java
│   │   ├── SagaStatus.java
│   │   └── SagaStep.java
│   ├── repository/                   # Repositories
│   │   ├── OrderRepository.java
│   │   └── SagaStateRepository.java
│   ├── saga/                         # Orchestration Saga
│   │   ├── SagaOrchestrator.java
│   │   ├── SagaOrchestratorImpl.java
│   │   ├── CompensationHandler.java
│   │   └── CompensationHandlerImpl.java
│   └── service/                      # Services métier
│       ├── OrderService.java
│       └── OrderServiceImpl.java
└── resources/
    └── application.properties         # Configuration
```

## 🚀 Installation et Démarrage

### Prérequis
- Java 17+
- Maven 3.6+
- PostgreSQL 13+
- Docker (optionnel)

### Installation Locale

1. **Cloner le projet**
```bash
git clone <repository-url>
cd order_service
```

2. **Configurer la base de données**
```bash
# Créer la base de données PostgreSQL
createdb order_db
```

3. **Configurer les variables d'environnement**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/order_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

4. **Compiler et démarrer**
```bash
mvn clean compile
mvn spring-boot:run
```

### Démarrage avec Docker

```bash
# Démarrer avec docker-compose
docker-compose up -d

# Vérifier les logs
docker-compose logs -f order-service
```

## 🌐 API Endpoints

### Orders

#### Créer une commande
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": 123,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 29.99
    }
  ],
  "shippingAddress": "123 Rue Example, 75001 Paris"
}
```

**Réponse (200 OK)**
```json
{
  "id": 1,
  "orderNumber": "ORD-2024-001",
  "customerId": 123,
  "status": "COMPLETED",
  "totalAmount": 59.98,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 29.99,
      "totalPrice": 59.98
    }
  ],
  "createdAt": "2024-12-14T09:50:00"
}
```

#### Récupérer une commande
```http
GET /api/orders/{orderId}
```

#### Récupérer par numéro de commande
```http
GET /api/orders/number/{orderNumber}
```

#### Lister les commandes d'un client
```http
GET /api/orders/customer/{customerId}
```

#### Annuler une commande
```http
DELETE /api/orders/{orderId}
```

## 📊 États et Statuts

### États de Commande
- `PENDING` : Commande créée, en attente de traitement
- `PROCESSING` : En cours de traitement par la Saga
- `COMPLETED` : Traitement terminé avec succès
- `FAILED` : Échec du traitement
- `CANCELLED` : Commande annulée

### États de Saga
- `STARTED` : Saga initiée
- `INVENTORY_CHECKED` : Stock vérifié
- `PAYMENT_PROCESSED` : Paiement traité
- `SHIPPING_CREATED` : Livraison créée
- `NOTIFIED` : Notification envoyée
- `COMPLETED` : Saga terminée avec succès
- `COMPENSATING` : En cours de compensation
- `COMPENSATED` : Compensation terminée

## 🔧 Configuration

### Base de Données
```properties
# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/order_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Services Externes
```properties
# URLs des services externes
services.inventory.url=http://localhost:8081
services.payment.url=http://localhost:8082
services.shipping.url=http://localhost:8083
```

### Resilience4j
```properties
# Circuit Breaker
resilience4j.circuitbreaker.instances.default.slidingWindowSize=10
resilience4j.circuitbreaker.instances.default.failureRateThreshold=50

# Retry
resilience4j.retry.instances.default.maxAttempts=3
resilience4j.retry.instances.default.waitDuration=1s
```

## 🧪 Tests

### Exécuter les Tests
```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn test -Dspring.profiles.active=test

# Rapport de couverture
mvn jacoco:report
```

### Test avec Postman

1. **Importer la collection** : `docs/postman/OrderService.postman_collection.json`

2. **Variables d'environnement** :
   ```
   baseUrl: http://localhost:8080
   customerId: 123
   ```

3. **Tests à exécuter** :
   - `POST /api/orders` - Créer une commande
   - `GET /api/orders/{orderId}` - Récupérer une commande
   - `GET /api/orders/customer/{customerId}` - Lister les commandes

## 🐳 Docker

### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/order-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  order-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/order_db
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
    depends_on:
      - postgres

  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=order_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

## 🔍 Monitoring et Observabilité

### Logs
Les logs sont configurés avec différents niveaux :
- `DEBUG` : Détails du flux Saga
- `INFO` : Événements métier
- `WARN` : Avertissements
- `ERROR` : Erreurs

### Métriques
- Temps de traitement par étape Saga
- Taux de succès/échec des transactions
- Utilisation des circuit breakers
- Temps de réponse des services externes

## 🛠️ Développement

### Ajouter un Nouveau Service Externe

1. **Créer le DTO de requête/réponse**
```java
@Data
public class NewServiceRequest {
    private String param1;
    private String param2;
}

@Data
public class NewServiceResponse {
    private String result;
    private boolean success;
}
```

2. **Créer le client Feign**
```java
@FeignClient(name = "new-service", url = "${services.new.url}")
public interface NewServiceClient {
    @PostMapping("/process")
    NewServiceResponse process(NewServiceRequest request);
}
```

3. **Intégrer dans la Saga**
```java
private void executeNewServiceStep(Order order) {
    NewServiceRequest request = // ... build request
    NewServiceResponse response = newServiceClient.process(request);
    // ... handle response
}
```

## 🚨 Gestion d'Erreurs

### Stratégies de Compensation

1. **Échec Stock** : Rollback panier
2. **Échec Paiement** : Libération stock
3. **Échec Livraison** : Remboursement + Libération stock
4. **Échec Notification** : Réessai ou notification manuelle

### Circuit Breakers
Chaque appel externe est protégé par un circuit breaker :
- **Ouvert** : Rejette immédiatement après échecs répétés
- **Mi-ouvert** : Teste quelques requêtes avant réouverture
- **Fermé** : Fonctionnement normal

## 📈 Points d'Amélioration

### Court Terme
- [ ] Implémentation complète des controllers
- [ ] Tests d'intégration avec services mock
- [ ] Validation des données d'entrée
- [ ] Gestion des timeouts

### Moyen Terme
- [ ] Migration vers architecture event-driven
- [ ] Implémentation du pattern Outbox
- [ ] Monitoring avancé avec Prometheus
- [ ] Documentation OpenAPI/Swagger

### Long Terme
- [ ] Support des transactions asynchrones
- [ ] Machine learning pour la prédiction d'échecs
- [ ] Auto-scaling basé sur les métriques
- [ ] Migration vers Kubernetes

## 🤝 Contribution

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/amazing-feature`)
3. Commit les changements (`git commit -m 'Add amazing feature'`)
4. Push vers la branche (`git push origin feature/amazing-feature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 📞 Support

Pour toute question ou support :
- 📧 Email : support@onlineshop.com
- 📚 Documentation : [Wiki du projet](wiki-url)
- 🐛 Issues : [GitHub Issues](issues-url)
