package com.ntu.lottery.service.engine.tree;

public class EndNode implements RuleTreeNode {
    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        return RuleTreeResult.pass();
    }
}
