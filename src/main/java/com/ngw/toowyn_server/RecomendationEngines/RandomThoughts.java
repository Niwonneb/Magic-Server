package com.ngw.toowyn_server.RecomendationEngines;

import com.graphaware.common.policy.BaseNodeInclusionPolicy;
import com.graphaware.reco.generic.context.Context;
import com.graphaware.reco.generic.policy.ParticipationPolicy;
import com.graphaware.reco.neo4j.engine.RandomRecommendations;
import com.ngw.toowyn_server.Database.Neo4jDatabase;
import org.neo4j.graphdb.Node;
import com.graphaware.common.policy.NodeInclusionPolicy;

public class RandomThoughts extends RandomRecommendations {

    @Override
    public String name() {
        return "random";
    }

    @Override
    protected NodeInclusionPolicy getPolicy() {
        return new BaseNodeInclusionPolicy() {
            @Override
            public boolean include(Node node) {
                return node.hasLabel(Neo4jDatabase.NodeType.THOUGHT);
            }
        };
    }

    @Override
    public ParticipationPolicy<Node, Node> participationPolicy(Context context) {
        return ParticipationPolicy.IF_MORE_RESULTS_NEEDED_AND_ENOUGH_TIME;
    }
}
