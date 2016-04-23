package models;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import controllers.data.Triple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DisjointTripleCorrupter extends TripleCorrupter {
    private final Random randomGenerator;
    // Maps individuals to most specific classes
    private Multimap<OWLNamedIndividual, OWLClass> individualsClasses;

    // Maps classes to individuals
    private Multimap<OWLClass, OWLNamedIndividual> classesIndividuals;

    // Maps classes to disjoint classes
    private Multimap<OWLClass, OWLClass> disjointClasses;

    private final int RANDOM_SEED = 12345;

    DisjointTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException {
        super(ontologyFile);

        this.randomGenerator = new Random(RANDOM_SEED);

        logger.info("-- Building individuals to classes index");
        buildIndividualsClasses();

        logger.info("-- Building classes to individuals index");
        buildClassesIndividuals();

        logger.info("-- Building disjoint classes index");
        buildDisjointClasses();
    }

    @Override
    public List<Triple> corrupt(Triple triple, int numCorrupted) {
        List<Triple> triples = new ArrayList<>();
        boolean corruptedSubject = randomGenerator.nextBoolean();

        OWLNamedIndividual iriIndividual = (corruptedSubject) ?
                new OWLNamedIndividualImpl(IRI.create(triple.subject)) :
                new OWLNamedIndividualImpl(IRI.create(triple.object));

        Collection<OWLClass> iriClasses = individualsClasses.get(iriIndividual);

        if (iriClasses != null) {
            Set<OWLNamedIndividual> notIriIndividuals = new HashSet<>();
            for(OWLClass iriClass: iriClasses) {
                Collection<OWLClass> notIriClasses = disjointClasses.get(iriClass);

                for (OWLClass currentClass : notIriClasses) {
                    classesIndividuals.get(currentClass).forEach(notIriIndividuals::add);
                }
            }

            notIriIndividuals.stream().
                    limit(numCorrupted).
                    forEach(i -> {
                        Triple t = new Triple();
                        if (corruptedSubject) {
                            t.subject = i.getIRI().toString();
                            t.predicate = triple.predicate;
                            t.object = triple.object;
                        } else {
                            t.subject = triple.subject;
                            t.predicate = triple.predicate;
                            t.object = i.getIRI().toString();
                        }
                        triples.add(t);
                    });
        }

        return triples;
    }

    private boolean throwCoin(int seed) {


        return randomGenerator.nextDouble() > 0.5;
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

    private void buildDisjointClasses() {
        disjointClasses = HashMultimap.create();
        for (OWLClass currentClass: ontology.getClassesInSignature()) {
            disjointClasses.putAll(currentClass, ontology.classesInSignature().
                    filter(c -> !reasoner.getSuperClasses(currentClass).getFlattened().contains(c) && !currentClass.equals(c)).
                    collect(Collectors.toList()));
        }
    }

}
