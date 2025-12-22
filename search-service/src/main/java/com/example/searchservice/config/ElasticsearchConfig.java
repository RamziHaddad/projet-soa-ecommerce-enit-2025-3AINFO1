package com.example.searchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.searchservice.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @PostConstruct
    public void createIndexWithMapping() {
        try {
            String indexName = "products";
            // Vérifier si l'index existe
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                // Créer l'index avec le mapping défini
                elasticsearchClient.indices().create(c -> c
                    .index(indexName)
                    .mappings(m -> m
                        .properties("name", p -> p.text(t -> t))
                        .properties("description", p -> p.text(t -> t))
                        .properties("price", p -> p.double_(d -> d))
                        .properties("category", p -> p.keyword(k -> k))
                    )
                );
                System.out.println("Index 'products' créé avec mapping.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de l'index: " + e.getMessage());
        }
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo("elasticsearch:9200")
            .withConnectTimeout(5000)
            .withSocketTimeout(3000)
            .build();
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations(ElasticsearchClient elasticsearchClient) {
        return retryTemplate.execute(context -> 
            new ElasticsearchTemplate(elasticsearchClient)
        );
    }
}
