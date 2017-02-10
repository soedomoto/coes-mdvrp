package com.soedomoto.vrp.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by soedomoto on 07/01/17.
 */
@DatabaseTable
public class DistanceMatrix {
    @DatabaseField(columnName = "location_a")
    private long locationA;
    @DatabaseField(columnName = "location_b")
    private long locationB;
    @DatabaseField
    private double distance;
    @DatabaseField
    private double duration;


    public long getLocationA() {
        return locationA;
    }

    public void setLocationA(long locationA) {
        this.locationA = locationA;
    }

    public long getLocationB() {
        return locationB;
    }

    public void setLocationB(long locationB) {
        this.locationB = locationB;
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
