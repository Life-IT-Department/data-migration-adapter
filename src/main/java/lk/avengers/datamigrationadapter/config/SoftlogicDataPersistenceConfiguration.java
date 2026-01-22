package lk.avengers.datamigrationadapter.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
        basePackages = "lk.avengers.datamigrationadapter.repository.softlogicdb",
        entityManagerFactoryRef = "softlogicEntityManagerFactory",
        transactionManagerRef = "softlogicPlatformTransactionManager"
)
public class SoftlogicDataPersistenceConfiguration {

    @Value("${spring.softlogic-jpa.hibernate.ddl-auto}")
    private String hbm2ddlAuto;

    @Value("${spring.softlogic-jpa.database-platform}")
    private String dialect;

    /* ============================================================
       DATASOURCE
       ============================================================ */

    @Bean(name = "softlogicDataSource")
    @ConfigurationProperties(prefix = "spring.softlogic-datasource")
    public DataSource softlogicDataSource() {
        return DataSourceBuilder.create().build();
    }

    /* ============================================================
       ENTITY MANAGER FACTORY
       ============================================================ */

    @Bean(name = "softlogicEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean softlogicEntityManagerFactory(
            @Qualifier("softlogicDataSource") DataSource dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        Map<String, Object> properties = Map.of(
                "hibernate.hbm2ddl.auto", hbm2ddlAuto,
                "hibernate.dialect", dialect
        );

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("lk.avengers.datamigrationadapter.entity.softlogicdb");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);
        emf.setPersistenceUnitName("report");

        return emf;
    }

    /* ============================================================
       TRANSACTION MANAGER
       ============================================================ */

    @Bean(name = "softlogicPlatformTransactionManager")
    public PlatformTransactionManager softlogicPlatformTransactionManager(
            @Qualifier("softlogicEntityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}

