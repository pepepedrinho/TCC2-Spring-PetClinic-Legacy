import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Offline migration from an original 2017 HSQL database to two NEW V2 databases. */
// Componente novo da coexistência: migração offline para dois bancos V2 novos, preservando identificadores
// e abrindo a origem somente para leitura.
class MigrateOwners {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("source HSQL file base, NEW target directory, repo root");
        }
        Path source = Path.of(args[0]).toAbsolutePath(), target = Path.of(args[1]).toAbsolutePath(),
                root = Path.of(args[2]);
        if (!Files.exists(Path.of(source + ".properties"))) {
            throw new IllegalArgumentException("Original database does not exist");
        }
        if (Files.exists(target)) {
            throw new IllegalArgumentException("Target must not exist; existing files are never replaced");
        }
        // Source is read-only and must have been shut down before migration.
        try (Connection sourceConnection = DriverManager.getConnection(
                "jdbc:hsqldb:file:" + source + ";ifexists=true;readonly=true;files_readonly=true", "SA", "")) {
            sourceConnection.setReadOnly(true);
            ResultSet orphanResultSet = sourceConnection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM pets p LEFT JOIN owners o ON o.id=p.owner_id WHERE o.id IS NULL");
            orphanResultSet.next();
            if (orphanResultSet.getInt(1) != 0) {
                throw new IllegalStateException("Source has orphan Pets");
            }
            Files.createDirectory(target);
            try (Connection legacyConnection = DriverManager
                    .getConnection("jdbc:hsqldb:file:" + target.resolve("petclinic") + ";shutdown=true", "SA", "");
                    Connection ownersConnection = DriverManager.getConnection(
                            "jdbc:h2:file:" + target.resolve("owners"), "sa",
                            "")) {
                sql(legacyConnection, Files.readString(root.resolve("projects/monolith/src/main/resources/db/hsqldb/schema.sql")));
                String h2 = Files.readString(root.resolve("projects/owners-service/src/main/resources/schema.sql"));
                sql(ownersConnection, h2.substring(0, h2.indexOf("INSERT INTO")));
                ownersConnection.createStatement().execute("INSERT INTO seed_history VALUES(1)");
                copy(sourceConnection, ownersConnection, "owners");
                for (String table : List.of("vets", "specialties", "vet_specialties", "types", "pets", "visits")) {
                    copy(sourceConnection, legacyConnection, table);
                }
                legacyConnection.createStatement().execute("SHUTDOWN");
            }
            Files.writeString(target.resolve("migration-complete.txt"),
                    "source=" + source + "\nOwner IDs and Pet/Visit IDs preserved. Source opened read-only.\n");
            System.out.println("PASS: migration complete: " + target);
        }
    }

    static void sql(Connection connection, String script) throws Exception {
        for (String statement : script.split(";")) {
            if (!statement.isBlank()) {
                connection.createStatement().execute(statement);
            }
        }
    }

    static void copy(Connection source, Connection target, String table) throws Exception {
        try (ResultSet resultSet = source.createStatement().executeQuery("SELECT * FROM " + table)) {
            int n = resultSet.getMetaData().getColumnCount();
            String marks = String.join(",", Collections.nCopies(n, "?"));
            try (PreparedStatement preparedStatement = target
                    .prepareStatement("INSERT INTO " + table + " VALUES (" + marks + ")")) {
                int count = 0;
                while (resultSet.next()) {
                    for (int i = 1; i <= n; i++) {
                        preparedStatement.setObject(i, resultSet.getObject(i));
                    }
                    preparedStatement.executeUpdate();
                    count++;
                }
                System.out.println(table + ": " + count + " rows copied");
            }
        }
        if (!table.equals("vet_specialties")) {
            ResultSet maxResultSet = target.createStatement()
                    .executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM " + table);
            maxResultSet.next();
            target.createStatement()
                    .execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + maxResultSet.getInt(1));
        }
    }
}
