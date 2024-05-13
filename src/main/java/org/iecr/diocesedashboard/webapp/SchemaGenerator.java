package org.iecr.diocesedashboard.webapp;

import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public class SchemaGenerator {

    private static final Logger LOG
            = Logger.getLogger(SchemaGenerator.class.getName());

    public static void main(String[] args) {
        Celebrant celebrant = new Celebrant();
        LOG.info(celebrant.toString());
        SchemaGenerator generator = new SchemaGenerator();
        generator.generateSchema();
    }

    public void generateSchema() {
        LOG.info("Generating Schema");
        Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
        var serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(cfg.getProperties())
                .build();
        LOG.info("Service Registry created.");
        var entities = scanForEntities("org.iecr.diocesedashboard.domain.objects");
        LOG.info("Found classes: ");
        entities.forEach(cls -> LOG.info("--- " + cls));
        LOG.info("Found Schema entities " + entities.size());
        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        entities.forEach(metadataSources::addAnnotatedClass);
        Metadata metadata = metadataSources.buildMetadata();
        LOG.info("Built metadata");
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setFormat(true);
        schemaExport.setOverrideOutputFileContent();
        schemaExport.setOutputFile("schema.sql");
        LOG.info("Running Create Only...");
        schemaExport.createOnly(EnumSet.of(TargetType.SCRIPT, TargetType.STDOUT), metadata);
        LOG.info("Schema export created");
    }

    private Set<Class<?>> scanForEntities(String pkg) {
        var reflections = new Reflections(pkg, new SubTypesScanner(false), new TypeAnnotationsScanner());
        reflections.getConfiguration().shouldExpandSuperTypes();
        LOG.info("Scanning for entities in package " + pkg);
        return reflections.getTypesAnnotatedWith(Entity.class);
    }
}
