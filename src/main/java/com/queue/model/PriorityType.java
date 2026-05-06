package com.queue.model;

public enum PriorityType {
    EMERGENCY(1),
    ELDERLY(2),
    REGULAR(3);

    private final int level;

    PriorityType(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
