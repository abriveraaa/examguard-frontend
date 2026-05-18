package com.example.examguard.model.exam.result;

import javafx.beans.property.*;

public class ExamRow {

    private final LongProperty examId = new SimpleLongProperty();
    private final StringProperty dateCreated = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty duration = new SimpleStringProperty();
    private final StringProperty assigned = new SimpleStringProperty();
    private final StringProperty takers = new SimpleStringProperty();
    private final StringProperty validity = new SimpleStringProperty();
    private final StringProperty createdBy = new SimpleStringProperty();
    private final StringProperty updatedBy = new SimpleStringProperty();

    public ExamRow(Long examId,
                   String dateCreated,
                   String title,
                   String status,
                   String duration,
                   String assigned,
                   String takers,
                   String validity,
                   String createdBy,
                   String updatedBy) {
        this.examId.set(examId == null ? 0L : examId);
        this.dateCreated.set(dateCreated);
        this.title.set(title);
        this.status.set(status);
        this.duration.set(duration);
        this.assigned.set(assigned);
        this.takers.set(takers);
        this.validity.set(validity);
        this.createdBy.set(createdBy);
        this.updatedBy.set(updatedBy);
    }

    public Long getExamId() {
        return examId.get();
    }

    public LongProperty examIdProperty() {
        return examId;
    }

    public String getDateCreated() {
        return dateCreated.get();
    }

    public StringProperty dateCreatedProperty() {
        return dateCreated;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getStatus() { return status.get(); }

    public StringProperty statusProperty() {
        return status;
    }

    public String getDuration() {
        return duration.get();
    }

    public StringProperty durationProperty() {
        return duration;
    }

    public String getAssigned() {
        return assigned.get();
    }

    public StringProperty assignedProperty() {
        return assigned;
    }

    public String getTakers() {
        return takers.get();
    }

    public StringProperty takersProperty() {
        return takers;
    }

    public String getValidity() { return validity.get(); }

    public StringProperty validityProperty() { return validity; }

    public String getCreatedBy() { return createdBy.get(); }

    public StringProperty createdByProperty() { return createdBy; }

    public String getUpdatedBy() { return updatedBy.get(); }

    public StringProperty updatedByProperty() { return updatedBy; }
}