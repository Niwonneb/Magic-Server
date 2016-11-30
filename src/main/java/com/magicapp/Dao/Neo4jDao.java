package com.magicapp.Dao;

import com.graphaware.reco.generic.config.SimpleConfig;
import com.graphaware.reco.generic.result.Recommendation;
import com.magicapp.RecomendationEngines.ImageRecommendationEngine;
import org.neo4j.graphdb.*;
import org.parboiled.common.Tuple2;
import org.springframework.stereotype.Repository;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import javax.management.relation.Relation;
import java.io.File;
import java.util.*;

@Repository
public class Neo4jDao {
    private static GraphDatabaseService db = null;

    private static class IndexNumber {
        private Long nr;
        synchronized public long nextNr() {
            long n = nr;
            ++nr;
            return n;
        }
        synchronized public void setNr(long nr) {
            this.nr = nr;
        }
    }

    private static IndexNumber nextUserNr = new IndexNumber();
    private static IndexNumber nextImageNr = new IndexNumber();

    public enum NodeType implements Label {
        IMAGE, USER;
    }

    public enum Relationships implements RelationshipType {
        ADORES, SIMILAR, CREATED, LIKED, DISLIKED, OMITTED;
    }

    public Neo4jDao() {
        initDB();

        findCurrentNr(nextUserNr, "USER");
        findCurrentNr(nextImageNr, "IMAGE");
    }

    synchronized private void initDB() {
        if (db == null) {
            GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
            File dbFile = new File("C:/Projekte/MagicServer/neo4jDB");
            db = dbFactory.newEmbeddedDatabase(dbFile);
        }
    }

    private void findCurrentNr(IndexNumber nrVariable, String label) {
        Long nr = this.<Long>getValueFromCypher("MATCH (node:" + label + ")\n" +
                                                "RETURN node.nr\n" +
                                                "ORDER BY node.nr DESC LIMIT 1", "node.nr");
        if (nr != null) {
           nrVariable.setNr(nr);
        } else {
            // TODO: Error handling
            nrVariable.setNr(1L);
        }
    }

    public long createUser(String date) {
        Node userNode;
        long nr = nextUserNr.nextNr();
        try (Transaction tx = db.beginTx()) {
            userNode = db.createNode(NodeType.USER);

            userNode.setProperty("nr", nr);
            userNode.setProperty("date", date);
            userNode.setProperty("fame", 0);
            userNode.setProperty("dibles", 0);

            tx.success();
        }

        return nr;
    }

    public long createImage(long userNr, String date, String description, String path) {
        Node ImageNode;
        long nr = nextImageNr.nextNr();
        try (Transaction tx = db.beginTx()) {
            ImageNode = db.createNode(NodeType.IMAGE);

            ImageNode.setProperty("nr", nr);
            ImageNode.setProperty("date", date);
            ImageNode.setProperty("description", description);
            ImageNode.setProperty("path", path);
            tx.success();
        }

        setImageRelationship(userNr, nr, Relationships.CREATED, date);

        return nr;
    }

    public void likeImage(long userNr, long imageNr, String date) {
        judgeImage(userNr, imageNr, date, Relationships.LIKED, 1);
    }

    public void omitImage(long userNr, long imageNr, String date) {
        judgeImage(userNr, imageNr, date, Relationships.OMITTED, 0.1);
    }

    public void dislikeImage(long userNr, long imageNr, String date) {
        judgeImage(userNr, imageNr, date, Relationships.DISLIKED, -1);
    }

    private void judgeImage(long userNr, long imageNr, String date, Relationships relType, double adoration) {
        setImageRelationship(userNr, imageNr, relType, date);
        updateAdoration(userNr, imageNr, adoration);
        updateSimilarity(userNr, imageNr, relType);
    }

    private void updateSimilarity(long userNr, long imageNr, Relationships relType) {
        updateJudgeType(userNr, imageNr, Relationships.LIKED,    getSimilarityValue(Relationships.LIKED,    relType));
        updateJudgeType(userNr, imageNr, Relationships.DISLIKED, getSimilarityValue(Relationships.DISLIKED, relType));
        updateJudgeType(userNr, imageNr, Relationships.OMITTED,   getSimilarityValue(Relationships.OMITTED,   relType));
    }

    private static double llValue = 1;
    private static double ooValue = 0.1;
    private static double ddValue = 1;
    private static double ldValue = -1;
    private static double loValue = 0.3;
    private static double odValue = -0.3;

    static private Map<Tuple2<Relationships, Relationships>, Double> similarityValues = new HashMap<>();

