#!/bin/bash

# Script de démarrage rapide pour le microservice Order Service
# Ce script lance l'ensemble de l'infrastructure Docker

set -e

echo "🚀 Démarrage du microservice Order Service avec services mock..."

# Fonction pour afficher l'aide
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help         Afficher cette aide"
    echo "  -c, --clean        Nettoyer les conteneurs et volumes avant le démarrage"
    echo "  -d, --detached     Lancer en mode détaché (en arrière-plan)"
    echo "  -m, --mock-only    Lancer uniquement les services mock"
    echo "  -o, --order-only   Lancer uniquement le service order"
    echo "  --no-monitoring    Désactiver le monitoring (Prometheus/Grafana)"
    echo ""
    echo "Exemples:"
    echo "  $0                 # Démarrage complet"
    echo "  $0 -c -d           # Nettoyage puis démarrage en arrière-plan"
    echo "  $0 -m              # Services mock uniquement"
}

# Vérifier que Docker est installé et en cours d'exécution
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker n'est pas installé. Veuillez installer Docker."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        echo "❌ Docker n'est pas en cours d'exécution. Démarrez Docker."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo "❌ Docker Compose n'est pas installé. Veuillez installer Docker Compose."
        exit 1
    fi
}

# Nettoyer les conteneurs et volumes
clean_environment() {
    echo "🧹 Nettoyage de l'environnement..."
    docker-compose down -v --remove-orphans
    docker system prune -f
}

# Construire les images
build_images() {
    echo "🔨 Construction des images Docker..."
    docker-compose build --no-cache
}

# Fonction pour démarrer les services
start_services() {
    local compose_args=""
    
    if [ "$DETAILED_MODE" = true ]; then
        compose_args="-d"
    fi
    
    echo "🐳 Lancement des services..."
    
    if [ "$MOCK_ONLY" = true ]; then
        echo "   📦 Services mock uniquement"
        docker-compose up $compose_args postgres mock-services
    elif [ "$ORDER_ONLY" = true ]; then
        echo "   📦 Service Order uniquement"
        docker-compose up $compose_args postgres order-service
    else
        echo "   📦 Infrastructure complète"
        docker-compose up $compose_args
    fi
}

# Attendre que les services soient prêts
wait_for_services() {
    echo "⏳ Attente que les services soient prêts..."
    
    # Attendre PostgreSQL
    echo "   🔄 Attente de PostgreSQL..."
    until docker-compose exec -T postgres pg_isready -U postgres; do
        sleep 2
    done
    
    # Attendre le service Order
    echo "   🔄 Attente du service Order..."
    until curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; do
        sleep 5
    done
    
    # Attendre les services mock
    echo "   🔄 Attente des services mock..."
    until curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; do
        sleep 3
    done
    
    echo "✅ Tous les services sont prêts!"
}

# Afficher les informations de connexion
show_connection_info() {
    echo ""
    echo "🎉 Infrastructure démarrée avec succès!"
    echo ""
    echo "📊 Services disponibles:"
    echo "   🌐 Order Service:          http://localhost:8080"
    echo "   🗄️  PostgreSQL:             localhost:5432 (postgres/postgres)"
    echo "   📦 Services Mock:"
    echo "      - Cart Service:         http://localhost:8081/api/cart"
    echo "      - Inventory Service:    http://localhost:8081/api/inventory"
    echo "      - Payment Service:      http://localhost:8081/api/payment"
    echo "      - Shipping Service:     http://localhost:8081/api/shipping"
    echo "      - Communication Service: http://localhost:8081/api/communication"
    echo ""
    echo "🔍 Monitoring (optionnel):"
    if [ "$NO_MONITORING" != true ]; then
        echo "   📈 Prometheus:            http://localhost:9090"
        echo "   📊 Grafana:               http://localhost:3000 (admin/admin)"
    fi
    echo ""
    echo "🧪 Tests:"
    echo "   📋 Collection Postman:     ./docs/postman/OrderService.postman_collection.json"
    echo "   🔧 API Documentation:      http://localhost:8080/swagger-ui.html"
    echo ""
    echo "📝 Logs en temps réel: docker-compose logs -f"
    echo "🛑 Arrêter les services: ./stop.sh"
}

# Gestion des paramètres
CLEAN_MODE=false
DETAILED_MODE=false
MOCK_ONLY=false
ORDER_ONLY=false
NO_MONITORING=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -c|--clean)
            CLEAN_MODE=true
            shift
            ;;
        -d|--detached)
            DETAILED_MODE=true
            shift
            ;;
        -m|--mock-only)
            MOCK_ONLY=true
            shift
            ;;
        -o|--order-only)
            ORDER_ONLY=true
            shift
            ;;
        --no-monitoring)
            NO_MONITORING=true
            shift
            ;;
        *)
            echo "❌ Paramètre inconnu: $1"
            show_help
            exit 1
            ;;
    esac
done

# Vérifications préliminaires
check_docker

# Nettoyage si demandé
if [ "$CLEAN_MODE" = true ]; then
    clean_environment
fi

# Construction des images
build_images

# Démarrage des services
start_services

# Attendre que les services soient prêts (sauf si mode détaché)
if [ "$DETAILED_MODE" != true ]; then
    wait_for_services
fi

# Afficher les informations de connexion
show_connection_info
