# Microservice de Recherche - SOA E-commerce

## Vue d'ensemble

Ce microservice implémente un système de recherche de produits pour une plateforme e-commerce utilisant Elasticsearch. Il s'intègre avec un service catalogue pour indexer automatiquement les produits et fournit des API REST pour effectuer des recherches avancées.

## Architecture

Le microservice suit une architecture SOA (Service-Oriented Architecture) avec communication REST synchrone entre services.

### Composants principaux

- **Search Service** : Service principal de recherche (Port 8081)
- **Mock Catalogue Service** : Service mock du catalogue de produits (Port 8080)
- **Elasticsearch** : Moteur de recherche et d'indexation (Port 9200)

### Communication inter-services

La communication utilise le protocole HTTP/REST avec les patterns suivants :

- **Synchronisation des données** : Le service de recherche récupère périodiquement les produits non indexés du catalogue
- **Indexation automatique** : Les nouveaux produits sont automatiquement indexés dans Elasticsearch
- **Recherche en temps réel** : Les clients peuvent effectuer des recherches complexes via des filtres

## Fonctionnalités

### Pattern Outbox pour la cohérence
- **Marquage préventif** : Les produits sont marqués comme indexés avant traitement pour éviter les doublons
- **Compensation automatique** : En cas d'échec d'indexation, les produits sont automatiquement "demarqués"
- **Cohérence garantie** : Pattern transactionnel pour les opérations distribuées

### Indexation automatique
- Récupération automatique des produits non indexés au démarrage
- Indexation planifiée toutes les 5 minutes
- Mapping Elasticsearch optimisé pour les champs texte et numérique

### API de Recherche
Endpoint principal : `GET /search`

Paramètres supportés :
- `query` : Recherche par nom de produit (recherche partielle)
- `category` : Filtrage par catégorie
- `priceMin` / `priceMax` : Filtrage par plage de prix
- `page` / `size` : Pagination
- `sortBy` / `sortDir` : Tri (par prix ou catégorie)

### Exemples d'utilisation

```bash
# Recherche tous les produits
GET http://localhost:8081/search

# Recherche par nom
GET http://localhost:8081/search?query=Ordinateur

# Recherche par catégorie
GET http://localhost:8081/search?category=Electronique

# Recherche combinée avec pagination
GET http://localhost:8081/search?query=Smartphone&category=Electronique&page=0&size=10

# Recherche par prix avec tri
GET http://localhost:8081/search?priceMin=100&priceMax=1000&sortBy=price&sortDir=asc
```

### Indexation manuelle
- `POST /index/all` : Force l'indexation de tous les produits non indexés

## Mock Catalogue Service

### Pourquoi un mock ?

Le service catalogue original n'ayant pas été fourni par l'autre binôme, nous avons implémenté un service mock pour :
- Démontrer l'intégration complète du système
- Permettre les tests d'indexation automatique
- Montrer la communication inter-services

### Fonctionnalités du mock
- Base de données H2 en mémoire avec 4 produits de test
- API REST compatible avec le service catalogue réel
- Gestion des flags d'indexation pour éviter les doublons

### Endpoints du mock
- `GET /products/not-indexed` : Liste des IDs non indexés
- `GET /products/{id}` : Détails d'un produit
- `POST /products/mark-indexed` : Marquage des produits comme indexés

## Technologies utilisées

- **Spring Boot 3.2.0** : Framework principal
- **Elasticsearch 8.10.0** : Moteur de recherche
- **Spring Data Elasticsearch** : Intégration avec Elasticsearch
- **Docker & Docker Compose** : Conteneurisation
- **H2 Database** : Base de données mock
- **Maven** : Gestion des dépendances

## Configuration

### Variables d'environnement
- `CATALOG_BASE_URL` : URL du service catalogue (défaut: http://mock-catalogue:8080)
- `SPRING_ELASTICSEARCH_URIS` : URL d'Elasticsearch (défaut: http://elasticsearch:9200)

### Mapping Elasticsearch
```json
{
  "mappings": {
    "properties": {
      "name": {"type": "text", "analyzer": "standard"},
      "description": {"type": "text", "analyzer": "standard"},
      "price": {"type": "double"},
      "category": {"type": "keyword"}
    }
  }
}
```

## Démarrage du système

### Prérequis
- Docker et Docker Compose installés
- Java 17+
- Maven 3.8+

### Commandes de démarrage
```bash
# Depuis le dossier search-service
cd search-service

# Compilation
mvn clean package

# Démarrage des services
docker-compose up --build -d

# Vérification des logs
docker-compose logs search-service
```

### Tests

Le système démarre avec 4 produits de test automatiquement indexés :
1. Ordinateur Portable - Électronique - 999.99€
2. Smartphone - Électronique - 799.99€
3. Livre Java - Livres - 29.99€
4. Cafétière - Maison - 149.99€

## Gestion des erreurs

- **Retry mechanism** : Tentatives automatiques en cas d'échec d'indexation
- **Logs détaillés** : Suivi des opérations d'indexation et recherche
- **Gestion des conflits** : Marquage des produits pour éviter les doublons

## Sécurité et performance

- **Health checks** : Vérification de la disponibilité d'Elasticsearch
- **Limites de pagination** : Protection contre les requêtes trop larges
- **Validation des paramètres** : Sanitisation des entrées utilisateur

## Auteurs

- Sarra Tlili
- Aycha Chouchene


