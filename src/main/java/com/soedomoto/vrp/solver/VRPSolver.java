package com.soedomoto.vrp.solver;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by soedomoto on 22/01/17.
 */
public interface VRPSolver {
    public void onStarted(String channel, List<Long> depots, Set<Long> locations);
    public void onSolution(String channel, Map<String, Object> routeVehicle, Map<String, Object> activity, double duration, double serviceTime);
    public void onFinished(String channel, List<Long> depots, Set<Long> locations);
}
