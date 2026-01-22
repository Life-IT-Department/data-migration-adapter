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
        basePackages = "lk.avengers.datamigrationadapter.repository.postgresql.reportdb",
        entityManagerFactoryRef = "reportEntityManagerFactory",
        transactionManagerRef = "reportPlatformTransactionManager"
)
public class ReportPersistenceConfiguration {

    @Value("${spring.report-jpa.hibernate.ddl-auto}")
    private String hbm2ddlAuto;

    @Value("${spring.report-jpa.database-platform}")
    private String dialect;

    /* ============================================================
       DATASOURCE
       ============================================================ */

    @Bean(name = "reportDataSource")
    @ConfigurationProperties(prefix = "spring.report-datasource")
    public DataSource reportDataSource() {
        return DataSourceBuilder.create().build();
    }

    /* ============================================================
       ENTITY MANAGER FACTORY
       ============================================================ */

    @Bean(name = "reportEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean reportEntityManagerFactory(
            @Qualifier("reportDataSource") DataSource dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        Map<String, Object> properties = Map.of(
                "hibernate.hbm2ddl.auto", hbm2ddlAuto,
                "hibernate.dialect", dialect
        );

        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(dataSource);
        emf.setPackagesToScan("lk.avengers.datamigrationadapter.entity.postgresql.reportdb");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);
        emf.setPersistenceUnitName("report");

        return emf;
    }

    /* ============================================================
       TRANSACTION MANAGER
       ============================================================ */

    @Bean(name = "reportPlatformTransactionManager")
    public PlatformTransactionManager reportPlatformTransactionManager(
            @Qualifier("reportEntityManagerFactory")
            EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
