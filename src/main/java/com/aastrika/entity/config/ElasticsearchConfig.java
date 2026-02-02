package com.aastrika.entity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.aastrika.entity.repository.es")
public class ElasticsearchConfig {
}
