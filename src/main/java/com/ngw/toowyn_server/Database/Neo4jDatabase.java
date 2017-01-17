package com.ngw.toowyn_server.Database;

import com.graphaware.reco.generic.config.SimpleConfig;
import com.graphaware.reco.generic.result.Recommendation;
import com.ngw.toowyn_server.Entity.Thought;
import com.ngw.toowyn_server.RecomendationEngines.ThoughtRecommendationEngine;
import org.neo4j.graphdb.*;
import org.springframework.stereotype.Repository;

import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.*;

@Repository
public class Neo4jDatabase {
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
    private static IndexNumber nextThoughtNr = new IndexNumber();

    public enum NodeType implements Label {
        THOUGHT, USER
    }

    public enum Relationships implements RelationshipType {
        SIMILAR, LIKED, DISLIKED
    }

    public Neo4jDatabase() {
        initDB();

        findCurrentNr(nextUserNr, "USER");
        findCurrentNr(nextThoughtNr, "THOUGHT");
    }

    synchronized private void initDB() {
        if (db == null) {
            GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
            File dbFile = new File("C:/Projekte/toowynServer/neo4jDB");
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

    public void createUserIfNotExists(String androidID) {
        Node userNode;
        try (Transaction tx = db.beginTx()) {
            Node user = getUserByID(androidID);
            if (user == null) {

                userNode = db.createNode(NodeType.USER);

                userNode.setProperty("id", androidID);
            }

            tx.success();
        }
    }

    public long createThought(String text) {
        Node ThoughtNode;
        long nr = nextThoughtNr.nextNr();
        try (Transaction tx = db.beginTx()) {
            ThoughtNode = db.createNode(NodeType.THOUGHT);

            ThoughtNode.setProperty("text", text);
            tx.success();
        }

        return nr;
    }

    public void likeThought(long userNr, long thoughtNr) {
        judgeThought(userNr, thoughtNr, Relationships.LIKED);
    }

    public void dislikeThought(long userNr, long thoughtNr) {
        judgeThought(userNr, thoughtNr, Relationships.DISLIKED);
    }

    private void judgeThought(long userNr, long thoughtNr, Relationships relType) {
        setJudgeRelationship(userNr, thoughtNr, relType);
        updateSimilarity(userNr, thoughtNr, relType);
    }

    private void updateSimilarity(long userNr, long thoughtNr, Relationships relType) {
        updateJudgeType(userNr, thoughtNr, Relationships.LIKED,    getSimilarityChange(Relationships.LIKED,    relType));
        updateJudgeType(userNr, thoughtNr, Relationships.DISLIKED, getSimilarityChange(Relationships.DISLIKED, relType));
    }

    private int getSimilarityChange(Relationships relationshipType1, Relationships relationshipType2) {
        if (relationshipType1 == relationshipType2) {
            return 1;
        } else {
            return -1;
        }
    }

    private void updateJudgeType(long userNr, long newlyJudgedThoughtNr, Relationships relType, int similarityChange) {
        Result results = db.execute("MATCH (user1:USER {nr: " + userNr + "}) -[]-> (thought:THOUGHT)\n" +
                                    "WHERE NOT thought.nr = " + newlyJudgedThoughtNr + "\n" +
                                    "RETURN thought.nr");

        while (results.hasNext()) {
            Map<String, Object> node = results.next();

            Long oldJudgedThoughtNr = (Long) node.get("thought.nr");

            if (oldJudgedThoughtNr == null) {
                continue;
            }

            Double similarity = getValueFromCypher("MATCH (thought1:THOUGHT {nr: " + newlyJudgedThoughtNr + "})\n" +
                                                   "MATCH (thought2:THOUGHT {nr: " + oldJudgedThoughtNr + "})\n" +
                                                   "MATCH (thought1) -[rel:SIMILAR]- (thought2)\n" +
                                                   "SET rel.similarity = rel.similarity + " + similarityChange + "\n" +
                                                   "RETURN rel.similarity", "rel.similarity");

            if (similarity == null) {
                setSimilarityRelationship(userNr, oldJudgedThoughtNr, similarityChange);
            }
        }
        results.close();
    }

    private void setJudgeRelationship(long userNr, long thoughtNr, Relationships relType) {
        setRelationship(NodeType.USER, userNr, NodeType.THOUGHT, thoughtNr, relType, "");
    }

    private void setSimilarityRelationship(long thoughtNr1, long thoughtNr2, int similarity) {
        setRelationship(NodeType.THOUGHT, thoughtNr1, NodeType.THOUGHT, thoughtNr2, Relationships.SIMILAR, " {similarity: " + similarity + "}");
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


    public Thought getRandomThought() {
        List<Recommendation<Node>> recommendations;
        Thought thought = new Thought("There are no Thoughts");

        try (Transaction tx = db.beginTx()) {
            ThoughtRecommendationEngine recoEngine = new ThoughtRecommendationEngine();

            Node user = db.findNodes(NodeType.USER).next();
            recommendations = recoEngine.recommend(user, new SimpleConfig(2));

            if (!recommendations.isEmpty()) {
                thought = new Thought((String) recommendations.get(0).getItem().getProperty("text"));
            }

            tx.success();
        }

        return thought;
    }


    // TODO: change
    public String getRecommendations(String id) {
        List<Recommendation<Node>> recommendations;
        StringBuilder sb = new StringBuilder();

        try (Transaction tx = db.beginTx()) {
            ThoughtRecommendationEngine recoEngine = new ThoughtRecommendationEngine();

            Node user = getUserByID(id);
            recommendations = recoEngine.recommend(user, new SimpleConfig(2));

            for (int i = 0; i < recommendations.size(); ++i) {
                sb.append(recommendations.get(i).getItem().getProperty("description"));
            }

            tx.success();
        }

        return sb.toString();
    }

    private Node getUserByID(String id) {
        return db.findNode(NodeType.USER, "id", id);
    }
}
