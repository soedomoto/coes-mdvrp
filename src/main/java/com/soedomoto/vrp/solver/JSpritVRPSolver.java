package com.soedomoto.vrp.solver;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.model.Enumerator;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 19/01/17.
 */
public abstract class JSpritVRPSolver extends AbstractVRPSolver implements Runnable, VRPSolver {
    private static final Logger LOG = Logger.getLogger(JSpritVRPSolver.class.getName());

    public JSpritVRPSolver(App app, String channel) throws SQLException {
        super(app, channel);
    }

    public void run() {
        // Define problem
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance().setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);

        // Define service locations
        for(CensusBlock bs : unassignedBses.values()) {
            Service.Builder builder = Service.Builder.newInstance(String.valueOf(bs.getId()));
            builder.addSizeDimension(0, 1);
            //builder.setTimeWindow(TimeWindow.newInstance(0.0, 0.0));
            //builder.setServiceTime(Double.parseDouble(line[3]));

            Location loc = Location.Builder.newInstance()
                    .setId(String.valueOf(bs.getId()))
                    .setCoordinate(Coordinate.newInstance(bs.getLon(), bs.getLat()))
                    .build();
            builder.setLocation(loc);

            Service node = builder.build();
            vrpBuilder.addJob(node);
        }

        // Define vehicles
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("enumerator");
        vehicleTypeBuilder.addCapacityDimension(0, (unassignedBses.size() / allEnumerators.size()) + 1);
        //vehicleTypeBuilder.setCostPerDistance(1.0);
        vehicleTypeBuilder.setCostPerDistance(0);
        vehicleTypeBuilder.setCostPerTransportTime(1);
        vehicleTypeBuilder.setCostPerServiceTime(1);
        VehicleType vehicleType = vehicleTypeBuilder.build();


        Collection<Enumerator> enumerators = allEnumerators.values();
        List<Long> processingDepots = new ArrayList<Long>();
        for(Enumerator e : enumerators) {
            VehicleImpl.Builder builder = VehicleImpl.Builder.newInstance(String.valueOf(e.getId()));

            CensusBlock bs = allBses.get(e.getDepot()); //censusBlockDao.queryForId(e.depot);
            Location loc = Location.Builder.newInstance()
                    .setId(String.valueOf(bs.getId()))
                    .setCoordinate(Coordinate.newInstance(bs.getLon(), bs.getLat()))
                    .build();
            builder.setStartLocation(loc);

            builder.setType(vehicleType);
            VehicleImpl vehicle = builder.build();
            vrpBuilder.addVehicle(vehicle);

            processingDepots.add(bs.getId());
        }

        // Define cost matrix
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder
                .newInstance(true);

        for(Long a : DURATION_MATRIX.keySet()) {
            for(Long b : DURATION_MATRIX.get(a).keySet()) {
                costMatrixBuilder.addTransportDistance(String.valueOf(a), String.valueOf(b), DISTANCE_MATRIX.get(a).get(b));
                costMatrixBuilder.addTransportTime(String.valueOf(a), String.valueOf(b), DURATION_MATRIX.get(a).get(b));
            }
        }

        VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();
        vrpBuilder.setRoutingCost(costMatrix);
        LOG.debug(String.format("Using: %s enumerator(s), %s location(s)", 1, unassignedBses.size()));


        // Build problem
        VehicleRoutingProblem problem = vrpBuilder.build();
        this.onStarted(this.channel, processingDepots, unassignedBses.keySet());


        // Define algorithm
        int numProcessors = Runtime.getRuntime().availableProcessors();
        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                .setProperty(Jsprit.Parameter.THREADS, String.valueOf(numProcessors))
                .buildAlgorithm();
        algorithm.setMaxIterations(app.getMaxIteration());
        LOG.debug(String.format("Finding solution using %s thread(s) and %s iteration(s)", numProcessors, app.getMaxIteration()));


        // Find solutions
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        LOG.debug(String.format("%s solution(s) found. Finding the best", solutions.size()));
        VehicleRoutingProblemSolution solution = this.findBest(solutions);


        // Best solution found --> parse routes
        for(VehicleRoute route : solution.getRoutes()) {
            Vehicle routeVehicle = route.getVehicle();
            List<Job> jobs = new ArrayList(route.getTourActivities().getJobs());
            if(jobs.size() > 0) {
                Job job = jobs.get(0);
                if (job instanceof Service) {
                    Service activity = (Service) job;

                    double duration = 0.0;
                    long a = Long.parseLong(routeVehicle.getStartLocation().getId());
                    long b = Long.parseLong(activity.getLocation().getId());
                    if(DURATION_MATRIX.keySet().contains(a)) {
                        if(DURATION_MATRIX.get(a).keySet().contains(b)) {
                            duration = DURATION_MATRIX.get(a).get(b);
                        }
                    }

                    CensusBlock currBs = allBses.get(Long.valueOf(activity.getLocation().getId()));

                    Map<String, Object> routeVehicleMap = new HashMap();
                    routeVehicleMap.put("depot", routeVehicle.getStartLocation().getId());
                    routeVehicleMap.put("depot-coord", new Double[] {
                            routeVehicle.getStartLocation().getCoordinate().getY(),
                            routeVehicle.getStartLocation().getCoordinate().getX()});

                    Map<String, Object> activityMap = new HashMap();
                    activityMap.put("location", activity.getLocation().getId());
                    activityMap.put("location-coord", new Double[] {
                            activity.getLocation().getCoordinate().getY(),
                            activity.getLocation().getCoordinate().getX()});

                    onSolution(routeVehicle.getStartLocation().getId(), routeVehicleMap, activityMap, duration, currBs.getServiceTime());
                }
            }

            // LOG.debug(String.format("%s = %s", routeVehicle.getStartLocation().getId(), jobs));
        }

        onFinished(this.channel, processingDepots, unassignedBses.keySet());
    }

    private VehicleRoutingProblemSolution findBest(Collection<VehicleRoutingProblemSolution> solutions) {
        VehicleRoutingProblemSolution bestSolution = null;
        double leastDuration = Double.MAX_VALUE;

        for (VehicleRoutingProblemSolution s : solutions) {
            double duration = 0.0;

            for(VehicleRoute r : s.getRoutes()) {
                Location depot = r.getVehicle().getStartLocation();
                if(r.getActivities().size() > 0) {
                    TourActivity firstAct = r.getActivities().get(0);

                    long a = Long.parseLong(depot.getId());
                    long b = Long.parseLong(firstAct.getLocation().getId());
                    if(DURATION_MATRIX.keySet().contains(a)) {
                        if(DURATION_MATRIX.get(a).keySet().contains(b)) {
                            duration += DURATION_MATRIX.get(a).get(b);
                        }
                    }
                }
            }

            if(duration < leastDuration) {
                bestSolution = s;
                leastDuration = duration;
            }
        }

        LOG.debug(String.format("Best solution found, with total 'first-job' transport time is %s", leastDuration));

        return bestSolution;
    }
}
