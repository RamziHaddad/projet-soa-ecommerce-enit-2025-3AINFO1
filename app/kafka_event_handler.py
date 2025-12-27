"""
Service de gestion des événements Kafka pour le système de recommandation
Ce module consomme les événements des autres microservices et met à jour le Feature Store
"""

import json
import logging
from typing import Dict, Any, Optional
from datetime import datetime
from confluent_kafka import Consumer, Producer, KafkaError
from app.feature_store import FeatureStore
from app.models import UserEvent, ProductEvent, OrderEvent

# Configuration du logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class KafkaEventHandler:
    """
    Handler pour les événements Kafka provenant des autres microservices
    Topics traités :
    - user.events : Événements utilisateur (views, clicks, etc.)
    - order.events : Événements de commande
    - catalog.events : Événements du catalogue produits
    """
    
    def __init__(self, bootstrap_servers: str = 'localhost:9092', group_id: str = 'recommendation-group'):
        """
        Initialise le handler Kafka
        
        Args:
            bootstrap_servers: Serveurs Kafka (format: host:port)
            group_id: ID du groupe de consommateurs
        """
        self.bootstrap_servers = bootstrap_servers
        self.group_id = group_id
        self.feature_store = FeatureStore()
        
        # Configuration du consommateur Kafka
        self.consumer_config = {
            'bootstrap.servers': bootstrap_servers,
            'group.id': group_id,
            'auto.offset.reset': 'earliest',
            'enable.auto.commit': False
        }
        
        # Configuration du producteur Kafka
        self.producer_config = {
            'bootstrap.servers': bootstrap_servers,
            'client.id': 'recommendation-service'
        }
        
        self.consumer = None
        self.producer = None
        self.running = False
        
        # Topics à écouter
        self.topics = ['user.events', 'order.events', 'catalog.events']
        
        # Mappings des handlers d'événements
        self.event_handlers = {
            'user.events': self._handle_user_event,
            'order.events': self._handle_order_event,
            'catalog.events': self._handle_catalog_event
        }
        
        logger.info(f"KafkaEventHandler initialisé pour les topics: {self.topics}")
    
    def start(self):
        """
        Démarre la consommation des événements Kafka
        """
        try:
            # Initialiser le consommateur
            self.consumer = Consumer(self.consumer_config)
            self.consumer.subscribe(self.topics)
            
            # Initialiser le producteur
            self.producer = Producer(self.producer_config)
            
            self.running = True
            logger.info("Démarrage de la consommation Kafka...")
            
            # Boucle de consommation
            while self.running:
                msg = self.consumer.poll(timeout=1.0)
                
                if msg is None:
                    continue
                
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        logger.debug(f"Fin de partition atteinte: {msg.topic()} [{msg.partition()}]")
                    else:
                        logger.error(f"Erreur Kafka: {msg.error()}")
                    continue
                
                # Traiter le message
                self._process_message(msg)
                
                # Commit manuel de l'offset
                self.consumer.commit(asynchronous=False)
                
        except KeyboardInterrupt:
            logger.info("Arrêt demandé par l'utilisateur")
        except Exception as e:
            logger.error(f"Erreur dans la boucle Kafka: {e}")
        finally:
            self.stop()
    
    def stop(self):
        """
        Arrête le consommateur Kafka
        """
        self.running = False
        if self.consumer:
            self.consumer.close()
        logger.info("Consommateur Kafka arrêté")
    
    def _process_message(self, msg):
        """
        Traite un message Kafka reçu
        
        Args:
            msg: Message Kafka
        """
        try:
            # Décoder le message
            message_data = json.loads(msg.value().decode('utf-8'))
            topic = msg.topic()
            partition = msg.partition()
            offset = msg.offset()
            
            logger.debug(f"Message reçu - Topic: {topic}, Partition: {partition}, Offset: {offset}")
            
            # Appeler le handler approprié
            handler = self.event_handlers.get(topic)
            if handler:
                handler(message_data)
            else:
                logger.warning(f"Aucun handler pour le topic: {topic}")
                
        except json.JSONDecodeError as e:
            logger.error(f"Erreur de décodage JSON: {e}")
        except Exception as e:
            logger.error(f"Erreur de traitement du message: {e}")
    
    def _handle_user_event(self, event_data: Dict[str, Any]):
        """
        Traite un événement utilisateur
        
        Types d'événements:
        - PRODUCT_VIEW: Consultation d'un produit
        - PRODUCT_CLICK: Clic sur un produit
        - SEARCH: Recherche utilisateur
        - ADD_TO_CART: Ajout au panier
        - REMOVE_FROM_CART: Retrait du panier
        - LOGIN: Connexion utilisateur
        """
        try:
            event_type = event_data.get('event_type')
            user_id = event_data.get('user_id')
            timestamp = event_data.get('timestamp', datetime.now().isoformat())
            
            if not all([event_type, user_id]):
                logger.warning(f"Événement utilisateur incomplet: {event_data}")
                return
            
            logger.info(f"Événement utilisateur: {event_type} pour user: {user_id}")
            
            # Créer l'objet UserEvent
            user_event = UserEvent(
                user_id=user_id,
                event_type=event_type,
                timestamp=timestamp,
                product_id=event_data.get('product_id'),
                session_id=event_data.get('session_id'),
                metadata=event_data.get('metadata', {})
            )
            
            # Mettre à jour le Feature Store
            self.feature_store.update_user_features(user_event)
            
            # Envoyer un événement de confirmation
            self._send_recommendation_event({
                'type': 'USER_EVENT_PROCESSED',
                'user_id': user_id,
                'event_type': event_type,
                'timestamp': datetime.now().isoformat()
            })
            
        except Exception as e:
            logger.error(f"Erreur lors du traitement de l'événement utilisateur: {e}")
    
    def _handle_order_event(self, event_data: Dict[str, Any]):
        """
        Traite un événement de commande
        
        Types d'événements:
        - ORDER_CREATED: Commande créée
        - ORDER_COMPLETED: Commande terminée
        - ORDER_CANCELLED: Commande annulée
        - PAYMENT_RECEIVED: Paiement reçu
        """
        try:
            event_type = event_data.get('event_type')
            order_id = event_data.get('order_id')
            user_id = event_data.get('user_id')
            
            if not all([event_type, order_id, user_id]):
                logger.warning(f"Événement commande incomplet: {event_data}")
                return
            
            logger.info(f"Événement commande: {event_type}, Order: {order_id}, User: {user_id}")
            
            # Créer l'objet OrderEvent
            order_event = OrderEvent(
                order_id=order_id,
                user_id=user_id,
                event_type=event_type,
                timestamp=event_data.get('timestamp', datetime.now().isoformat()),
                items=event_data.get('items', []),
                total_amount=event_data.get('total_amount'),
                metadata=event_data.get('metadata', {})
            )
            
            # Mettre à jour le Feature Store avec les données d'achat
            self.feature_store.update_purchase_history(order_event)
            
            # Si c'est un achat complet, mettre à jour les recommandations
            if event_type == 'ORDER_COMPLETED':
                self._trigger_recommendation_update(user_id, order_event.items)
                
        except Exception as e:
            logger.error(f"Erreur lors du traitement de l'événement commande: {e}")
    
    def _handle_catalog_event(self, event_data: Dict[str, Any]):
        """
        Traite un événement du catalogue
        
        Types d'événements:
        - PRODUCT_CREATED: Produit créé
        - PRODUCT_UPDATED: Produit mis à jour
        - PRODUCT_DELETED: Produit supprimé
        - PRICE_CHANGED: Prix modifié
        - STOCK_UPDATED: Stock mis à jour
        """
        try:
            event_type = event_data.get('event_type')
            product_id = event_data.get('product_id')
            
            if not all([event_type, product_id]):
                logger.warning(f"Événement catalogue incomplet: {event_data}")
                return
            
            logger.info(f"Événement catalogue: {event_type} pour produit: {product_id}")
            
            # Créer l'objet ProductEvent
            product_event = ProductEvent(
                product_id=product_id,
                event_type=event_type,
                timestamp=event_data.get('timestamp', datetime.now().isoformat()),
                product_data=event_data.get('product_data', {}),
                metadata=event_data.get('metadata', {})
            )
            
            # Mettre à jour le Feature Store
            self.feature_store.update_product_features(product_event)
            
        except Exception as e:
            logger.error(f"Erreur lors du traitement de l'événement catalogue: {e}")
    
    def _trigger_recommendation_update(self, user_id: str, purchased_items: list):
        """
        Déclenche une mise à jour des recommandations après un achat
        
        Args:
            user_id: ID de l'utilisateur
            purchased_items: Liste des produits achetés
        """
        try:
            # Envoyer un événement pour mettre à jour les recommandations
            event_data = {
                'type': 'RECOMMENDATION_UPDATE_TRIGGERED',
                'user_id': user_id,
                'purchased_items': purchased_items,
                'timestamp': datetime.now().isoformat(),
                'reason': 'post_purchase'
            }
            
            self._send_recommendation_event(event_data)
            
            logger.info(f"Mise à jour des recommandations déclenchée pour user: {user_id}")
            
        except Exception as e:
            logger.error(f"Erreur lors du déclenchement de mise à jour: {e}")
    
    def _send_recommendation_event(self, event_data: Dict[str, Any]):
        """
        Envoie un événement au topic de recommandation
        
        Args:
            event_data: Données de l'événement
        """
        try:
            topic = 'recommendation.events'
            
            # Sérialiser les données
            message_value = json.dumps(event_data).encode('utf-8')
            
            # Produire le message
            self.producer.produce(
                topic=topic,
                value=message_value,
                callback=self._delivery_report
            )
            
            # Flusher pour s'assurer que le message est envoyé
            self.producer.flush()
            
            logger.debug(f"Événement envoyé au topic {topic}: {event_data.get('type')}")
            
        except Exception as e:
            logger.error(f"Erreur lors de l'envoi de l'événement: {e}")
    
    def _delivery_report(self, err, msg):
        """
        Callback pour les rapports de livraison des messages
        
        Args:
            err: Erreur éventuelle
            msg: Message produit
        """
        if err is not None:
            logger.error(f"Échec de livraison du message: {err}")
        else:
            logger.debug(f"Message livré à {msg.topic()} [{msg.partition()}]")


def main():
    """
    Fonction principale pour démarrer le service Kafka
    """
    import argparse
    
    parser = argparse.ArgumentParser(description='Service de gestion des événements Kafka')
    parser.add_argument('--bootstrap-servers', default='localhost:9092',
                       help='Serveurs Kafka bootstrap (default: localhost:9092)')
    parser.add_argument('--group-id', default='recommendation-group',
                       help='ID du groupe de consommateurs (default: recommendation-group)')
    
    args = parser.parse_args()
    
    # Créer et démarrer le handler
    handler = KafkaEventHandler(
        bootstrap_servers=args.bootstrap_servers,
        group_id=args.group_id
    )
    
    try:
        handler.start()
    except KeyboardInterrupt:
        logger.info("Service arrêté par l'utilisateur")
    except Exception as e:
        logger.error(f"Erreur fatale: {e}")
        raise


if __name__ == "__main__":
    main()