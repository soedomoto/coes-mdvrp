package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.solution.Point;
import org.apache.log4j.Logger;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 03/02/17.
 */
public abstract class CoESVRPSolver extends AbstractVRPSolver implements Runnable, VRPSolver {
    private final static Logger LOG = Logger.getLogger(CoESVRPSolver.class.getName());

    public CoESVRPSolver(App app, String channel, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, brokerUrl);
    }

    public void run() {
        super.run();

        // Vehicles
        int enumeratorSize = allEnumerators.size();
        Long[] enumeratorIds = new Long[enumeratorSize];
        float[] enumeratorDepotXs = new float[enumeratorSize];
        float[] enumeratorDepotYs = new float[enumeratorSize];
        float[] enumeratorDurations = new float[enumeratorSize];
        int[] enumeratorCapacities = new int[enumeratorSize];

        int idx = 0;
        for(Long eId : allEnumerators.keySet()) {
            long dId = allEnumerators.get(eId).getDepot();
            enumeratorIds[idx] = dId;
            enumeratorDepotXs[idx] = (float) allBses.get(dId).getLon();
            enumeratorDepotYs[idx] = (float) allBses.get(dId).getLat();
            enumeratorDurations[idx] = 0;
            enumeratorCapacities[idx] = (unassignedBses.size() / allEnumerators.size()) + 1;
            idx++;
        }

        // Customers
        int bsSize = unassignedBses.size();
        Long[] bsIds = new Long[bsSize];
        float[] bsCoordXs = new float[bsSize];
        float[] bsCoordYs = new float[bsSize];
        int[] bsDemands = new int[bsSize];

        idx = 0;
        for(CensusBlock bs : unassignedBses.values()) {
            bsIds[idx] = bs.getId();
            bsCoordXs[idx] = (float) bs.getLon();
            bsCoordYs[idx] = (float) bs.getLat();
            bsDemands[idx] = 1;
            idx++;
        }

        // Distances
        float[][] c2cDistances = new float[bsSize][bsSize];
        for(int x=0; x<c2cDistances.length; x++) {
            for(int y=0; y<c2cDistances[x].length; y++) {
                if(bsIds[x].longValue() != bsIds[y].longValue()) {
                    c2cDistances[x][y] = durationMatrix.get(bsIds[x]).get(bsIds[y]).floatValue();
                } else {
                    c2cDistances[x][y] = Float.MAX_VALUE;
                }
            }
        }

        float[][] d2cDistances = new float[enumeratorSize][bsSize];
        for (int x = 0; x < d2cDistances.length; x++) {
            for (int y = 0; y < d2cDistances[x].length; y++) {
                if (enumeratorIds[x].longValue() != bsIds[y].longValue()) {
                    d2cDistances[x][y] = durationMatrix.get(enumeratorIds[x]).get(bsIds[y]).floatValue();
                } else {
                    d2cDistances[x][y] = 0.0f;
                }
            }
        }

        // Notify
        this.onStarted(this.channel, Arrays.asList(enumeratorIds), unassignedBses.keySet());

        // Solver
        CoESVRPJNI coes = new CoESVRPJNI();
        coes.setVehicles(enumeratorSize, enumeratorDepotXs, enumeratorDepotYs, enumeratorDurations, enumeratorCapacities);
        coes.setCustomers(bsSize, bsCoordXs, bsCoordYs, bsDemands);
        coes.setCustomerToCustomerDistance(bsSize, bsSize, c2cDistances);
        coes.setDepotToCustomerDistance(enumeratorSize, bsSize, d2cDistances);
        coes.configSetNumSubpopulation(3);
        coes.configStopWhenMaxExecutionTime(60);
        coes.configStopWhenMaxTimeWithoutUpdate(40);
        coes.configStopWhenNumGeneration(100);
        int code = coes.solve();
        if(code == 0) {
            int[] enumeratorIdxs = coes.getSolutionDepots();
            int[] routeIdxs = coes.getSolutionRoutes();
            float[] routeCosts = coes.getSolutionCosts();
            int[] routeDemands = coes.getSolutionDemands();
            int[][] destinationBsIdxs = coes.getSolutionCustomers();

            try {
                Map<Long, List<Long>> enumeratorBsIdRoutes = new HashMap();
                Map<Long, Integer> enumeratorDemands = new HashMap();
                Map<Long, Float> enumeratorCosts = new HashMap();
                for (int i = 0; i < enumeratorIdxs.length; i++) {
                    long eId = enumeratorIds[enumeratorIdxs[i]];

                    if(! enumeratorBsIdRoutes.keySet().contains(eId)) enumeratorBsIdRoutes.put(eId, new ArrayList());
                    if(! enumeratorDemands.keySet().contains(eId)) enumeratorDemands.put(eId, 0);
                    if(! enumeratorCosts.keySet().contains(eId)) enumeratorCosts.put(eId, 0.0f);

                    for (int bsIdx : destinationBsIdxs[i]) {
                        try {
                            if (bsIdx > 0) enumeratorBsIdRoutes.get(eId).add(bsIds[bsIdx-1]);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }

                    enumeratorDemands.put(eId, enumeratorDemands.get(eId) + routeDemands[i]);
                    enumeratorCosts.put(eId, enumeratorCosts.get(eId) + routeCosts[i]);
                }

                for(long enumeratorId : enumeratorBsIdRoutes.keySet()) {
                    int enumeratorIdx = Arrays.asList(enumeratorIds).indexOf(enumeratorId);
                    if(enumeratorBsIdRoutes.get(enumeratorId).size() > 0) {
                        Long bsId = enumeratorBsIdRoutes.get(enumeratorId).get(0);
                        int bsIdx = Arrays.asList(bsIds).indexOf(bsId);

                        Point depot = new Point();
                        depot.id = enumeratorId;
                        depot.latitude = Double.valueOf(enumeratorDepotYs[enumeratorIdx]);
                        depot.longitude = Double.valueOf(enumeratorDepotXs[enumeratorIdx]);

                        Point destination = new Point();
                        destination.id = bsId;
                        destination.latitude = Double.valueOf(bsCoordYs[bsIdx]);
                        destination.longitude = Double.valueOf(bsCoordXs[bsIdx]);

                        double duration = 0.0;
                        if (durationMatrix.keySet().contains(depot)) {
                            if (durationMatrix.get(depot).keySet().contains(bsId)) {
                                duration = durationMatrix.get(depot).get(bsId);
                            }
                        }

                        CensusBlock currBs = allBses.get(bsId);
                        onSolution(String.valueOf(enumeratorId), depot, destination, duration, currBs.getServiceTime());
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        onFinished(this.channel, Arrays.asList(enumeratorIds), unassignedBses.keySet());
    }
}
