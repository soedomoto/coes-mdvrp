package com.soedomoto.vrp.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by soedomoto on 07/01/17.
 */
@DatabaseTable
public class Enumerator {
    @DatabaseField(id = true)
    private long id;
    @DatabaseField
    private double lat;
    @DatabaseField
    private double lon;
    @DatabaseField
    private long depot;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getDepot() {
        return depot;
    }

    public void setDepot(long depot) {
        this.depot = depot;
    }
}
