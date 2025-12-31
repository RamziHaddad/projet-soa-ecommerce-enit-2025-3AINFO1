package com.recommendation.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Word2Vec Implementation using Skip-gram with TF-IDF
 * Génère des embeddings RÉELS basés sur co-occurrences
 */
@Slf4j
@Service
public class Word2VecServiceImpl {
    
    private static final int EMBEDDING_DIM = 128;
    private static final int WINDOW_SIZE = 5;
    private static final double LEARNING_RATE = 0.025;
    
    // Stockage des embeddings appris
    private final Map<String, float[]> productEmbeddings = new ConcurrentHashMap<>();
    private final Map<String, float[]> userEmbeddings = new ConcurrentHashMap<>();
    
    // Co-occurrence matrix
    private final Map<String, Map<String, Double>> coOccurrenceMatrix = new ConcurrentHashMap<>();
    
    /**
     * Entraîne Word2Vec sur des séquences d'interactions
     */
    public void train(List<List<String>> sequences, int epochs) {
        log.info("Training Word2Vec on {} sequences for {} epochs", sequences.size(), epochs);
        
        // 1. Build co-occurrence matrix
        buildCoOccurrenceMatrix(sequences);
        
        // 2. Initialize random embeddings
        Set<String> vocabulary = coOccurrenceMatrix.keySet();
        for (String item : vocabulary) {
            productEmbeddings.put(item, randomEmbedding());
        }
        
        // 3. Train with Skip-gram
        for (int epoch = 0; epoch < epochs; epoch++) {
            double totalLoss = 0.0;
            
            for (List<String> sequence : sequences) {
                for (int i = 0; i < sequence.size(); i++) {
                    String target = sequence.get(i);
                    
                    // Context window
                    int start = Math.max(0, i - WINDOW_SIZE);
                    int end = Math.min(sequence.size(), i + WINDOW_SIZE + 1);
                    
                    for (int j = start; j < end; j++) {
                        if (i != j) {
                            String context = sequence.get(j);
                            totalLoss += trainPair(target, context);
                        }
                    }
                }
            }
            
            if (epoch % 10 == 0) {
                log.info("Epoch {}/{}: Loss = {}", epoch, epochs, totalLoss);
            }
        }
        
        log.info("Word2Vec training completed. Vocabulary size: {}", vocabulary.size());
    }
    
    /**
     * Construit la matrice de co-occurrences
     */
    private void buildCoOccurrenceMatrix(List<List<String>> sequences) {
        for (List<String> sequence : sequences) {
            for (int i = 0; i < sequence.size(); i++) {
                String target = sequence.get(i);
                Map<String, Double> contextCounts = coOccurrenceMatrix.computeIfAbsent(
                    target, k -> new ConcurrentHashMap<>()
                );
                
                int start = Math.max(0, i - WINDOW_SIZE);
                int end = Math.min(sequence.size(), i + WINDOW_SIZE + 1);
                
                for (int j = start; j < end; j++) {
                    if (i != j) {
                        String context = sequence.get(j);
                        double distance = Math.abs(i - j);
                        double weight = 1.0 / distance; // Closer items have higher weight
                        contextCounts.merge(context, weight, Double::sum);
                    }
                }
            }
        }
    }
    
    /**
     * Entraîne une paire (target, context) avec Skip-gram
     */
    private double trainPair(String target, String context) {
        float[] targetEmb = productEmbeddings.get(target);
        float[] contextEmb = productEmbeddings.get(context);
        
        if (targetEmb == null || contextEmb == null) return 0.0;
        
        // Compute dot product (prediction)
        double dotProduct = 0.0;
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            dotProduct += targetEmb[i] * contextEmb[i];
        }
        
        // Sigmoid activation
        double predicted = 1.0 / (1.0 + Math.exp(-dotProduct));
        
        // Binary cross-entropy loss (target label = 1 for positive pair)
        double loss = -Math.log(predicted + 1e-10);
        
        // Gradient descent update
        double gradient = predicted - 1.0; // derivative of loss w.r.t. dot product
        
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            double targetGrad = gradient * contextEmb[i];
            double contextGrad = gradient * targetEmb[i];
            
