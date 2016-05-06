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
    protected final Random randomTripleGenerator;
    protected final Logger logger = Logger.getLogger(TripleCorrupter.class.getName());
    protected final int RANDOM_SEED = 12345;

    public TripleCorrupter(File ontologyFile) throws OWLOntologyCreationException {
        logger.info("-- Loading ontology: " + ontologyFile.getAbsolutePath());
        ontology = OWLManager.createOWLOntologyManager().loadOntology(
                IRI.create(ontologyFile));

        logger.info("-- Initializing reasoner");
        OWLReasonerConfiguration config = new SimpleConfiguration(50000);
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        this.randomTripleGenerator = new Random(RANDOM_SEED);
    }

    public List<Triple> corrupt(Triple triple, int numCorrupted) {
        List<Triple> triples = new ArrayList<>();

        for (int i = 0; i < numCorrupted; i++) {
            triples.add(corrupt(triple, randomTripleGenerator.nextBoolean()));
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
