import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Offline migration from an original 2017 HSQL database to two NEW V2 databases. */
class MigrateOwners {
    static void sql(Connection c,String script) throws Exception {
        for(String statement:script.split(";")) if(!statement.isBlank()) c.createStatement().execute(statement);
    }
    static void copy(Connection source,Connection target,String table) throws Exception {
        try(ResultSet rows=source.createStatement().executeQuery("SELECT * FROM "+table)) {
            int n=rows.getMetaData().getColumnCount();
            String marks=String.join(",",Collections.nCopies(n,"?"));
            try(PreparedStatement insert=target.prepareStatement("INSERT INTO "+table+" VALUES ("+marks+")")) {
                int count=0;
                while(rows.next()) {
                    for(int i=1;i<=n;i++) insert.setObject(i,rows.getObject(i));
                    insert.executeUpdate();count++;
                }
                System.out.println(table+": "+count+" rows copied");
            }
        }
        if(!table.equals("vet_specialties")) {
            ResultSet max=target.createStatement().executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM "+table);max.next();
            target.createStatement().execute("ALTER TABLE "+table+" ALTER COLUMN id RESTART WITH "+max.getInt(1));
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("source HSQL file base, NEW target directory, repo root");
        Path source=Path.of(args[0]).toAbsolutePath(),target=Path.of(args[1]).toAbsolutePath(),root=Path.of(args[2]);
        if(!Files.exists(Path.of(source+".properties"))) throw new IllegalArgumentException("Original database does not exist");
        if(Files.exists(target)) throw new IllegalArgumentException("Target must not exist; existing files are never replaced");
        // Source is read-only and must have been shut down before migration.
        try(Connection old=DriverManager.getConnection("jdbc:hsqldb:file:"+source+";ifexists=true;readonly=true;files_readonly=true","SA","")) {
            old.setReadOnly(true);
            ResultSet orphans=old.createStatement().executeQuery("SELECT COUNT(*) FROM pets p LEFT JOIN owners o ON o.id=p.owner_id WHERE o.id IS NULL");
            orphans.next();if(orphans.getInt(1)!=0) throw new IllegalStateException("Source has orphan Pets");
            Files.createDirectory(target);
            try(Connection legacy=DriverManager.getConnection("jdbc:hsqldb:file:"+target.resolve("petclinic")+";shutdown=true","SA","");
                Connection owners=DriverManager.getConnection("jdbc:h2:file:"+target.resolve("owners"),"sa","")) {
                sql(legacy,Files.readString(root.resolve("src/main/resources/db/hsqldb/schema.sql")));
                String h2=Files.readString(root.resolve("owners-service/src/main/resources/schema.sql"));
                sql(owners,h2.substring(0,h2.indexOf("INSERT INTO")));
                owners.createStatement().execute("INSERT INTO seed_history VALUES(1)");
                copy(old,owners,"owners");
                for(String table:List.of("vets","specialties","vet_specialties","types","pets","visits")) copy(old,legacy,table);
                legacy.createStatement().execute("SHUTDOWN");
            }
            Files.writeString(target.resolve("migration-complete.txt"),"source="+source+"\nOwner IDs and Pet/Visit IDs preserved. Source opened read-only.\n");
            System.out.println("PASS: migration complete: "+target);
        }
    }
}
