package com.ngw.toowyn_server.Service;

import com.ngw.toowyn_server.Database.Neo4jDatabase;
import com.ngw.toowyn_server.Entity.Thought;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThoughtService {

    @Autowired
    private Neo4jDatabase neo4JDatabase;

    public ThoughtService() {}

    public void createExampleNodes() {
        neo4JDatabase.createUserIfNotExists("abc");

        long img1 = neo4JDatabase.createThought("s1");
        long img2 = neo4JDatabase.createThought("s2");
        long img3 = neo4JDatabase.createThought("s3");
    }

    public void createThought(String text) {
        neo4JDatabase.createThought(text);
    }

    public String getRecommendations(String id) {
        return neo4JDatabase.getRecommendations(id);
    }

    public Thought getRandomThought() {
        return neo4JDatabase.getRandomThought();
    }
}
