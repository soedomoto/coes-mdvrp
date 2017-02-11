package com.soedomoto.vrp.model.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by soedomoto on 07/01/17.
 */
@DatabaseTable
public class DistanceMatrix {
    @DatabaseField(columnName = "location_a")
    private long from;
    @DatabaseField(columnName = "location_b")
    private long to;
    @DatabaseField
    private double distance;
    @DatabaseField
    private double duration;


    public long getFrom() {
        return from;
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }
}