    static {
        similarityValues.put(new Tuple2<>(Relationships.LIKED,    Relationships.LIKED),     llValue);
        similarityValues.put(new Tuple2<>(Relationships.OMITTED,   Relationships.OMITTED),  ooValue);
        similarityValues.put(new Tuple2<>(Relationships.DISLIKED,  Relationships.DISLIKED), ddValue);
        similarityValues.put(new Tuple2<>(Relationships.LIKED,     Relationships.DISLIKED), ldValue);
        similarityValues.put(new Tuple2<>(Relationships.DISLIKED,  Relationships.LIKED),    ldValue);
        similarityValues.put(new Tuple2<>(Relationships.LIKED,     Relationships.OMITTED),  loValue);
        similarityValues.put(new Tuple2<>(Relationships.OMITTED,   Relationships.LIKED),    loValue);
        similarityValues.put(new Tuple2<>(Relationships.OMITTED,   Relationships.DISLIKED), odValue);
        similarityValues.put(new Tuple2<>(Relationships.DISLIKED,  Relationships.OMITTED),  odValue);
    }

    private double getSimilarityValue(Relationships relType1, Relationships relType2) {
        return similarityValues.get(new Tuple2<>(relType1, relType2));
    }

    private void updateJudgeType(long userNr, long imageNr, Relationships relType, double similarityChange) {
        Result results = db.execute("MATCH (user1:USER {nr: " + userNr + "}) -[]-> (image:IMAGE {nr: " + imageNr + "})\n" +
                                    "MATCH (user2:USER) -[rel:" + relType.name() + "]-> (image)\n" +
                                    "WHERE NOT user2.nr = " + userNr + "\n" +
                                    "RETURN user2.nr");

        while (results.hasNext()) {
            Map<String, Object> node = results.next();

            Long nr = (Long) node.get("user2.nr");

            if (nr == null) {
                continue;
            }

            Double similarity = getValueFromCypher("MATCH (user1:USER {nr: " + userNr + "})\n" +
                                                   "MATCH (user2:USER {nr: " + nr + "})\n" +
                                                   "MATCH (user1) -[rel:SIMILAR]- (user2)\n" +
                                                   "SET rel.similarity = rel.similarity + " + similarityChange + "\n" +
                                                   "RETURN rel.similarity", "rel.similarity");

            if (similarity == null) {
                setUserRelationship(userNr, nr, Relationships.SIMILAR, " {similarity: " + similarityChange + "}");
            }
        }
        results.close();
    }

    private void updateAdoration(long userNr, long imageNr, double adoration) {
        Long creatorNr = this.<Long>getValueFromCypher("MATCH (user:USER) -[:CREATED]-> (image:IMAGE)\n" +
                                                       "WHERE image.nr = " + String.valueOf(imageNr) + "\n" +
                                                       "RETURN user.nr", "user.nr");

        if (creatorNr == null) {
            // TODO: check if possible
            return;
        }

        updateOrCreateAdoration(userNr, creatorNr, adoration);
    }

    // TODO: Avoid Strings
    private void updateOrCreateAdoration(long userNr, long creatorNr, double adorationChange) {
        Map<String, Object> rel = getNodeFromCypher("MATCH (:USER {nr: " + userNr + "}) -[rel:ADORES]-> (:USER {nr: " + creatorNr + "})\n" +
                                                    "SET rel.adoration = rel.adoration + " + adorationChange + "\n" +
                                                    "RETURN rel");

        if (rel == null) {
            setUserRelationship(userNr, creatorNr, Relationships.ADORES, " {adoration: " + adorationChange + "}");
        }
    }

    private void setImageRelationship(long userNr, long imageNr, Relationships relType, String date) {
        setRelationship(NodeType.USER, userNr, NodeType.IMAGE, imageNr, relType,  " {date: \"" + date + "\"}");
    }

    private void setUserRelationship(long userNr1, long userNr2, Relationships relType, String property) {
        setRelationship(NodeType.USER, userNr1, NodeType.USER, userNr2, relType, property);
    }

    private void setRelationship(NodeType type1, long nr1, NodeType type2, long nr2, Relationships relType, String property) {
        db.execute("MATCH (node1:" + type1.name() + " {nr: " + nr1 + "})" +
                   "MATCH (node2:" + type2.name() + " {nr: " + nr2 + "})" +
                   "CREATE UNIQUE (node1) -[:" + relType.name() + property + "]-> (node2)");
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
            // TODO: Error handling
            return null;
        }
    }

    private Map<String, Object> getNodeFromCypher(String cypher) {
        Result result = db.execute(cypher);
        try {
            if (result.hasNext()) {
                return result.next();
            } else {
                // TODO: errorhandling
                return null;
            }
        } finally {
            result.close();
        }
    }

    public String getRecommendations(int nr) {
        List<Recommendation<Node>> recommendations;
        StringBuilder sb = new StringBuilder();

        try (Transaction tx = db.beginTx()) {
            ImageRecommendationEngine recoEngine = new ImageRecommendationEngine();

            Node user = getUserByNr(nr);
            recommendations = recoEngine.recommend(user, new SimpleConfig(2));

            for (int i = 0; i < recommendations.size(); ++i) {
                sb.append(recommendations.get(i).getItem().getProperty("description"));
            }

            tx.success();
        }

        return sb.toString();
    }

    private Node getUserByNr(int nr) {
        return db.findNode(NodeType.USER, "nr", nr);
    }
}
