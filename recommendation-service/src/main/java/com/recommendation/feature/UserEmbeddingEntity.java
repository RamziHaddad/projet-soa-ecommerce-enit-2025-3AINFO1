package com.recommendation.feature;

import jakarta.persistence.*;

@Entity
@Table(name = "user_embeddings")
public class UserEmbeddingEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Lob
    @Column(name = "embedding_json", nullable = false)
    private String embeddingJson;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmbeddingJson() { return embeddingJson; }
    public void setEmbeddingJson(String embeddingJson) { this.embeddingJson = embeddingJson; }
}
