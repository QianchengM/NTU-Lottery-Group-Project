package com.ntu.lottery.service.engine.tree;

public interface RuleTreeNode {
    RuleTreeResult evaluate(RuleTreeContext ctx);
}
