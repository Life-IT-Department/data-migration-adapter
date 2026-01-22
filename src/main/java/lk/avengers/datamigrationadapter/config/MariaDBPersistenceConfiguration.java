package lk.avengers.datamigrationadapter.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableJpaRepositories(
        basePackages = "lk.avengers.datamigrationadapter.repository.mariadb",
        entityManagerFactoryRef = "mariaDBEntityManagerFactory",
        transactionManagerRef = "mariaDBTransactionManager"
)
@EntityScan("lk.avengers.datamigrationadapter.entity.mariadb")
public class MariaDBPersistenceConfiguration {

    private final Environment env;

    /* ============================================================
       DATASOURCE
       ============================================================ */

    @Bean
    @ConfigurationProperties(prefix = "spring.mariadb-datasource")
    public DataSource mariaDBDataSource() {
        return DataSourceBuilder.create().build();
    }

    /* ============================================================
       ENTITY MANAGER FACTORY
       ============================================================ */

    @Bean(name = "mariaDBEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mariaDBManagerFactory(
            @Qualifier("mariaDBDataSource") DataSource dataSource
    ) {

        var vendorAdapter = new HibernateJpaVendorAdapter();

        var properties = Map.<String, Object>of(
                "hibernate.hbm2ddl.auto",
                env.getProperty("hibernate.hbm2ddl.auto", "none"),

                "hibernate.dialect",
                "org.hibernate.dialect.MySQLDialect"
        );

        var emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("lk.avengers.datamigrationadapter.entity.mariadb");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);

        return emf;
    }

    /* ============================================================
       TRANSACTION MANAGER
       ============================================================ */

    @Primary
    @Bean(name = "mariaDBTransactionManager")
    public PlatformTransactionManager mariaDBTransactionManager(
            @Qualifier("mariaDBEntityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
