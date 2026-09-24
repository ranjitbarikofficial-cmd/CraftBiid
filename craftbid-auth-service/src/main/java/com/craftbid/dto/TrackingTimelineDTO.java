package com.craftbid.dto;

import java.time.LocalDateTime;

public class TrackingTimelineDTO {

    private String stepKey;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private boolean completed;
    private boolean current;

    public TrackingTimelineDTO() {
    }

    public TrackingTimelineDTO(String stepKey, String title, String description, LocalDateTime timestamp, boolean completed, boolean current) {
        this.stepKey = stepKey;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.completed = completed;
        this.current = current;
    }

    public String getStepKey() {
        return stepKey;
    }

    public void setStepKey(String stepKey) {
        this.stepKey = stepKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
