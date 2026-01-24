package com.ntu.lottery.service.dto;

/**
 * Cached prize configuration used by assemble + draw.
 */
public class PrizeConfig {
    private Long id;
    private Long activityId;
    private String name;
    /**
     * 0 = "thanks" (non-stock), !=0 = real prize (has stock)
     */
    private Integer type;
    private Integer stock;
    /**
     * Probability in the same unit as DB (e.g. sum to 100)
     */
    private Integer probability;
    private Integer pointCost;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public Integer getPointCost() {
        return pointCost;
    }

    public void setPointCost(Integer pointCost) {
        this.pointCost = pointCost;
    }
}
