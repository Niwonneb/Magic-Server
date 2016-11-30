package com.magicapp.RecomendationEngines;

import com.graphaware.reco.generic.config.SimpleConfig;
import com.graphaware.reco.generic.context.Context;
import com.graphaware.reco.generic.engine.RecommendationEngine;
import com.graphaware.reco.generic.engine.SingleScoreRecommendationEngine;
import com.graphaware.reco.generic.policy.ParticipationPolicy;
import com.graphaware.reco.generic.result.PartialScore;
import com.graphaware.reco.generic.result.Recommendations;
import com.graphaware.reco.neo4j.engine.SomethingInCommon;
import com.magicapp.Dao.Neo4jDao;
import com.magicapp.Dao.Neo4jDao.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.HashMap;
import java.util.Map;

import static com.graphaware.reco.neo4j.util.DirectionUtils.reverse;
import static org.neo4j.graphdb.Direction.BOTH;

public class AdoredLikedEngine extends SingleScoreRecommendationEngine<Node, Node> {

    private static double score = 1;

    @Override
    protected Map<Node, PartialScore> doRecommendSingle(Node node, Context<Node, Node> context) {
        Map<Node, PartialScore> result = new HashMap<>();

        for (Relationship r1 : node.getRelationships(Relationships.ADORES, BOTH)) {
            Node adoredUser = r1.getOtherNode(node);
            for (Relationship r2 : adoredUser.getRelationships(Relationships.LIKED, Direction.OUTGOING)) {
                Node recommendedImage = r2.getOtherNode(adoredUser);
                addToResult(result, recommendedImage, new PartialScore((float) score));
            }
        }

        return result;
    }

    @Override
    public String name() {
        return "AdoredLikedEngine";
    }
}
