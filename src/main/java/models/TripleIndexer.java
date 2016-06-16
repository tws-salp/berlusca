package models;


import au.com.bytecode.opencsv.CSVWriter;
import controllers.data.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class TripleIndexer {

    private Map<String, Long> entity2id;
    private Map<String, Long> relation2id;
    private Map<Long, String> id2entity;
    private Map<Long, String> id2relation;
    private List<Triple> triples;

    private final Logger logger = Logger.getLogger(TripleIndexer.class.getName());

    Map<String, Long> getEntity2id() {
        return entity2id;
    }

    Map<String, Long> getRelation2id() {
        return relation2id;
    }

    Map<Long, String> getId2entity() {
        return id2entity;
    }

    Map<Long, String> getId2relation() {
        return id2relation;
    }

    void buildIndexes(String inputFile, String type) throws IOException {
        Model model = ModelFactory.createDefaultModel() ;
        InputStream in = new FileInputStream(inputFile);
        model.read(in, type);

        entity2id = new HashMap<>();
        relation2id = new HashMap<>();
        id2entity = new HashMap<>();
        id2relation = new HashMap<>();
        triples = new ArrayList<>();

        final StmtIterator stmtIterator = model.listStatements();

        long entityCounter = 0;
        long predicateCounter = 0;

        while(stmtIterator.hasNext()) {
            Statement triple = stmtIterator.next();

            Triple t = new Triple();
            t.subject = triple.getSubject().toString();
            t.predicate = triple.getPredicate().toString();
            t.object = triple.getObject().toString();

            Triple t_id = new Triple();

            if(!entity2id.containsKey(t.subject)) {
                entity2id.put(t.subject, entityCounter);
                id2entity.put(entityCounter, t.subject);
                entityCounter++;
            }
            t_id.subject = String.valueOf(entity2id.get(t.subject));

            if(!relation2id.containsKey(t.predicate)) {
                relation2id.put(t.predicate, predicateCounter);
                id2relation.put(predicateCounter, t.predicate);
                predicateCounter++;
            }
            t_id.predicate = String.valueOf(relation2id.get(t.predicate));

            if(!entity2id.containsKey(t.object)) {
                entity2id.put(t.object, entityCounter);
                id2entity.put(entityCounter, t.object);
                entityCounter++;
            }
            t_id.object = String.valueOf(entity2id.get(t.object));

            triples.add(t_id);
        }
        logger.info("-- Indexes built successfully: ");
        logger.info("-- Number of entities: " + entity2id.size());
        logger.info("-- Number of predicates: " + relation2id.size());
    }

    void saveIndexesCsv(String outputFile, char delimiter) throws IOException {
        if(delimiter == '\u0000')
            delimiter = ',';

        try(CSVWriter writer = new CSVWriter(new FileWriter(outputFile), delimiter, CSVWriter.NO_QUOTE_CHARACTER)) {
            for(Triple triple: this.triples) {
                String[] entries = {triple.subject, triple.predicate, triple.object};
                writer.writeNext(entries);
            }
        }
    }
}
