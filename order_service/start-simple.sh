#!/bin/bash

# Script de démarrage simple pour Order Service
# Solution aux problèmes Docker buildx

set -e

echo "🚀 Démarrage simple du Order Service"
echo "=================================="

# Fonction d'aide
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help         Afficher cette aide"
    echo "  -c, --clean        Nettoyer PostgreSQL avant démarrage"
    echo "  -d, --detached     PostgreSQL en arrière-plan"
    echo "  --mock             Lancer les services mock"
    echo "  --build            Compiler l'application avant lancement"
    echo ""
    echo "Exemples:"
    echo "  $0                 # Démarrage normal"
    echo "  $0 -c -d           # Nettoyage + PostgreSQL en arrière-plan"
    echo "  $0 --mock          # Avec services mock"
}

# Vérifications
check_prerequisites() {
    echo "🔍 Vérification des prérequis..."
    
    # Vérifier Java
    if ! command -v java &> /dev/null; then
        echo "❌ Java n'est pas installé"
        exit 1
    fi
    java -version
    
    # Vérifier Maven
    if ! command -v mvn &> /dev/null; then
        echo "❌ Maven n'est pas installé"
        exit 1
    fi
    mvn -version
    
    # Vérifier Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker n'est pas installé"
        exit 1
    fi
    
    echo "✅ Prérequis OK"
}

# Nettoyer PostgreSQL
clean_postgres() {
    echo "🧹 Nettoyage PostgreSQL..."
    docker-compose -f docker-compose.simple.yml down -v --remove-orphans 2>/dev/null || true
    docker system prune -f 2>/dev/null || true
}

# Démarrer PostgreSQL
start_postgres() {
    echo "🗄️  Démarrage de PostgreSQL..."
    
    if [ "$DETACHED_MODE" = true ]; then
        docker-compose -f docker-compose.simple.yml up postgres -d
    else
        docker-compose -f docker-compose.simple.yml up postgres
    fi
}

# Attendre PostgreSQL
wait_for_postgres() {
    echo "⏳ Attente que PostgreSQL soit prêt..."
    
    local retries=30
    local count=0
    
    while [ $count -lt $retries ]; do
        if docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
            echo "✅ PostgreSQL est prêt!"
            return 0
        fi
        
        count=$((count + 1))
        echo "   Tentative $count/$retries..."
        sleep 2
    done
    
    echo "❌ PostgreSQL n'est pas prêt après $retries tentatives"
    return 1
}

# Compiler l'application
build_application() {
    echo "🔨 Compilation de l'application..."
    mvn clean compile -q
}

# Démarrer Order Service
start_order_service() {
    echo "📦 Démarrage du Order Service..."
    
    # Variables d'environnement
    export SPRING_PROFILES_ACTIVE=dev
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/order_db
    export SPRING_DATASOURCE_USERNAME=postgres
    export SPRING_DATASOURCE_PASSWORD=postgres
    export SERVER_PORT=8080
    
    # Configuration des services mock
    if [ "$MOCK_MODE" = true ]; then
        export SERVICES_INVENTORY_URL=http://localhost:8081
        export SERVICES_PAYMENT_URL=http://localhost:8081
        export SERVICES_SHIPPING_URL=http://localhost:8081
        export SERVICES_COMMUNICATION_URL=http://localhost:8081
        echo "🔧 Services mock configurés"
    fi
    
    # Lancer l'application
    mvn spring-boot:run
}

# Démarrer services mock
start_mock_services() {
    echo "🧪 Démarrage des services mock..."
    
    # Variables pour les mock services
    export SPRING_PROFILES_ACTIVE=mock
    export SERVER_PORT=8081
    
    # Ouvrir un nouveau terminal pour les mock services
    if command -v gnome-terminal &> /dev/null; then
        gnome-terminal -- bash -c "export SPRING_PROFILES_ACTIVE=mock; export SERVER_PORT=8081; mvn spring-boot:run; exec bash"
    elif command -v xterm &> /dev/null; then
        xterm -e "export SPRING_PROFILES_ACTIVE=mock; export SERVER_PORT=8081; mvn spring-boot:run; exec bash" &
    else
        echo "⚠️  Impossible d'ouvrir un nouveau terminal pour les mock services"
        echo "   Lancez manuellement dans un autre terminal:"
        echo "   export SPRING_PROFILES_ACTIVE=mock"
        echo "   export SERVER_PORT=8081"
        echo "   mvn spring-boot:run"
    fi
}

# Afficher les informations finales
show_final_info() {
    echo ""
    echo "🎉 Order Service démarré avec succès!"
    echo "=================================="
    echo ""
    echo "📊 Services disponibles:"
    echo "   🌐 Order Service:     http://localhost:8080"
    echo "   🗄️  PostgreSQL:        localhost:5432 (postgres/postgres)"
    
    if [ "$MOCK_MODE" = true ]; then
        echo "   🧪 Services Mock:     http://localhost:8081"
    fi
    
    echo ""
    echo "🔍 Tests rapides:"
    echo "   curl http://localhost:8080/actuator/health"
    echo ""
    echo "📋 Logs en temps réel:"
    echo "   docker-compose -f docker-compose.simple.yml logs -f postgres"
    echo ""
    echo "🛑 Arrêter:"
    echo "   Ctrl+C pour arrêter Order Service"
    echo "   docker-compose -f docker-compose.simple.yml down pour PostgreSQL"
    echo ""
}

# Gestion des paramètres
CLEAN_MODE=false
DETACHED_MODE=false
MOCK_MODE=false
BUILD_MODE=false

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
            DETACHED_MODE=true
            shift
            ;;
        --mock)
            MOCK_MODE=true
            shift
            ;;
        --build)
            BUILD_MODE=true
            shift
            ;;
        *)
            echo "❌ Paramètre inconnu: $1"
            show_help
            exit 1
            ;;
    esac
done

# Exécution principale
check_prerequisites

if [ "$CLEAN_MODE" = true ]; then
    clean_postgres
fi

start_postgres

if [ "$DETACHED_MODE" != true ]; then
    wait_for_postgres
fi

if [ "$BUILD_MODE" = true ]; then
    build_application
fi

if [ "$MOCK_MODE" = true ]; then
    start_mock_services
    sleep 5  # Attendre que les mock services démarrent
fi

start_order_service
