#!/bin/bash

# Script d'arrêt pour le microservice Order Service
# Ce script arrête proprement tous les conteneurs Docker

set -e

echo "🛑 Arrêt du microservice Order Service..."

# Fonction pour afficher l'aide
show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help         Afficher cette aide"
    echo "  -v, --volumes      Supprimer aussi les volumes (données)"
    echo "  -f, --force        Forcer l'arrêt (kill) des conteneurs"
    echo "  -c, --clean        Nettoyage complet (conteneurs + volumes + images)"
    echo ""
    echo "Exemples:"
    echo "  $0                 # Arrêt normal"
    echo "  $0 -v              # Arrêt + suppression des volumes"
    echo "  $0 -c              # Nettoyage complet"
}

# Arrêter les conteneurs
stop_containers() {
    echo "🛑 Arrêt des conteneurs..."
    if [ "$FORCE_MODE" = true ]; then
        docker-compose kill
    else
        docker-compose down
    fi
}

# Supprimer les volumes
remove_volumes() {
    echo "🗑️  Suppression des volumes..."
    docker-compose down -v --remove-orphans
}

# Nettoyage complet
clean_everything() {
    echo "🧹 Nettoyage complet..."
    
    # Arrêter tous les conteneurs
    docker-compose down -v --remove-orphans
    
    # Supprimer les images construites
    echo "🗑️  Suppression des images..."
    docker images | grep "order-service" | awk '{print $3}' | xargs -r docker rmi -f
    docker images | grep "mock-services" | awk '{print $3}' | xargs -r docker rmi -f
    
    # Nettoyer le système
    docker system prune -f
    
    echo "✅ Nettoyage terminé!"
}

# Afficher les logs avant l'arrêt
show_logs() {
    if [ "$SHOW_LOGS" = true ]; then
        echo ""
        echo "📋 Derniers logs du service Order :"
        echo "========================================"
        docker-compose logs --tail=50 order-service
        echo ""
        echo "📋 Derniers logs des services mock :"
        echo "========================================"
        docker-compose logs --tail=50 mock-services
        echo ""
    fi
}

# Gestion des paramètres
VOLUMES_MODE=false
FORCE_MODE=false
CLEAN_MODE=false
SHOW_LOGS=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--volumes)
            VOLUMES_MODE=true
            shift
            ;;
        -f|--force)
            FORCE_MODE=true
            shift
            ;;
        -c|--clean)
            CLEAN_MODE=true
            shift
            ;;
        -l|--logs)
            SHOW_LOGS=true
            shift
            ;;
        *)
            echo "❌ Paramètre inconnu: $1"
            show_help
            exit 1
            ;;
    esac
done

# Afficher les logs si demandé
if [ "$SHOW_LOGS" = true ]; then
    show_logs
fi

# Effectuer les actions demandées
if [ "$CLEAN_MODE" = true ]; then
    clean_everything
elif [ "$VOLUMES_MODE" = true ]; then
    remove_volumes
else
    stop_containers
fi

echo ""
echo "✅ Arrêt terminé!"

if [ "$CLEAN_MODE" = true ]; then
    echo "🧹 Environnement complètement nettoyé."
elif [ "$VOLUMES_MODE" = true ]; then
    echo "🗑️  Volumes supprimés (les données sont perdues)."
else
    echo "📦 Conteneurs arrêtés (les données sont conservées)."
fi

echo ""
echo "💡 Pour redémarrer : ./start.sh"
