package com.ntu.lottery.service.engine.tree;

/**
 * Base class for rule tree nodes, to ease extension.
 */
public abstract class RuleTreeNode {

    protected RuleTreeNode next;
    protected RuleTreeNode fallback;

    public RuleTreeNode next(RuleTreeNode next) {
        this.next = next;
        return this;
    }

    public RuleTreeNode fallback(RuleTreeNode fallback) {
        this.fallback = fallback;
        return this;
    }

    protected RuleTreeResult nextOrPass(RuleTreeContext ctx) {
        return next == null ? RuleTreeResult.pass() : next.evaluate(ctx);
    }

    protected RuleTreeResult fallbackOrReject(String message, RuleTreeContext ctx) {
        if (fallback != null) {
            return fallback.evaluate(ctx);
        }
        return RuleTreeResult.reject(message);
    }

    public abstract RuleTreeResult evaluate(RuleTreeContext ctx);
}
