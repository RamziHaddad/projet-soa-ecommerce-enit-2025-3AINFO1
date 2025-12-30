package com.recommendation.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Gradient Boosting Ranker (Lightweight XGBoost-like implementation)
 * Utilise des arbres de décision pour le ranking
 */
@Slf4j
@Service
public class GradientBoostingRanker {
    
    private static final int NUM_TREES = 50;
    private static final int MAX_DEPTH = 6;
    private static final double LEARNING_RATE = 0.1;
    private static final double MIN_SPLIT_GAIN = 0.01;
    
    private List<DecisionTree> trees = new ArrayList<>();
    private boolean isTrained = false;
    
    /**
     * Entraîne le modèle sur des données (features, labels)
     */
    public void train(List<double[]> features, List<Double> labels) {
        log.info("Training Gradient Boosting with {} trees", NUM_TREES);
        
        int n = features.size();
        double[] predictions = new double[n];
        Arrays.fill(predictions, 0.0);
        
        // Initialize with mean
        double meanLabel = labels.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Arrays.fill(predictions, meanLabel);
        
        for (int t = 0; t < NUM_TREES; t++) {
            // Compute residuals (gradients)
            double[] residuals = new double[n];
            for (int i = 0; i < n; i++) {
                residuals[i] = labels.get(i) - predictions[i];
            }
            
            // Fit tree to residuals
            DecisionTree tree = new DecisionTree(MAX_DEPTH, MIN_SPLIT_GAIN);
            tree.fit(features, residuals);
            trees.add(tree);
            
            // Update predictions
            for (int i = 0; i < n; i++) {
                predictions[i] += LEARNING_RATE * tree.predict(features.get(i));
            }
            
            // Compute loss
            if (t % 10 == 0) {
                double loss = computeMSE(predictions, labels);
                log.debug("Tree {}: MSE = {}", t, loss);
            }
        }
        
        isTrained = true;
        log.info("Gradient Boosting training completed");
    }
    
    /**
     * Prédit le score pour des features
     */
    public double predict(double[] features) {
        if (!isTrained || trees.isEmpty()) {
            // Fallback: weighted sum of features
            return simpleWeightedScore(features);
        }
        
        double score = 0.0;
        for (DecisionTree tree : trees) {
            score += LEARNING_RATE * tree.predict(features);
        }
        return score;
    }
    
    /**
     * Fallback: scoring basé sur poids simples
     */
    private double simpleWeightedScore(double[] features) {
        // Feature weights (à ajuster selon l'importance)
        double[] weights = {
            0.3,  // ANN similarity
            0.15, // Trending score
            0.1,  // Product popularity
            0.1,  // Product rating
            0.05, // Price affinity
            0.05, // Category match
            0.05, // Temporal boost
            0.05, // User activity
            0.05, // Seasonal boost
            0.05, // Cross-feature
            0.05  // Experiment boost
        };
        
        double score = 0.0;
        for (int i = 0; i < Math.min(features.length, weights.length); i++) {
            score += features[i] * weights[i];
        }
        return score;
    }
    
