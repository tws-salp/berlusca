package models;

import controllers.data.Triple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public abstract class TripleCorrupter {
    protected OWLOntology ontology;
    protected OWLReasoner reasoner;
    protected TripleIndexer indexer;
    protected final Random randomTripleGenerator;
    protected final Logger logger = Logger.getLogger(TripleCorrupter.class.getName());
    protected final int RANDOM_SEED = 12345;
    protected final String ONTOLOGY_FILETYPE = "RDF/XML";

    public TripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        logger.info("-- Loading ontology: " + ontologyFile.getAbsolutePath());
        ontology = OWLManager.createOWLOntologyManager().loadOntology(
                IRI.create(ontologyFile));

        logger.info("-- Building indexes");
        indexer = new TripleIndexer();
        indexer.buildIndexes(ontologyFile.getAbsolutePath(), ONTOLOGY_FILETYPE);
        String filename = ontologyFile.getAbsolutePath();
        if (filename.indexOf(".") > 0)
            filename = filename.substring(0, filename.lastIndexOf("."));
        filename += ".csv";
        logger.info("-- Saving indexes to file: " + filename);
        indexer.saveIndexesCsv(filename, ',');

        logger.info("-- Initializing reasoner");
        OWLReasonerConfiguration config = new SimpleConfiguration(50000);
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        this.randomTripleGenerator = new Random(RANDOM_SEED);
    }

    public List<Triple> corrupt(Triple triple, int numCorrupted) {
        List<Triple> triples = new ArrayList<>();

        triple.subject = indexer.getId2entity().get(Long.parseLong(triple.subject));
        triple.predicate = indexer.getId2relation().get(Long.parseLong(triple.predicate));
        triple.object = indexer.getId2entity().get(Long.parseLong(triple.object));

        for (int i = 0; i < numCorrupted; i++) {
            Triple t = corrupt(triple, randomTripleGenerator.nextBoolean());
            Triple t_id = new Triple();
            t_id.subject = String.valueOf(indexer.getEntity2id().get(t.subject));
            t_id.predicate = String.valueOf(indexer.getRelation2id().get(t.predicate));
            t_id.object = String.valueOf(indexer.getEntity2id().get(t.object));
            triples.add(t_id);
        }

        return triples;
    }

    protected abstract Triple corrupt(Triple triple, boolean corruptSubject);

    public static TripleCorrupter create(File ontologyFile, TripleCorrupterType tripleCorrupterType)
            throws OWLOntologyCreationException, IOException {
        switch (tripleCorrupterType) {
            case DISJOINT:
                return new DisjointTripleCorrupter(ontologyFile);
            case SIMILARITY:
                return new SimilarityTripleCorrupter(ontologyFile);
            default:
                throw new IllegalArgumentException("Invalid triple corrupter type!");
        }
    }
}
