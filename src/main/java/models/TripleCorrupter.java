package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
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
    protected final Random randomEntityGenerator;
    // Maps individuals to most specific classes
    protected Multimap<OWLNamedIndividual, OWLClass> individualsClasses;
    // Maps classes to individuals
    protected Multimap<OWLClass, OWLNamedIndividual> classesIndividuals;
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
        /*String filename = ontologyFile.getAbsolutePath();
        if (filename.indexOf(".") > 0)
            filename = filename.substring(0, filename.lastIndexOf("."));
        filename += ".csv";
        logger.info("-- Saving indexes to file: " + filename);
        indexer.saveIndexesCsv(filename, ',');*/

        logger.info("-- Initializing reasoner");
        OWLReasonerConfiguration config = new SimpleConfiguration(50000);
        OWLReasonerFactory reasonerFactory = new JFactFactory();
        reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        this.randomTripleGenerator = new Random(RANDOM_SEED);

        this.randomEntityGenerator = new Random(RANDOM_SEED);
        logger.info("-- Building individuals to classes index");
        buildIndividualsClasses();

        logger.info("-- Building classes to individuals index");
        buildClassesIndividuals();
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

    protected Triple generateRandomTriple(Triple triple, OWLNamedIndividual iriIndividual, boolean corruptSubject) {
        Triple corruptedTriple = new Triple();
        List<OWLNamedIndividual> ontologyIndividuals = new ArrayList<>(individualsClasses.keySet());

        if (corruptSubject) {

            OWLNamedIndividual corruptedEntity;
            do {
                corruptedEntity = ontologyIndividuals.get(randomEntityGenerator.nextInt(ontologyIndividuals.size()));
            } while (corruptedEntity.equals(iriIndividual));

            corruptedTriple.subject = corruptedEntity.getIRI().toString();
            corruptedTriple.predicate = triple.predicate;
            corruptedTriple.object = triple.object;
        } else {
            OWLNamedIndividual corruptedEntity;
            do {
                corruptedEntity = ontologyIndividuals.get(randomEntityGenerator.nextInt(ontologyIndividuals.size()));
            } while (corruptedEntity.equals(iriIndividual));

            corruptedTriple.subject = triple.subject;
            corruptedTriple.predicate = triple.predicate;
            corruptedTriple.object = corruptedEntity.getIRI().toString();
        }

        return corruptedTriple;
    }

    private void buildIndividualsClasses() {
        individualsClasses = HashMultimap.create();
        for (OWLClass currentClass: ontology.getClassesInSignature()) {
            for (OWLNamedIndividual currentIndividual:
                    reasoner.getInstances(currentClass, false).getFlattened()) {
                individualsClasses.put(currentIndividual, currentClass);
            }
        }
    }

    private void buildClassesIndividuals() {
        classesIndividuals = HashMultimap.create();
        for (OWLClass owlClass: ontology.getClassesInSignature()) {
            classesIndividuals.putAll(owlClass, reasoner.getInstances(owlClass, false).getFlattened());
        }
    }

}
