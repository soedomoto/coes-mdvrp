package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 03/02/17.
 */
public abstract class CoESVRPSolver extends AbstractVRPSolver implements Runnable, VRPSolver {
    public CoESVRPSolver(App app, String channel) throws SQLException {
        super(app, channel);
    }

    public void run() {
        // Vehicles
        int vSize = allEnumerators.size();
        Long[] vIds = new Long[vSize];
        float[] vXDepots = new float[vSize];
        float[] vYDepots = new float[vSize];
        float[] vDurations = new float[vSize];
        int[] vCapacities = new int[vSize];

        int iE = 0;
        for(Long eId : allEnumerators.keySet()) {
            long dId = allEnumerators.get(eId).getDepot();
            vIds[iE] = dId;
            vXDepots[iE] = (float) allBses.get(dId).getLon();
            vYDepots[iE] = (float) allBses.get(dId).getLat();
            vDurations[iE] = 0;
            vCapacities[iE] = (unassignedBses.size() / allEnumerators.size()) + 1;
            iE++;
        }

        // Customers
        int cSize = unassignedBses.size();
        Long[] cIds = new Long[cSize];
        float[] cXDepots = new float[cSize];
        float[] cYDepots = new float[cSize];
        int[] cDemands = new int[cSize];

        iE = 0;
        for(CensusBlock c : unassignedBses.values()) {
            cIds[iE] = c.getId();
            cXDepots[iE] = (float) c.getLon();
            cYDepots[iE] = (float) c.getLat();
            cDemands[iE] = Integer.valueOf(1);
            iE++;
        }

        // Distances
        float[][] c2cDistances = new float[cSize][cSize];
        for(int x=0; x<c2cDistances.length; x++) {
            for(int y=0; y<c2cDistances[x].length; y++) {
                if(cIds[x].longValue() != cIds[y].longValue()) {
                    c2cDistances[x][y] = DURATION_MATRIX.get(cIds[x]).get(cIds[y]).floatValue();
                } else {
                    c2cDistances[x][y] = Float.MAX_VALUE;
                }
            }
        }

        float[][] d2cDistances = new float[vSize][cSize];
        for (int x = 0; x < d2cDistances.length; x++) {
            for (int y = 0; y < d2cDistances[x].length; y++) {
                if (vIds[x].longValue() != cIds[y].longValue()) {
                    d2cDistances[x][y] = DURATION_MATRIX.get(vIds[x]).get(cIds[y]).floatValue();
                } else {
                    d2cDistances[x][y] = 0.0f;
                }
            }
        }

        // Notify
        this.onStarted(this.channel, Arrays.asList(vIds), unassignedBses.keySet());

        // Solver
        CoESVRPJNI coes = new CoESVRPJNI();
        coes.setVehicles(vSize, vXDepots, vYDepots, vDurations, vCapacities);
        coes.setCustomers(cSize, cXDepots, cYDepots, cDemands);
        coes.setCustomerToCustomerDistance(cSize, cSize, c2cDistances);
        coes.setDepotToCustomerDistance(vSize, cSize, d2cDistances);
        coes.configSetNumSubpopulation(1);
        coes.configStopWhenMaxExecutionTime(60);
        coes.configStopWhenMaxTimeWithoutUpdate(40);
        coes.configStopWhenNumGeneration(100);
        int code = coes.solve();
        if(code == 0) {
            int[] solDepots = coes.getSolutionDepots();
            int[] solRoutes = coes.getSolutionRoutes();
            float[] solCosts = coes.getSolutionCosts();
            int[] solDemands = coes.getSolutionDemands();
            int[][] solCustomers = coes.getSolutionCustomers();

            try {
                Map<Long, List<Long>> routes = new HashMap();
                Map<Long, Integer> demands = new HashMap();
                Map<Long, Float> costs = new HashMap();
                for (int i = 0; i < solDepots.length; i++) {
                    long eId = vIds[solDepots[i]];

                    routes.putIfAbsent(eId, new ArrayList());
                    demands.putIfAbsent(eId, 0);
                    costs.putIfAbsent(eId, 0.0f);

                    for (int c : solCustomers[i]) {
                        try {
                            if (c > 0) routes.get(eId).add(cIds[c - 1]);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    demands.put(eId, demands.get(eId) + solDemands[i]);
                    costs.put(eId, costs.get(eId) + solCosts[i]);
                }

                //System.out.println(new Gson().toJson(routes));
                for(long depot : routes.keySet()) {
                    int dIdx = Arrays.asList(vIds).indexOf(depot);
                    Long customer = routes.get(depot).get(0);
                    int cIdx = Arrays.asList(cIds).indexOf(customer);

                    Map<String, Object> routeVehicleMap = new HashMap();
                    routeVehicleMap.put("depot", depot);
                    routeVehicleMap.put("depot-coord", new Double[] {
                            Double.valueOf(vYDepots[dIdx]),
                            Double.valueOf(vXDepots[dIdx])});

                    Map<String, Object> activityMap = new HashMap();
                    activityMap.put("location", customer);
                    activityMap.put("location-coord", new Double[] {
                            Double.valueOf(cYDepots[cIdx]),
                            Double.valueOf(cXDepots[cIdx])});

                    double duration = 0.0;
                    if(DURATION_MATRIX.keySet().contains(depot)) {
                        if(DURATION_MATRIX.get(depot).keySet().contains(customer)) {
                            duration = DURATION_MATRIX.get(depot).get(customer);
                        }
                    }

                    CensusBlock currBs = allBses.get(customer);

                    onSolution(String.valueOf(depot), routeVehicleMap, activityMap, duration, currBs.getServiceTime());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        onFinished(this.channel, Arrays.asList(vIds), unassignedBses.keySet());
    }

//    public static void main(String args[]) throws IOException {
//        List<String> problemLines = IOUtils.readLines(new FileInputStream(args[0]), Charset.defaultCharset());
//        List<String> solutionLines = IOUtils.readLines(new FileInputStream(args[1]), Charset.defaultCharset());
//
//        String[] ivcd = problemLines.get(0).trim().split("\\s+");
//        int vSize = Integer.parseInt(ivcd[1]);
//        int cSize = Integer.parseInt(ivcd[2]);
//        int dSize = Integer.parseInt(ivcd[3]);
//        float[] vDurations = new float[vSize];
//        int[] vCapacities = new int[vSize];
//        float[] cXDepots = new float[cSize];
//        float[] cYDepots = new float[cSize];
//        int[] cDemands = new int[cSize];
//        float[] vXDepots = new float[vSize];
//        float[] vYDepots = new float[vSize];
//
//        for (int i = 0; i < dSize; ++i) {
//            String[] dc = problemLines.get(i + 1).trim().split("\\s+");
//            vDurations[i] = Float.valueOf(dc[0]);
//            vCapacities[i] = Integer.valueOf(dc[1]);
//        }
//
//        for (int i = 0; i < cSize; ++i) {
//            String[] ixytd = problemLines.get(i + dSize + 1).trim().split("\\s+");
//
//            cXDepots[i] = Float.parseFloat(ixytd[1]);
//            cYDepots[i] = Float.parseFloat(ixytd[2]);
//            cDemands[i] = Integer.valueOf(ixytd[4]);
//        }
//
//        for (int i = 0; i < dSize; ++i) {
//            String[] ixy = problemLines.get(i + dSize + cSize + 1).split("\\s+");
//
//            vXDepots[i] = Float.parseFloat(ixy[1]);
//            vYDepots[i] = Float.parseFloat(ixy[2]);
//        }
//
//        String[] b = solutionLines.get(0).trim().split("\\s+");
//        float bestKnowSolution = Float.parseFloat(b[0]);
//
//        final CoESVRPJNI coes = new CoESVRPJNI();
//        coes.setVehicles(vSize, vXDepots, vYDepots, vDurations, vCapacities);
//        coes.setCustomers(cSize, cXDepots, cYDepots, cDemands);
//        int code = coes.solve();
//        System.out.println(code);
//    }
}
