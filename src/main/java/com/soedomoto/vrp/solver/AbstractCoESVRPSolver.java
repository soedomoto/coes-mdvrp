package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Set;

/**
 * Created by soedomoto on 12/02/17.
 */
public abstract class AbstractCoESVRPSolver extends AbstractVRPSolver implements Runnable, VRPSolver {
    protected int enumeratorSize;
    protected Long[] enumeratorIds;
    protected float[] enumeratorDepotXs;
    protected float[] enumeratorDepotYs;
    protected float[] enumeratorDurations;
    protected int[] enumeratorCapacities;
    protected int bsSize;
    protected Long[] bsIds;
    protected float[] bsCoordXs;
    protected float[] bsCoordYs;
    protected int[] bsDemands;
    protected float[][] c2cDistances;
    protected float[][] d2cDistances;

    public AbstractCoESVRPSolver(App app, String channel, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, brokerUrl);
    }

    public AbstractCoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl);
    }

    public AbstractCoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl, useAllEnumerator);
    }

    public AbstractCoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, String baseDir, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl, baseDir, useAllEnumerator);
    }

    @Override
    public void run() {
        super.run();

        // Vehicles
        if(useAllEnumerator) enumerators = allEnumerators;
        else enumerators = selfEnumerator;

        enumeratorSize = enumerators.size();
        enumeratorIds = new Long[enumeratorSize];
        enumeratorDepotXs = new float[enumeratorSize];
        enumeratorDepotYs = new float[enumeratorSize];
        enumeratorDurations = new float[enumeratorSize];
        enumeratorCapacities = new int[enumeratorSize];

        int idx = 0;
        for(Long eId : enumerators.keySet()) {
            long dId = enumerators.get(eId).getDepot();
            enumeratorIds[idx] = dId;
            enumeratorDepotXs[idx] = (float) allBses.get(dId).getLon();
            enumeratorDepotYs[idx] = (float) allBses.get(dId).getLat();
            enumeratorDurations[idx] = 0;
            enumeratorCapacities[idx] = (unassignedBses.size() / enumerators.size()) + 1;
            idx++;
        }

        // Customers
        bsSize = unassignedBses.size();
        bsIds = new Long[bsSize];
        bsCoordXs = new float[bsSize];
        bsCoordYs = new float[bsSize];
        bsDemands = new int[bsSize];

        idx = 0;
        for(CensusBlock bs : unassignedBses.values()) {
            bsIds[idx] = bs.getId();
            bsCoordXs[idx] = (float) bs.getLon();
            bsCoordYs[idx] = (float) bs.getLat();
            bsDemands[idx] = 1;
            idx++;
        }

        // Distances
        c2cDistances = new float[bsSize][bsSize];
        for(int x=0; x<c2cDistances.length; x++) {
            for(int y=0; y<c2cDistances[x].length; y++) {
                if(bsIds[x].longValue() != bsIds[y].longValue()) {
                    c2cDistances[x][y] = durationMatrix.get(bsIds[x]).get(bsIds[y]).floatValue();
                } else {
                    c2cDistances[x][y] = Float.MAX_VALUE;
                }
            }
        }

        d2cDistances = new float[enumeratorSize][bsSize];
        for (int x = 0; x < d2cDistances.length; x++) {
            for (int y = 0; y < d2cDistances[x].length; y++) {
                if (enumeratorIds[x].longValue() != bsIds[y].longValue()) {
                    d2cDistances[x][y] = durationMatrix.get(enumeratorIds[x]).get(bsIds[y]).floatValue();
                } else {
                    d2cDistances[x][y] = 0.0f;
                }
            }
        }
    }
}
