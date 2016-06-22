package conf;

import com.google.inject.Inject;
import com.google.inject.Provider;
import models.TripleCorrupter;
import models.TripleCorrupterType;
import models.TripleIndexer;
import ninja.utils.NinjaProperties;
import org.apache.jena.base.Sys;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;

public class TripleIndexerProvider implements Provider<TripleIndexer> {

    private final NinjaProperties properties;

    @Inject
    public TripleIndexerProvider(NinjaProperties properties) {
        this.properties = properties;
    }

    @Override
    public TripleIndexer get() {
        try {
            return new TripleIndexer(
                    new File(properties.get("application.entity_mappings_filename")),
                    new File(properties.get("application.relation_mappings_filename")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}