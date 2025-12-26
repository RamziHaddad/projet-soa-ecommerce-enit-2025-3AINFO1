# Payment Microservice

Un microservice de paiement robuste basé sur Quarkus, conçu pour traiter les paiements dans une architecture e-commerce distribuée.

## 🎯 Fonctionnalités Implémentées

### Architecture Microservice
- **API REST** pour le traitement des paiements
- **Base de données PostgreSQL** pour la persistance
- **Synchronous REST notifications** to notify other services (configurable endpoint)
- **RabbitMQ** pour la gestion des commandes

### Patterns de Résilience
- **Circuit Breaker** : Protection contre les pannes en cascade
- **Retry** : Redémarrage automatique en cas d'échec temporaire
- **Timeout** : Limitation des temps d'attente
- **Saga Pattern** : Coordination de transactions distribuées

- **Idempotence** : Traitement sécurisé des requêtes dupliquées

### Simulation de Paiement
- Logique de simulation avec taux de succès configurable (80%)
- Validation basique des données de carte (format 16 chiffres)
- Gestion des états de paiement (PENDING, SUCCESS, FAILED)

## 🛠️ Prérequis

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL** (base de données)
- **Kafka** (removed — synchronous REST notifications are used instead)
- **RabbitMQ** (optionnel - dev services activés)

## 🗄️ Configuration Base de Données

Créer une base PostgreSQL avec les paramètres suivants :
- **Nom** : `payment_db`
- **Utilisateur** : `payment_user`
- **Mot de passe** : `password`

## 🚀 Démarrage de l'Application

```bash
cd payment-service
mvn quarkus:dev
```

L'application démarre sur `http://localhost:8081`.

Configure the notification endpoint (optional):

```properties
# Where to send payment notifications (leave empty to disable)
services.order.notify.url=http://localhost:8082/api/orders/payment-notify
```

## 📊 Schéma de Base de Données

### Table `paiements`
| Colonne | Type | Description |
|---------|------|-------------|
| `payment_id` | UUID | Clé primaire |
| `user_id` | UUID | Identifiant utilisateur |
| `card_number` | VARCHAR(16) | Numéro de carte (simulation) |
| `amount` | DECIMAL | Montant du paiement |
| `status` | VARCHAR | État (PENDING/SUCCESS/FAILED) |
| `attempts` | INT | Nombre de tentatives |
| `previous_step` | VARCHAR | Étape précédente (INIT/VALIDATE/PROCESS) |
| `next_step` | VARCHAR | Étape suivante |
| `created_at` | TIMESTAMP | Date de création |



## 🔌 API REST

### Traitement d'un Paiement (internal)

```bash
POST /paiement
Content-Type: application/json

{
  "paymentId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "cardNumber": "1234567890123456",
  "amount": 100.50
}
```

**Réponse de succès :**
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "message": "Payment processed successfully"
}
```

### Compatibility endpoints for order-service

Order service expects a different payload shape; a compatibility endpoint is available:

```bash
POST /api/payment/process
Content-Type: application/json

{
  "orderNumber": "ORD-123",
  "customerId": 123,
  "amount": 50.00,
  "paymentMethod": "CARD"
}
```

This will be mapped to an internal payment and processed (a paymentId UUID will be generated). Note: `customerId` (Long) is deterministically converted to an internal `userId` UUID using a name-based UUID to allow correlation between services.

Refund endpoint:

```bash
POST /api/payment/refund/{transactionId}
```


## 🧪 Tests Fonctionnels

### Configuration des Tests

```bash
# Démarrer PostgreSQL avec Docker
docker run --name postgres-payment \
  -e POSTGRES_DB=payment_db \
  -e POSTGRES_USER=payment_user \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 -d postgres:15

# Lancer l'application
mvn quarkus:dev
```

### 1. Test de Paiement Réussi

```bash
curl -X POST http://localhost:8081/paiement \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "cardNumber": "1234567890123456",
    "amount": 100.50
  }'
