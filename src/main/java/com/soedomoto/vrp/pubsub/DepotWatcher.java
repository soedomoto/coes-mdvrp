package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.solver.JSpritVRPSolver;
import org.apache.log4j.Logger;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by soedomoto on 27/01/17.
 */
public class DepotWatcher extends ChannelWatcher {
    private final static Logger LOG = Logger.getLogger(DepotWatcher.class.getName());

    private final App app;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(15);
    private final List<String> beingProcessedChannels = new ArrayList();
    private final List<String> assignedLocations = new ArrayList();
    private final Map<String, Map<String, Object>> cachedChannelResults = new HashMap();

    public DepotWatcher(App app, String brokerUrl) throws URISyntaxException {
        super(brokerUrl);
        this.app = app;

        try {
            List<CensusBlock> assignedBses = app.getCensusBlockDao().queryBuilder()
                    .where().isNotNull("assigned_to").query();
            for(CensusBlock bs: assignedBses) assignedLocations.add(String.valueOf(bs.getId()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onChannelAdded(String depotChannel) {
        String channel = depotChannel.replace("depot.", "");
        LOG.debug(String.format("%s channel added", channel));

        if(cachedChannelResults.keySet().contains(channel)) {
            Map<String, Object> result = cachedChannelResults.get(channel);
            Long receivers = publish(channel, result);
            if(receivers > 0) {
                LOG.debug(String.format("Cached result is published to channel %s. Number receivers = %s", channel, receivers));
                return;
            }
        }

        if(! beingProcessedChannels.contains(channel)) {
            try {
                executor.submit(new JSpritVRPSolver(app, channel) {
                    public void onStarted(String channel, List<Long> depots, Set<Long> locations) {
                        LOG.debug(String.format("Start solving channel %s", channel));
                        for(Long d: depots)
                            beingProcessedChannels.add(String.valueOf(d));
                    }

                    public void onSolution(String channel, Map<String, Object> routeVehicle, Map<String, Object> activity, double duration, double serviceTime) {
                        Map<String, Object> solutionMap = new HashMap();
                        solutionMap.putAll(routeVehicle);
                        solutionMap.putAll(activity);
                        solutionMap.put("duration", duration);
                        solutionMap.put("service-time", serviceTime);

                        Long receivers = publish(channel, solutionMap);
                        LOG.debug(String.format("Result is published to channel %s. Number receivers = %s", channel, receivers));

                        cachedChannelResults.put(channel, solutionMap);
                    }

                    public void onFinished(String channel, List<Long> depots, Set<Long> locations) {
                        LOG.debug(String.format("Finish solving channel %s", channel));
                        for(Long d: depots)
                            beingProcessedChannels.remove(String.valueOf(d));
                    }
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void onChannelRemoved(String depotChannel) {
        String channel = depotChannel.replace("depot.", "");
        LOG.debug(String.format("%s channel removed", channel));
    }

    private Long publish(String channel, Map<String, Object> solutionMap) {
        if(! assignedLocations.contains(solutionMap.get("location"))) {
            String result = new Gson().toJson(solutionMap);
            long receivers = jedis.publish(String.format("depot.%s", channel), result);

            if (receivers > 0) {
                try {
                    CensusBlock bs = app.getCensusBlockDao().queryForId(Long.valueOf(String.valueOf(solutionMap.get("location"))));
                    bs.setAssignedTo((long) 1000);
                    bs.setAssignDate(new Date());
                    app.getCensusBlockDao().update(bs);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                cachedChannelResults.remove(channel);
                assignedLocations.add(channel);
            }

            return receivers;
        } else {
            return (long) 0;
        }
    }
}
