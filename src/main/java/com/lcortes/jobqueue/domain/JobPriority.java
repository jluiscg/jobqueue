package com.lcortes.jobqueue.domain;

public enum JobPriority {
    LOW(0),
    NORMAL(5),
    HIGH(10),
    CRITICAL(20);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static JobPriority fromValue(int value) {
        for (JobPriority priority : values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Unknown JobPriority value: " + value);
    }
}