```

**Résultat attendu** : Status `SUCCESS` (80% de probabilité avec simulation)

### 2. Test de Paiement Échoué (Validation)

```bash
# Carte invalide (< 16 chiffres)
curl -X POST http://localhost:8081/paiement \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "cardNumber": "123456789",
    "amount": 50.00
  }'
```

**Résultat attendu** : Status `FAILED` avec message "Validation failed"

### 3. Test d'Idempotence

```bash
# Premier appel
curl -X POST http://localhost:8081/paiement \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "cardNumber": "1234567890123456",
    "amount": 75.25
  }'

# Deuxième appel avec même paymentId
curl -X POST http://localhost:8081/paiement \
  -H "Content-Type: application/json" \
  -d '{
    "paymentId": "550e8400-e29b-41d4-a716-446655440003",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "cardNumber": "1234567890123456",
    "amount": 75.25
  }'
```

**Résultat attendu** : Deuxième appel retourne le même résultat sans retraitement

### 4. Test des notifications

Après un paiement réussi, vérifier les logs ou configurer `services.order.notify.url` vers un endpoint de test pour valider la réception des notifications REST.


### 5. Tests Automatisés

```bash
# Lancer tous les tests unitaires
mvn test

# Tests d'intégration (avec base de données)
mvn test -Dtest=PaymentResourceTest
```

## 🏗️ Architecture Logicielle

### Composants Principaux

- **Entities** : `Paiement` (modèles de données)
- **DTOs** : `PaymentRequest`, `PaymentResponse` (objets de transfert)
- **Services** : `PaymentService` (logique métier), `SagaService` (coordination)
- **Resource** : `PaymentResource` (couche REST)

### Flux de Traitement

```
1. Réception HTTP POST /paiement
2. Validation des données d'entrée
3. Vérification d'idempotence (paymentId unique)
4. Simulation du traitement bancaire (80% succès)
5. Coordination via Saga Pattern
6. Notification des événements (REST)
7. Retour réponse HTTP
```

### Patterns Implémentés

- **Saga Pattern** : Gestion des transactions distribuées

- **Idempotence** : Sécurité contre les doublons
- **Circuit Breaker** : Résilience aux pannes

## 🚀 Prochaines étpes : Intégration API Externe

### Objectif
Remplacer la simulation actuelle par une véritable API bancaire pour traiter des paiements réels.

### Avantages
- **Paiements réels** : Traitement effectif des transactions
- **Conformité** : Respect des normes de sécurité (PCI DSS)
- **Fiabilité** : Réduction des risques de fraude
- **Évolutivité** : Support multi-devises et méthodes de paiement

### Options d'Intégration

#### 1. Stripe (Recommandé)
- **SDK Java** officiel disponible
- **Documentation** complète et API stable
- **Taux** : 2.9% + 0.25€ par transaction
- **Sécurité** : Tokenisation côté client

#### 2. PayPal
- **Adoption large** dans l'e-commerce
- **API REST** standardisée
- **Support multi-devises** natif

#### 3. APIs Bancaires Directes (PSD2)
- **Coûts réduits** (pas d'intermédiaire)
- **Conformité réglementaire** européenne
- **Complexité technique** plus élevée

### Implémentation Prévue

#### Phase 1 : Configuration
- Ajout dépendances SDK (Stripe Java)
- Configuration clés API (variables d'environnement)
- Création service de paiement externe

#### Phase 2 : Intégration
- Remplacement simulation par appels API réels
- Gestion erreurs et timeouts spécifiques
- Implémentation tokenisation frontend

#### Phase 3 : Sécurité
- Audit sécurité PCI DSS
- Chiffrement données sensibles
- Monitoring et alerting

#### Phase 4 : Production
- Tests en environnement de recette
- Mise en place monitoring
- Documentation exploitation

### Critères de Choix
- **Coûts** : Analyse TCO sur 3 ans
- **Sécurité** : Conformité réglementaire
- **Performance** : Latence et débit
- **Support** : Qualité de la documentation et communauté
- **Évolutivité** : Capacités d'intégration futures

Cette évolution permettra de transformer le prototype en solution de paiement production-ready, capable de traiter des transactions financières réelles de manière sécurisée et fiable.
