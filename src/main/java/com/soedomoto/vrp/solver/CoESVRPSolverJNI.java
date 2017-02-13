package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.solution.Point;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 03/02/17.
 */
public abstract class CoESVRPSolverJNI extends AbstractCoESVRPSolver implements Runnable, VRPSolver {
    private final static Logger LOG = Logger.getLogger(CoESVRPSolverJNI.class.getName());

    public CoESVRPSolverJNI(App app, String channel, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, brokerUrl);
    }

    public CoESVRPSolverJNI(App app, String channel, Set<String> otherChannels, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl);
    }

    public CoESVRPSolverJNI(App app, String channel, Set<String> otherChannels, String brokerUrl, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl, useAllEnumerator);
    }

    public void run() {
        super.run();

        // Notify
//        this.exportProblem();
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
                        onSolution(this.channel, String.valueOf(enumeratorId), depot, destination, duration, currBs.getServiceTime());
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        onFinished(this.channel, Arrays.asList(enumeratorIds), unassignedBses.keySet());
    }

    private void exportProblem() {
        List<String> lstStrProblems = new ArrayList<String>();
        lstStrProblems.add(StringUtils.join(new String[] {String.valueOf(2), String.valueOf(enumeratorSize), String.valueOf(bsSize), String.valueOf(enumeratorSize)}, " "));
        for(int e=0; e<enumeratorSize; e++) {
            lstStrProblems.add(StringUtils.join(new String[] {String.valueOf(enumeratorDurations[e]), String.valueOf(enumeratorCapacities[e])}, " "));
        }
        int s=1;
        for(int b=0; b<bsSize; b++) {
            lstStrProblems.add(StringUtils.join(new String[] {String.valueOf(s), String.valueOf(bsCoordXs[b]), String.valueOf(bsCoordYs[b]), String.valueOf(0), String.valueOf(bsDemands[b]), "1 4 1 2 4 8"}, " "));
            s++;
        }
        for(int e=0; e<enumeratorSize; e++) {
            lstStrProblems.add(StringUtils.join(new String[] {String.valueOf(s), String.valueOf(enumeratorDepotXs[e]), String.valueOf(enumeratorDepotYs[e]), "0 0 0 0"}, " "));
            s++;
        }
        for(int b=0; b<bsSize; b++) {
            String[] cd = new String[bsSize];
            for(int c=0; c<bsSize; c++) {
                cd[c] = String.valueOf(c2cDistances[b][c]);
            }
            lstStrProblems.add(StringUtils.join(cd, " "));
        }
        for(int e=0; e<enumeratorSize; e++) {
            String[] cd = new String[bsSize];
            for(int c=0; c<bsSize; c++) {
                cd[c] = String.valueOf(d2cDistances[e][c]);
            }
            lstStrProblems.add(StringUtils.join(cd, " "));
        }

        String strProblem = StringUtils.join(lstStrProblems, "\n");
        System.out.println(strProblem);
    }
}
