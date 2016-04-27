package models;

import controllers.data.Triple;
import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SimilarityTripleCorrupter extends TripleCorrupter {
    public SimilarityTripleCorrupter(File ontologyFile) throws OWLOntologyCreationException, IOException {
        super(ontologyFile);
        buildConceptHierarchy();
    }

    @Override
    public List<Triple> corrupt(Triple triple, EntityType entityType, int size) {
        return null;
    }

    private void buildConceptHierarchy() throws IOException {
        Graph<OWLClass, DefaultEdge> conceptHierarchy =
                new DefaultDirectedGraph<>(DefaultEdge.class);
        Queue<OWLClass> classQueue = new LinkedList<>();
        OWLClass topNode = reasoner.getTopClassNode().getRepresentativeElement();
        classQueue.add(topNode);
        Set<OWLClass> visitedClasses = new HashSet<>();
        while (!classQueue.isEmpty()) {
            OWLClass currentClass = classQueue.poll();
            if (!visitedClasses.contains(currentClass)) {
                conceptHierarchy.addVertex(currentClass);
                NodeSet<OWLClass> childrenClasses = reasoner.getSubClasses(currentClass);
                childrenClasses.forEach(c -> classQueue.add(c.getRepresentativeElement()));
                childrenClasses.forEach(c -> {
                    conceptHierarchy.addVertex(c.getRepresentativeElement());
                    conceptHierarchy.addEdge(currentClass, c.getRepresentativeElement());
                });

                visitedClasses.add(currentClass);
            }
        }
        System.out.println(conceptHierarchy);
        final Map<OWLClass, Integer> idMap = new HashMap<OWLClass, Integer>();
        DOTExporter exporter = new DOTExporter(new VertexNameProvider<OWLClass>() {
            @Override
            public String getVertexName(OWLClass owlClass) {
                Integer id = idMap.get(owlClass);
                if(id == null){
                    id = idMap.size();
                    idMap.put(owlClass, id);
                }
                return id.toString();
            }
        }, new VertexNameProvider<OWLClass>() {
            @Override
            public String getVertexName(OWLClass owlClass) {
                return owlClass.toString();
            }
        }, new EdgeNameProvider<DefaultEdge>() {
            @Override
            public String getEdgeName(DefaultEdge defaultEdge) {
                return "";
            }
        });
        exporter.export(new FileWriter("classHierarchy.dot"), conceptHierarchy);
    }

    public static void main(String [] args) throws OWLOntologyCreationException, IOException {
        TripleCorrupter tripleCorrupter = new SimilarityTripleCorrupter(new File("wine.rdf"));
    }
}
