package com.recommendation.config;

import com.recommendation.ann.ANNClient;
import com.recommendation.ann.impl.MilvusANNClientImpl;
import com.recommendation.model.ModelServingClient;
import com.recommendation.model.impl.StubModelServingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean
    public MilvusANNClientImpl milvusANNClientImpl(@Value("${milvus.host:milvus}") String host,
                                                   @Value("${milvus.port:19530}") int port) {
        return new MilvusANNClientImpl(host, port);
    }

    @Bean
    public ModelServingClient modelServingClient() {
        return new StubModelServingClient();
    }
}
