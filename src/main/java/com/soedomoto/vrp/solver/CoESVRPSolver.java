package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.dao.Enumerator;
import com.soedomoto.vrp.model.solution.Point;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 12/02/17.
 */
public abstract class CoESVRPSolver extends AbstractCoESVRPSolver implements Runnable, VRPSolver {
    private final static Logger LOG = Logger.getLogger(CoESVRPSolver.class.getName());

    public CoESVRPSolver(App app, String channel, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, brokerUrl);
    }

    public CoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl);
    }

    public CoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl, useAllEnumerator);
    }

    public CoESVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, String baseDir, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        super(app, channel, otherChannels, brokerUrl, baseDir, useAllEnumerator);
    }

    public void run() {
        super.run();

        onStarted(this.channel, Arrays.asList(enumeratorIds), unassignedBses.keySet());

        // Vehicles
        Map<Long, Enumerator> enumerators;
        if(useAllEnumerator) enumerators = allEnumerators;
        else enumerators = subscribingEnumerators;

        File program = new File(app.getCoesBin());
        File problem = new File(baseDir + File.separator + "problem" + File.separator + System.currentTimeMillis());
        File known = new File(baseDir + File.separator + "know-solution" + File.separator + System.currentTimeMillis());
        File solution = new File(baseDir + File.separator + "solution" + File.separator + System.currentTimeMillis());
        solution.getParentFile().mkdirs();

        try {
            this.createInputFile(problem, known);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(program.getAbsolutePath(), "--depot-subpop-ind", "3",
                    "--max-exec-time", "60", "--max-time-no-update", "40", problem.getAbsolutePath(),
                    known.getAbsolutePath(), solution.getAbsolutePath());
            pb.redirectErrorStream();
            Process process = pb.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) LOG.debug(line);

            int retval = process.waitFor();
            if(retval == 0) {
                List<String> lines = FileUtils.readLines(solution, Charset.defaultCharset());
                String[] enumeratorIdxs = lines.get(0).split("\t");
                String[] routeIdxs = lines.get(1).split("\t");
                String[] routeCosts = lines.get(2).split("\t");
                String[] routeDemands = lines.get(3).split("\t");
                String[] strDestinationBsIdxs = lines.get(4).split("\t");
                List<String[]> destinationBsIdxs = new ArrayList();
                for(int d=0; d<strDestinationBsIdxs.length; d++) {
                    String s = strDestinationBsIdxs[d];
                    if(s != null && s.length() != 0) {
                        destinationBsIdxs.add(d, s.split(","));
                    } else {
                        destinationBsIdxs.add(d, new String[] {});
                    }
                }

                try {
                    Map<Long, List<Long>> enumeratorBsIdRoutes = new HashMap();
                    Map<Long, Integer> enumeratorDemands = new HashMap();
                    Map<Long, Float> enumeratorCosts = new HashMap();
                    for (int i = 0; i < enumeratorIdxs.length; i++) {
                        long eId = enumeratorIds[Integer.parseInt(enumeratorIdxs[i])];

                        if(! enumeratorBsIdRoutes.keySet().contains(eId)) enumeratorBsIdRoutes.put(eId, new ArrayList());
                        if(! enumeratorDemands.keySet().contains(eId)) enumeratorDemands.put(eId, 0);
                        if(! enumeratorCosts.keySet().contains(eId)) enumeratorCosts.put(eId, 0.0f);

                        for (String sbsIdx : destinationBsIdxs.get(i)) {
                            int bsIdx = Integer.valueOf(sbsIdx);
                            try {
                                if (bsIdx > 0) enumeratorBsIdRoutes.get(eId).add(bsIds[bsIdx-1]);
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }

                        enumeratorDemands.put(eId, Integer.valueOf(enumeratorDemands.get(eId)) + Integer.valueOf(routeDemands[i]));
                        enumeratorCosts.put(eId, Float.valueOf(enumeratorCosts.get(eId)) + Float.valueOf(routeCosts[i]));
                    }
//
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
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        onFinished(this.channel, Arrays.asList(enumeratorIds), unassignedBses.keySet());
    }
    
    private void createInputFile(File inputFile, File solution) throws IOException {
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

        FileUtils.write(inputFile, strProblem, Charset.defaultCharset());
        FileUtils.write(solution, "1\n", Charset.defaultCharset());
    }

}
