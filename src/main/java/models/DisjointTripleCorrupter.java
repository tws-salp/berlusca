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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DisjointTripleCorrupter extends TripleCorrupter {
    // Maps classes to disjoint classes
    private Multimap<OWLClass, OWLClass> disjointClasses;

    DisjointTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        super(ontologyFile);

        logger.info("-- Building disjoint classes index");
        buildDisjointClasses();
    }

    protected Triple corrupt(Triple triple, boolean corruptSubject) {
        OWLNamedIndividual iriIndividual = (corruptSubject) ?
                new OWLNamedIndividualImpl(IRI.create(triple.subject)) :
                new OWLNamedIndividualImpl(IRI.create(triple.object));
        Triple corruptedTriple = null;
        Collection<OWLClass> iriClasses = individualsClasses.get(iriIndividual);

        if (iriClasses != null) {
            List<OWLNamedIndividual> notIriIndividuals = new ArrayList<>();
            for (OWLClass iriClass : iriClasses) {
                Collection<OWLClass> notIriClasses = disjointClasses.get(iriClass);

                for (OWLClass currentClass : notIriClasses) {
                    classesIndividuals.get(currentClass).forEach(notIriIndividuals::add);
                }
            }

            if (!notIriIndividuals.isEmpty()) {
                OWLNamedIndividual corruptedEntity = notIriIndividuals.get(
                        randomEntityGenerator.nextInt(notIriIndividuals.size()));
                corruptedTriple = new Triple();
                if (corruptSubject) {
                    corruptedTriple.subject = corruptedEntity.getIRI().toString();
                    corruptedTriple.predicate = triple.predicate;
                    corruptedTriple.object = triple.object;
                } else {
                    corruptedTriple.subject = triple.subject;
                    corruptedTriple.predicate = triple.predicate;
                    corruptedTriple.object = corruptedEntity.getIRI().toString();
                }
            }
        }

        return (corruptedTriple == null) ? generateRandomTriple(triple, iriIndividual, corruptSubject) : corruptedTriple;
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
