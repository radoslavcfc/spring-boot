package com.farm.workers.config;

import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  .NET → Java: CosmosDB Configuration                     ║
 * ║                                                          ║
 * ║  .NET:                                                   ║
 * ║  services.AddDbContext<CosmosContext>(opts =>           ║
 * ║    opts.UseCosmos(endpoint, key, database));             ║
 * ║                                                          ║
 * ║  Java (Spring Data Cosmos):                              ║
 * ║  Extend AbstractCosmosConfiguration + set in properties ║
 * ║                                                          ║
 * ║  Most config is in application.properties:              ║
 * ║  spring.cloud.azure.cosmos.endpoint = ...               ║
 * ║  spring.cloud.azure.cosmos.key = ...                    ║
 * ║  spring.cloud.azure.cosmos.database = farm-workers-db   ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * @EnableCosmosRepositories scans for interfaces extending CosmosRepository
 * ≈ EF Core's AddDbContext which registers all DbSet<T> repositories
 */
@Configuration
@EnableCosmosRepositories(basePackages = "com.farm.workers.repository")
public class CosmosDbConfig extends AbstractCosmosConfiguration {

    @Value("${spring.cloud.azure.cosmos.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    /**
     * CosmosConfig: performance and behavior tuning
     * ≈ CosmosClientOptions in the .NET SDK
     */
    @Bean
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
                .enableQueryMetrics(true)         // Log RU/s usage (great for optimization)
                .queryMetricsEnabled(true)
                .build();
    }
}
