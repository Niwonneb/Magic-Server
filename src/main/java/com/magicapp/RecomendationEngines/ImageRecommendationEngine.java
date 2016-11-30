package com.magicapp.RecomendationEngines;

import com.graphaware.reco.generic.engine.RecommendationEngine;
import com.graphaware.reco.generic.filter.BlacklistBuilder;
import com.graphaware.reco.generic.filter.Filter;
import com.graphaware.reco.generic.post.PostProcessor;
import com.graphaware.reco.neo4j.engine.Neo4jTopLevelDelegatingRecommendationEngine;
import com.graphaware.reco.neo4j.filter.ExistingRelationshipBlacklistBuilder;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;

import static com.magicapp.Dao.Neo4jDao.Relationships.CREATED;
import static org.neo4j.graphdb.Direction.INCOMING;

public class ImageRecommendationEngine extends Neo4jTopLevelDelegatingRecommendationEngine {
    @Override
    protected List<RecommendationEngine<Node, Node>> engines() {
        return Arrays.<RecommendationEngine<Node, Node>>asList(
                new RandomImages(),
                new AdoredLikedEngine()
        );
    }

    @Override
    protected List<PostProcessor<Node, Node>> postProcessors() {
        return Arrays.<PostProcessor<Node, Node>>asList(
        );
    }

    @Override
    protected List<BlacklistBuilder<Node, Node>> blacklistBuilders() {
        return Arrays.<BlacklistBuilder<Node, Node>>asList(
                new ExistingRelationshipBlacklistBuilder(CREATED, INCOMING)
        );
    }

    @Override
    protected List<Filter<Node, Node>> filters() {
        return Arrays.<Filter<Node, Node>>asList(
        );
    }
}
