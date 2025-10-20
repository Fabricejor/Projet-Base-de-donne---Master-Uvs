package com.example.dms.config;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.dms.dakar.repository",
    entityManagerFactoryRef = "dakarEntityManagerFactory",
    transactionManagerRef = "dakarTransactionManager"
)
public class DakarDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dakar")
    public DataSource dakarDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean dakarEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dakarDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.example.dms.model") // L’entité Vente
                .persistenceUnit("dakarPU")
                .build();
    }

    @Bean
    public PlatformTransactionManager dakarTransactionManager(
            @Qualifier("dakarEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
