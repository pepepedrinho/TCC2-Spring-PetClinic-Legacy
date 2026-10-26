package br.edu.tcc.ownerspets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseInitializationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveImportarDadosHistoricos() {
        assertEquals(10, contar("owners"));
        assertEquals(13, contar("pets"));
        assertEquals(6, contar("types"));
    }

    @Test
    void devePreservarIdentificadoresERelacionamentos() {
        assertEquals("George", jdbc.queryForObject(
            "SELECT first_name FROM owners WHERE id = 1", String.class));

        assertEquals("Leo", jdbc.queryForObject(
            "SELECT name FROM pets WHERE id = 1", String.class));

        assertEquals(1, jdbc.queryForObject(
            "SELECT owner_id FROM pets WHERE id = 1", Integer.class));

        assertEquals(6, jdbc.queryForObject(
            "SELECT type_id FROM pets WHERE id = 2", Integer.class));
    }

    private int contar(String tabela) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela, Integer.class);
    }
}