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
    basePackages = "com.example.dms.thies.repository",
    entityManagerFactoryRef = "thiesEntityManagerFactory",
    transactionManagerRef = "thiesTransactionManager"
)
public class ThiesDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.thies")
    public DataSource thiesDataSource() {
        return org.springframework.boot.jdbc.DataSourceBuilder.create().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean thiesEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("thiesDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.example.dms.model")
                .persistenceUnit("thiesPU")
                .build();
    }

    @Bean
    public PlatformTransactionManager thiesTransactionManager(
            @Qualifier("thiesEntityManagerFactory") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
