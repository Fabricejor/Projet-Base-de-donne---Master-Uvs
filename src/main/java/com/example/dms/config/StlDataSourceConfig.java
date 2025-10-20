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
    basePackages = "com.example.dms.stl.repository",
    entityManagerFactoryRef = "stlEntityManagerFactory",
    transactionManagerRef = "stlTransactionManager"
)
public class StlDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.stl")
    public DataSource stlDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean stlEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("stlDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.example.dms.model")
                .persistenceUnit("stlPU")
                .build();
    }

    @Bean
    public PlatformTransactionManager stlTransactionManager(
            @Qualifier("stlEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
