package com.ntu.lottery.service.engine.tree;

import lombok.Data;

@Data
public class RuleTreeResult {
    public enum Status { PASS, REJECT, FALLBACK }

    private Status status;
    private String message;
    private Long fallbackPrizeId;

    public static RuleTreeResult pass() {
        RuleTreeResult r = new RuleTreeResult();
        r.setStatus(Status.PASS);
        return r;
    }

    public static RuleTreeResult reject(String message) {
        RuleTreeResult r = new RuleTreeResult();
        r.setStatus(Status.REJECT);
        r.setMessage(message);
        return r;
    }

    public static RuleTreeResult fallback(Long prizeId) {
        RuleTreeResult r = new RuleTreeResult();
        r.setStatus(Status.FALLBACK);
        r.setFallbackPrizeId(prizeId);
        return r;
    }
}
