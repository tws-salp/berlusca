package models;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class TripleIndexer {

    private Map<String, Long> entity2id;
    private Map<String, Long> relation2id;
    private Map<Long, String> id2entity;
    private Map<Long, String> id2relation;

    private final Logger logger = Logger.getLogger(TripleIndexer.class.getName());

    public TripleIndexer(File entityMappingFile, File relationMappingFile) throws IOException {
        loadEntityIndexes(entityMappingFile);
        loadRelationIndexes(relationMappingFile);
    }

    public Map<String, Long> getEntity2id() {
        return entity2id;
    }

    public Map<String, Long> getRelation2id() {
        return relation2id;
    }

    public Map<Long, String> getId2entity() {
        return id2entity;
    }

    public Map<Long, String> getId2relation() {
        return id2relation;
    }


    private void loadEntityIndexes(File entityMappingFile) throws IOException {
        logger.info("-- Loading entity indexes");
        entity2id = new HashMap<>();
        id2entity = new HashMap<>();
        try(CSVParser csvParser = new CSVParser(new FileReader(entityMappingFile), CSVFormat.TDF)) {
            final Iterator<CSVRecord> csvIterator = csvParser.iterator();
            while(csvIterator.hasNext()) {
                final CSVRecord csvRecord = csvIterator.next();
                String uri = csvRecord.get(0);
                Long id = Long.parseLong(csvRecord.get(1));
                entity2id.put(uri, id);
                id2entity.put(id, uri);
            }
        }
    }

    private void loadRelationIndexes(File relationMappingFile) throws IOException {
        logger.info("-- Loading relation indexes");
        relation2id = new HashMap<>();
        id2relation = new HashMap<>();
        try(CSVParser csvParser = new CSVParser(new FileReader(relationMappingFile), CSVFormat.TDF)) {
            final Iterator<CSVRecord> csvIterator = csvParser.iterator();
            while(csvIterator.hasNext()) {
                final CSVRecord csvRecord = csvIterator.next();
                String uri = csvRecord.get(0);
                Long id = Long.parseLong(csvRecord.get(1));
                relation2id.put(uri, id);
                id2relation.put(id, uri);
            }
        }
    }
}
