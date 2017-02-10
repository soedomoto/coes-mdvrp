package com.soedomoto.vrp.solver;

/**
 * Created by soedomoto on 03/02/17.
 */
public class CoESVRPJNI {
    static {
        System.loadLibrary("jcoes_mdvrp");
    }

    public native void setVehicles(int size, float[] xDepots, float[] yDepots, float[] durations, int[] capacity);
    public native void setCustomers(int size, float[] xPoints, float[] yPoints, int[] demand);
    public native void setCustomerToCustomerDistance(int xSize, int ySize, float[][] distances);
    public native void setDepotToCustomerDistance(int xSize, int ySize, float[][] distances);
    public native void setDepotToDepotDistance(int xSize, int ySize, float[][] distances);
    public native void configSetNumSubpopulation(int num);
    public native void configStopWhenMaxExecutionTime(float seconds);
    public native void configStopWhenMaxTimeWithoutUpdate(float seconds);
    public native void configStopWhenNumGeneration(int num);
    public native int solve();
    public native int[] getSolutionDepots();
    public native int[] getSolutionRoutes();
    public native float[] getSolutionCosts();
    public native int[] getSolutionDemands();
    public native int[][] getSolutionCustomers();
}
