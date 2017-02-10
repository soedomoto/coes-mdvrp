package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.model.DistanceMatrix;
import com.soedomoto.vrp.model.Enumerator;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by soedomoto on 03/02/17.
 */
public abstract class AbstractVRPSolver implements VRPSolver, Runnable {
    protected static final Map<Long, Map<Long, Double>> DURATION_MATRIX = new LinkedHashMap();
    protected static final Map<Long, Map<Long, Double>> DISTANCE_MATRIX = new LinkedHashMap();

    protected final App app;
    protected final String channel;

    protected Map<Long, Enumerator> allEnumerators = new LinkedHashMap();
    protected Map<Long, CensusBlock> allBses = new LinkedHashMap();
    protected Map<Long, CensusBlock> unassignedBses = new LinkedHashMap();

    public AbstractVRPSolver(App app, String channel) throws SQLException {
        this.app = app;
        this.channel = channel;

        // Prepare Data
        for(Enumerator e : app.getEnumeratorDao().queryForAll()) {
            allEnumerators.put(e.getId(), e);
        }

        for(CensusBlock bs : app.getCensusBlockDao().queryForAll()) {
            allBses.put(bs.getId(), bs);
            if(bs.getAssignedTo() == null) unassignedBses.put(bs.getId(), bs);
        }

        if(DURATION_MATRIX.size() == 0 || DISTANCE_MATRIX.size() == 0) {
            for (DistanceMatrix m : app.getDistanceMatrixDao().queryForAll()) {
                if (!DURATION_MATRIX.keySet().contains(m.getLocationA()))
                    DURATION_MATRIX.put(m.getLocationA(), new HashMap());
                DURATION_MATRIX.get(m.getLocationA()).put(m.getLocationB(), m.getDuration());

                if (!DISTANCE_MATRIX.keySet().contains(m.getLocationA()))
                    DISTANCE_MATRIX.put(m.getLocationA(), new HashMap());
                DISTANCE_MATRIX.get(m.getLocationA()).put(m.getLocationB(), m.getDistance());
            }
        }
    }
}
