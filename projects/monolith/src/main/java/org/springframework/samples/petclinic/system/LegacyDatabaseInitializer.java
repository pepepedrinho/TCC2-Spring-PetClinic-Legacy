package org.springframework.samples.petclinic.system;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** Only initializes a fresh V2 database. Existing databases are never reset. */
// Componente novo da coexistência: prepara apenas o banco legado vazio da V2, preservando bancos existentes
// e sem persistir Owner.
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "spring.datasource.initialize", havingValue = "false")
public class LegacyDatabaseInitializer implements InitializingBean {
    private final DataSource dataSource;

    public LegacyDatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(null, "PUBLIC", "TYPES", null)) {
            connection.createStatement().execute("SET FILES WRITE DELAY 0");
            if (tables.next()) {
                return;
            }
        }
        new ResourceDatabasePopulator(new ClassPathResource("db/hsqldb/schema.sql"),
                new ClassPathResource("db/hsqldb/data.sql")).execute(dataSource);
    }
}
