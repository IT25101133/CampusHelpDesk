package com.sliit.helpdesk.report.dto;

// Resolution Time Avg Response is part of the campus help desk dto code.

public class ResolutionTimeAvgResponse {

    private double averageHours;
    private long resolvedCount;

    public double getAverageHours() {
        return averageHours;
    }

    public void setAverageHours(double averageHours) {
        this.averageHours = averageHours;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
}
