package com.soedomoto.vrp.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by soedomoto on 07/01/17.
 */
@DatabaseTable
public class CensusBlock {
    @DatabaseField(id = true)
    private long id;
    @DatabaseField
    private double lat;
    @DatabaseField
    private double lon;
    @DatabaseField(columnName = "service_time")
    private double serviceTime;
    @DatabaseField(columnName = "assigned_to")
    private Long assignedTo;
    @DatabaseField(columnName = "assign_date")
    private Date assignDate;
    @DatabaseField(columnName = "visited_by")
    private Long visitedBy;
    @DatabaseField(columnName = "visit_date")
    private Date visitDate;


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

    public double getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(double serviceTime) {
        this.serviceTime = serviceTime;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Date getAssignDate() {
        return assignDate;
    }

    public void setAssignDate(Date assignDate) {
        this.assignDate = assignDate;
    }

    public Long getVisitedBy() {
        return visitedBy;
    }

    public void setVisitedBy(Long visitedBy) {
        this.visitedBy = visitedBy;
    }

    public Date getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }
}
