package com.magicapp.Service;

import com.graphaware.reco.generic.config.SimpleConfig;
import com.graphaware.reco.generic.result.Recommendation;
import com.magicapp.Dao.Neo4jDao;
import com.magicapp.RecomendationEngines.ImageRecommendationEngine;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageService {

    @Autowired
    private Neo4jDao neo4jDao;

    public ImageService() {}

    public void createExampleNodes() {
        String date = "1.1.2000";
        long user1 = neo4jDao.createUser(date);
        long user2 = neo4jDao.createUser(date);
        long user3 = neo4jDao.createUser(date);

        long img1 = neo4jDao.createImage(user1, date, "the 1. image", "");
        long img2 = neo4jDao.createImage(user1, date, "what an ugly pic", "");
        long img3 = neo4jDao.createImage(user2, date, "good image", "");
        long img4 = neo4jDao.createImage(user2, date, "bild is gut", "");
        long img5 = neo4jDao.createImage(user2, date, "ganz neu", "");
        long img6 = neo4jDao.createImage(user1, date, "6", "");
        long img7 = neo4jDao.createImage(user3, date, "7", "");

        neo4jDao.omitImage(user2, img1, date);
        neo4jDao.dislikeImage(user2, img2, date);
        neo4jDao.likeImage(user1, img3, date);
        neo4jDao.likeImage(user1, img4, date);
        neo4jDao.likeImage(user2, img6, date);
        neo4jDao.likeImage(user2, img7, date);
        neo4jDao.omitImage(user3, img4, date);
    }

    public void createImage(String description, String path) {
        neo4jDao.createImage(1, "1.1.2001", description, path);
    }

    public String getRecommendations(int nr) {
        return neo4jDao.getRecommendations(nr);
    }
}
