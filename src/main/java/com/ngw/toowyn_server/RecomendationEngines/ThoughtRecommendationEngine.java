package com.ngw.toowyn_server.RecomendationEngines;

import com.graphaware.reco.generic.engine.RecommendationEngine;
import com.graphaware.reco.generic.filter.BlacklistBuilder;
import com.graphaware.reco.generic.filter.Filter;
import com.graphaware.reco.generic.post.PostProcessor;
import com.graphaware.reco.neo4j.engine.Neo4jTopLevelDelegatingRecommendationEngine;
import com.graphaware.reco.neo4j.filter.ExistingRelationshipBlacklistBuilder;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;

import static com.ngw.toowyn_server.Database.Neo4jDatabase.Relationships.DISLIKED;
import static com.ngw.toowyn_server.Database.Neo4jDatabase.Relationships.LIKED;
import static org.neo4j.graphdb.Direction.INCOMING;

public class ThoughtRecommendationEngine extends Neo4jTopLevelDelegatingRecommendationEngine {
    @Override
    protected List<RecommendationEngine<Node, Node>> engines() {
        return Arrays.<RecommendationEngine<Node, Node>>asList(
                new RandomThoughts()
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
                new ExistingRelationshipBlacklistBuilder(LIKED, INCOMING),
                new ExistingRelationshipBlacklistBuilder(DISLIKED, INCOMING)
        );
    }

    @Override
    protected List<Filter<Node, Node>> filters() {
        return Arrays.<Filter<Node, Node>>asList(
        );
    }
}
