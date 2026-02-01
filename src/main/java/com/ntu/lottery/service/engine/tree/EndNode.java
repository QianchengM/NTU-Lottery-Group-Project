package com.ntu.lottery.service.engine.tree;

public class EndNode extends RuleTreeNode {
    @Override
    public RuleTreeResult evaluate(RuleTreeContext ctx) {
        return RuleTreeResult.pass();
    }
}
