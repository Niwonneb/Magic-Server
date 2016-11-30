package com.magicapp.RecomendationEngines;

import com.graphaware.common.policy.BaseNodeInclusionPolicy;
import com.graphaware.reco.generic.context.Context;
import com.graphaware.reco.generic.policy.ParticipationPolicy;
import com.graphaware.reco.neo4j.engine.RandomRecommendations;
import com.magicapp.Dao.Neo4jDao;
import org.neo4j.graphdb.Node;
import com.graphaware.common.policy.NodeInclusionPolicy;

public class RandomImages extends RandomRecommendations {

    @Override
    public String name() {
        return "random";
    }

    @Override
    protected NodeInclusionPolicy getPolicy() {
        return new BaseNodeInclusionPolicy() {
            @Override
            public boolean include(Node node) {
                return node.hasLabel(Neo4jDao.NodeType.IMAGE);
            }
        };
    }

    @Override
    public ParticipationPolicy<Node, Node> participationPolicy(Context context) {
        return ParticipationPolicy.IF_MORE_RESULTS_NEEDED_AND_ENOUGH_TIME;
    }
}
