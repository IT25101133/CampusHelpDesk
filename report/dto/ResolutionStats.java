package com.sliit.helpdesk.report.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Average resolution hours overall, by priority, and by category
 * for RESOLVED and CLOSED tickets that have a {@code resolvedAt} timestamp.
 */
public class ResolutionStats {

    private double overallAverageHours;
    private long resolvedCount;
    private List<GroupAverage> byPriority = new ArrayList<>();
    private List<GroupAverage> byCategory = new ArrayList<>();

    public double getOverallAverageHours() {
        return overallAverageHours;
    }

    public void setOverallAverageHours(double overallAverageHours) {
        this.overallAverageHours = overallAverageHours;
    }

    public long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public List<GroupAverage> getByPriority() {
        return byPriority;
    }

    public void setByPriority(List<GroupAverage> byPriority) {
        this.byPriority = byPriority;
    }

    public List<GroupAverage> getByCategory() {
        return byCategory;
    }

    public void setByCategory(List<GroupAverage> byCategory) {
        this.byCategory = byCategory;
    }

    public static class GroupAverage {
        private String name;
        private double averageHours;
        private long count;

        public GroupAverage() {
        }

        public GroupAverage(String name, double averageHours, long count) {
            this.name = name;
            this.averageHours = averageHours;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getAverageHours() {
            return averageHours;
        }

        public void setAverageHours(double averageHours) {
            this.averageHours = averageHours;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
