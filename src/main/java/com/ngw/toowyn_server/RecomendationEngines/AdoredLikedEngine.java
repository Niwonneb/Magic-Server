package com.ngw.toowyn_server.RecomendationEngines;

import com.graphaware.reco.generic.context.Context;
import com.graphaware.reco.generic.engine.SingleScoreRecommendationEngine;
import com.graphaware.reco.generic.result.PartialScore;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.Map;

public class AdoredLikedEngine extends SingleScoreRecommendationEngine<Node, Node> {

    private static double score = 1;

    @Override
    protected Map<Node, PartialScore> doRecommendSingle(Node node, Context<Node, Node> context) {
        Map<Node, PartialScore> result = new HashMap<>();

//        for (Relationship r1 : node.getRelationships(Relationships.ADORES, BOTH)) {
//            Node adoredUser = r1.getOtherNode(node);
//            for (Relationship r2 : adoredUser.getRelationships(Relationships.LIKED, Direction.OUTGOING)) {
//                Node recommendedImage = r2.getOtherNode(adoredUser);
//                addToResult(result, recommendedImage, new PartialScore((float) score));
//            }
//        }

        return result;
    }

    @Override
    public String name() {
        return "AdoredLikedEngine";
    }
}
