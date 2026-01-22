package lk.avengers.datamigrationadapter.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@RequiredArgsConstructor
@EnableJpaRepositories(
        basePackages = "lk.avengers.datamigrationadapter.repository.postgresql.paymedb",
        entityManagerFactoryRef = "payMeEntityManagerFactory",
        transactionManagerRef = "payMePlatformTransactionManager"
)
public class PayMePersistenceConfiguration {

    /* ============================================================
       DATASOURCE
       ============================================================ */

    @Bean(name = "payMeDataSource")
    @ConfigurationProperties(prefix = "spring.payme-datasource")
    public DataSource payMeDataSource() {
        return DataSourceBuilder.create().build();
    }

    /* ============================================================
       ENTITY MANAGER FACTORY
       ============================================================ */

    @Bean(name = "payMeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean payMeEntityManagerFactory(
            @Qualifier("payMeDataSource") DataSource dataSource
    ) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        Map<String, Object> properties = Map.of(
                "hibernate.hbm2ddl.auto", "update",
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"
        );

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("lk.avengers.datamigrationadapter.entity.postgresql.payme");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);
        emf.setPersistenceUnitName("Payme");

        return emf;
    }

    /* ============================================================
       TRANSACTION MANAGER
       ============================================================ */

    @Bean(name = "payMePlatformTransactionManager")
    public PlatformTransactionManager paymePlatformTransactionManager(
            @Qualifier("payMeEntityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
