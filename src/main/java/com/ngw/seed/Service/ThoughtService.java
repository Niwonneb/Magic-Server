package com.ngw.seed.Service;

import com.ngw.seed.Database.Neo4jDatabase;
import com.ngw.seed.CommunicationObjects.Thought;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThoughtService {

    @Autowired
    private Neo4jDatabase neo4JDatabase;

    public ThoughtService() {}

    public void createThought(String text, String idBefore, boolean likedBefore) {
        neo4JDatabase.createNewThought(text, idBefore, likedBefore);
    }

    public void rateThought(String idBefore, boolean likedBefore, String idNow, boolean likedNow) {
        neo4JDatabase.rateThought(idBefore, likedBefore, idNow, likedNow);
    }

    public Thought getNextThought(String idBefore, boolean likedBefore) {
        return neo4JDatabase.getNextThought(idBefore, likedBefore);
    }

    public Thought getThoughtFromStart() {
        return neo4JDatabase.getThoughtFromStart();
    }
}
