package com.example.searchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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