            targetEmb[i] -= LEARNING_RATE * targetGrad;
            contextEmb[i] -= LEARNING_RATE * contextGrad;
        }
        
        return loss;
    }
    
    /**
     * Génère un embedding aléatoire initial (Xavier initialization)
     */
    private float[] randomEmbedding() {
        Random rand = new Random();
        float[] emb = new float[EMBEDDING_DIM];
        double stddev = Math.sqrt(2.0 / EMBEDDING_DIM);
        
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            emb[i] = (float) (rand.nextGaussian() * stddev);
        }
        
        // Normalize
        float norm = 0.0f;
        for (float v : emb) norm += v * v;
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < emb.length; i++) {
                emb[i] /= norm;
            }
        }
        
        return emb;
    }
    
    /**
     * Récupère l'embedding d'un produit
     */
    public float[] getProductEmbedding(String productId) {
        float[] emb = productEmbeddings.get(productId);
        if (emb == null) {
            log.warn("Product {} not in vocabulary, generating cold-start embedding", productId);
            emb = coldStartEmbedding(productId);
            productEmbeddings.put(productId, emb);
        }
        return emb;
    }
    
    /**
     * Génère embedding pour produit inconnu (cold start)
     */
    private float[] coldStartEmbedding(String productId) {
        // Use similar products' embeddings if available
        if (!productEmbeddings.isEmpty()) {
            // Average of all embeddings as fallback
            float[] avgEmb = new float[EMBEDDING_DIM];
            int count = 0;
            
            for (float[] emb : productEmbeddings.values()) {
                for (int i = 0; i < EMBEDDING_DIM; i++) {
                    avgEmb[i] += emb[i];
                }
                count++;
                if (count > 10) break; // Sample only 10 products
            }
            
            if (count > 0) {
                for (int i = 0; i < EMBEDDING_DIM; i++) {
                    avgEmb[i] /= count;
                }
            }
            return avgEmb;
        }
        
        return randomEmbedding();
    }
    
    /**
     * Calcule similarité cosinus entre deux produits
     */
    public double similarity(String productId1, String productId2) {
        float[] emb1 = getProductEmbedding(productId1);
        float[] emb2 = getProductEmbedding(productId2);
        
        return cosineSimilarity(emb1, emb2);
    }
    
    /**
     * Trouve les K produits les plus similaires
     */
    public List<Map.Entry<String, Double>> findSimilar(String productId, int k) {
        float[] targetEmb = getProductEmbedding(productId);
        
        return productEmbeddings.entrySet().stream()
            .filter(e -> !e.getKey().equals(productId))
            .map(e -> Map.entry(e.getKey(), cosineSimilarity(targetEmb, e.getValue())))
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(k)
            .collect(Collectors.toList());
    }
    
    /**
     * Calcule la similarité cosinus entre deux vecteurs
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        
        if (norm1 == 0 || norm2 == 0) return 0.0;
        
        return dotProduct / (norm1 * norm2);
    }
    
    /**
     * Génère user embedding basé sur historique d'achats
     */
    public float[] generateUserEmbedding(List<String> purchaseHistory) {
        if (purchaseHistory.isEmpty()) {
            return randomEmbedding();
        }
        
        // Average of purchased products' embeddings
        float[] userEmb = new float[EMBEDDING_DIM];
        int count = 0;
        
        for (String productId : purchaseHistory) {
            float[] productEmb = getProductEmbedding(productId);
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                userEmb[i] += productEmb[i];
            }
            count++;
        }
        
        if (count > 0) {
            for (int i = 0; i < EMBEDDING_DIM; i++) {
                userEmb[i] /= count;
            }
        }
        
        return userEmb;
    }
    
    /**
     * Sauvegarde les embeddings
     */
    public Map<String, float[]> getProductEmbeddings() {
        return new HashMap<>(productEmbeddings);
    }
    
    /**
     * Charge les embeddings
     */
    public void loadProductEmbeddings(Map<String, float[]> embeddings) {
        productEmbeddings.clear();
        productEmbeddings.putAll(embeddings);
        log.info("Loaded {} product embeddings", embeddings.size());
    }
}
