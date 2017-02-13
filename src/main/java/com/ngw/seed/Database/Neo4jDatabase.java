package com.ngw.seed.Database;

import com.ngw.seed.CommunicationObjects.Thought;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Repository
public class Neo4jDatabase {
    private static GraphDatabaseService db = null;

    private enum NodeType implements Label {
        THOUGHT
    }

    private enum Relationships implements RelationshipType {
        AFTER_LIKED, AFTER_DISLIKED
    }

    private Index<Node> idIndex;

    public Neo4jDatabase() {
        initDB();
        createIndex();
        createStartNodeIfDBisNew();
    }

    synchronized private void initDB() {
        if (db == null) {
            GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
            File dbFile = new File("C:/Projekte/seedServer/neo4jDB");
            db = dbFactory.newEmbeddedDatabase(dbFile);
        }
    }

    private void createIndex() {
        try (Transaction tx = db.beginTx()) {
            idIndex = db.index().forNodes("thoughts");
            tx.success();
        }
    }

    private void createStartNodeIfDBisNew() {
        try (Transaction tx = db.beginTx()) {
            if (!db.getAllNodes().iterator().hasNext()) {
                Node startNode = db.createNode(NodeType.THOUGHT);

                startNode.setProperty("id", "start");
                idIndex.add(startNode, "id", "start");
            }
            tx.success();
        }
    }

    public String createNewThought(String text, String idBefore, boolean likedBefore) {
        String idNew = createThoughtNode(text);
        Relationships relType = (likedBefore? Relationships.AFTER_LIKED : Relationships.AFTER_DISLIKED);
        setOrUpdateRatingRelationship(idBefore, idNew, relType, true);
        return idNew;
    }

    public String createThoughtNode(String text) {
        Node thoughtNode;
        String id = UUID.randomUUID().toString();
        try (Transaction tx = db.beginTx()) {
            thoughtNode = db.createNode(NodeType.THOUGHT);

            thoughtNode.setProperty("text", text);
            thoughtNode.setProperty("id", id);
            idIndex.add(thoughtNode, "id", id);
            tx.success();
        }
        return id;
    }

    public Thought getThoughtFromStart() {
        return getNextThought("start", true);
    }

    private static final double CHANCE_tO_GO_TO_START = 0.05;
    private static final int MAX_NUMBER_TO_ITERATE = 1000;

    public Thought getNextThought(String idNow, boolean liked) {
        Relationships relevantRelType = (liked ? Relationships.AFTER_LIKED : Relationships.AFTER_DISLIKED);

        try (Transaction tx = db.beginTx()) {
            Node node = idIndex.get("id", idNow).getSingle();

            if (node != null) {
                double totalChances = 0;
                Iterable<Relationship> relationships;

                relationships = node.getRelationships(Direction.OUTGOING, relevantRelType);

                if (!relationships.iterator().hasNext()) {
                    if (!idNow.equals("start")) {
                        return getThoughtFromStart();
                    } else {
                        return new Thought("start", "There are no Thoughts");
                    }
                }

                int i = 0;
                for (Relationship rel : relationships) {
                    Long rating = (Long) rel.getProperty("rating");
                    Long ratingsCount = (Long) rel.getProperty("ratingsCount");
                    totalChances += ((double) rating) / ratingsCount;

                    ++i;
                    if (i > MAX_NUMBER_TO_ITERATE) {
                        break;
                    }
                }

                Random rand = new Random();
                totalChances = totalChances * (1 + CHANCE_tO_GO_TO_START);
                double randValue = rand.nextDouble() * totalChances;

                totalChances = 0;
                Relationship chosenRelationship = null;

                i = 0;
                for (Relationship rel : relationships) {
                    Long rating = (Long) rel.getProperty("rating");
                    Long ratingsCount = (Long) rel.getProperty("ratingsCount");
                    totalChances += ((double) rating) / ratingsCount;

                    if (totalChances >= randValue) {
                        chosenRelationship = rel;
                        break;
                    }

                    ++i;
                    if (i > MAX_NUMBER_TO_ITERATE) {
                        break;
                    }
                }

                if (chosenRelationship != null) {
                    Node nextThoughtNode = chosenRelationship.getEndNode();
                    return new Thought((String) nextThoughtNode.getProperty("id"),
                                       (String) nextThoughtNode.getProperty("text"));
                } else {
                    return getThoughtFromStart();
                }
            } else {
                return new Thought("start", "Not a real ID");
            }
        }
    }

    public void rateThought(String idBefore, boolean likedBefore, String idNow, boolean likedNow) {
        Relationships relType = (likedBefore ? Relationships.AFTER_LIKED : Relationships.AFTER_DISLIKED);
        setOrUpdateRatingRelationship(idBefore, idNow, relType, likedNow);
    }

    private void setOrUpdateRatingRelationship(String idBefore, String idNow, Relationships relType, boolean likedNow) {
        int ratingChange = (likedNow ? 1 : 0);
        //TODO: check for injetion
        Long ratingsCount = getValueFromCypher("MATCH (thought1 {id: \'" + idBefore + "\'})\n" +
                                               "MATCH (thought2 {id: \'" + idNow + "\'})\n" +
                                               "MATCH (thought1) -[rel:" + relType.name() + "]-> (thought2)\n" +
                                               "SET rel.ratingsCount = rel.ratingsCount + 1\n" +
                                               "SET rel.rating = rel.rating + " + ratingChange + "\n" +
                                               "RETURN rel.ratingsCount", "rel.ratingsCount");

        if (ratingsCount == null &&
            likedNow) {
            setNewRatingRelationship(idBefore, idNow, relType);
        }
    }

    private void setNewRatingRelationship(String id1, String id2, Relationships relType) {
        // TODO: check for injection
        db.execute("MATCH (node1 {id: \'" + id1 + "\'})" +
                   "MATCH (node2 {id: \'" + id2 + "\'})" +
                   "CREATE UNIQUE (node1) -[:" + relType.name() + " {ratingsCount: 1, rating: 1}]-> (node2)");
    }

    private <T> T getValueFromCypher(String cypher, String property) {
        Map<String, Object> node = getNodeFromCypher(cypher);

        if (node == null) {
            return null;
        }

        T value = (T) node.get(property);
        if (value != null) {
            return value;
        } else {
            return null;
        }
    }

    private Map<String, Object> getNodeFromCypher(String cypher) {
        Result result = db.execute(cypher);
        try {
            if (result.hasNext()) {
                return result.next();
            } else {
                return null;
            }
        } finally {
            result.close();
        }
    }
}