    /**
     * Compute Mean Squared Error
     */
    private double computeMSE(double[] predictions, List<Double> labels) {
        double mse = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            double error = predictions[i] - labels.get(i);
            mse += error * error;
        }
        return mse / predictions.length;
    }
    
    /**
     * Decision Tree pour Gradient Boosting
     */
    private static class DecisionTree {
        private final int maxDepth;
        private final double minSplitGain;
        private Node root;
        
        public DecisionTree(int maxDepth, double minSplitGain) {
            this.maxDepth = maxDepth;
            this.minSplitGain = minSplitGain;
        }
        
        public void fit(List<double[]> features, double[] labels) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < features.size(); i++) {
                indices.add(i);
            }
            root = buildTree(features, labels, indices, 0);
        }
        
        private Node buildTree(List<double[]> features, double[] labels, List<Integer> indices, int depth) {
            if (indices.isEmpty() || depth >= maxDepth) {
                return new Leaf(computeMean(labels, indices));
            }
            
            // Find best split
            Split bestSplit = findBestSplit(features, labels, indices);
            
            if (bestSplit == null || bestSplit.gain < minSplitGain) {
                return new Leaf(computeMean(labels, indices));
            }
            
            // Split data
            List<Integer> leftIndices = new ArrayList<>();
            List<Integer> rightIndices = new ArrayList<>();
            
            for (int idx : indices) {
                if (features.get(idx)[bestSplit.featureIdx] <= bestSplit.threshold) {
                    leftIndices.add(idx);
                } else {
                    rightIndices.add(idx);
                }
            }
            
            // Recursively build subtrees
            Node left = buildTree(features, labels, leftIndices, depth + 1);
            Node right = buildTree(features, labels, rightIndices, depth + 1);
            
            return new InternalNode(bestSplit.featureIdx, bestSplit.threshold, left, right);
        }
        
        private Split findBestSplit(List<double[]> features, double[] labels, List<Integer> indices) {
            if (indices.size() < 2) return null;
            
            int numFeatures = features.get(0).length;
            Split bestSplit = null;
            double bestGain = 0.0;
            
            double parentVariance = computeVariance(labels, indices);
            
            for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
                // Get unique values for this feature
                Set<Double> uniqueValues = new HashSet<>();
                for (int idx : indices) {
                    uniqueValues.add(features.get(idx)[featureIdx]);
                }
                
                for (double threshold : uniqueValues) {
                    List<Integer> left = new ArrayList<>();
                    List<Integer> right = new ArrayList<>();
                    
                    for (int idx : indices) {
                        if (features.get(idx)[featureIdx] <= threshold) {
                            left.add(idx);
                        } else {
                            right.add(idx);
                        }
                    }
                    
                    if (left.isEmpty() || right.isEmpty()) continue;
                    
                    double leftVariance = computeVariance(labels, left);
                    double rightVariance = computeVariance(labels, right);
                    
                    double weightedVariance = (left.size() * leftVariance + right.size() * rightVariance) / indices.size();
                    double gain = parentVariance - weightedVariance;
                    
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestSplit = new Split(featureIdx, threshold, gain);
                    }
                }
            }
            
            return bestSplit;
        }
        
        private double computeMean(double[] labels, List<Integer> indices) {
            if (indices.isEmpty()) return 0.0;
            double sum = 0.0;
            for (int idx : indices) {
                sum += labels[idx];
            }
            return sum / indices.size();
        }
        
        private double computeVariance(double[] labels, List<Integer> indices) {
            if (indices.isEmpty()) return 0.0;
            double mean = computeMean(labels, indices);
            double variance = 0.0;
            for (int idx : indices) {
                double diff = labels[idx] - mean;
                variance += diff * diff;
            }
            return variance / indices.size();
        }
        
        public double predict(double[] features) {
            return root.predict(features);
        }
        
        private interface Node {
            double predict(double[] features);
        }
        
        private static class Leaf implements Node {
            private final double value;
            
            public Leaf(double value) {
                this.value = value;
            }
            
            @Override
            public double predict(double[] features) {
                return value;
            }
        }
        
        private static class InternalNode implements Node {
            private final int featureIdx;
            private final double threshold;
            private final Node left;
            private final Node right;
            
            public InternalNode(int featureIdx, double threshold, Node left, Node right) {
                this.featureIdx = featureIdx;
                this.threshold = threshold;
                this.left = left;
                this.right = right;
            }
            
            @Override
            public double predict(double[] features) {
                if (features[featureIdx] <= threshold) {
                    return left.predict(features);
                } else {
                    return right.predict(features);
                }
            }
        }
        
        private static class Split {
            final int featureIdx;
            final double threshold;
            final double gain;
            
            public Split(int featureIdx, double threshold, double gain) {
                this.featureIdx = featureIdx;
                this.threshold = threshold;
                this.gain = gain;
            }
        }
    }
}
