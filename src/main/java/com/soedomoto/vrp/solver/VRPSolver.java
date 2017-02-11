package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.model.solution.Point;

import java.util.List;
import java.util.Set;

/**
 * Created by soedomoto on 22/01/17.
 */
public interface VRPSolver {
    public void onStarted(String channel, List<Long> depots, Set<Long> locations);
    public void onSolution(String channel, Point depot, Point destination, double duration, double serviceTime);
    public void onFinished(String channel, List<Long> depots, Set<Long> locations);
}
