package com.soedomoto.vrp.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by soedomoto on 09/01/17.
 */
@DatabaseTable
public class Subscriber {
    @DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
    private Long id;
    @DatabaseField
    private Long subscriber;

    @DatabaseField(columnName = "is_subscribed", defaultValue = "false")
    private boolean subscribed;
    @DatabaseField(columnName = "subscription_date")
    private Date subscriptionDate;

    @DatabaseField(columnName = "is_processed", defaultValue = "false")
    private boolean processed;
    @DatabaseField(columnName = "processing_date")
    private Date processingDate;

    @DatabaseField
    private String response;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Long subscriber) {
        this.subscriber = subscriber;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public Date getSubscriptionDate() {
        return subscriptionDate;
    }

    public void setSubscriptionDate(Date subscriptionDate) {
        this.subscriptionDate = subscriptionDate;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
