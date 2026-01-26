package com.ntu.lottery.service.engine.tree;

import org.springframework.stereotype.Component;

@Component
public class RuleTreeEngine {

    private final RuleTreeFactory factory;

    public RuleTreeEngine(RuleTreeFactory factory) {
        this.factory = factory;
    }

    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        RuleTreeNode root = factory.build();
        return root.evaluate(ctx);
    }
}
