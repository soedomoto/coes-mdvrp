package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.solver.AbstractVRPSolver;
import com.soedomoto.vrp.solver.CoESVRPSolver;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by soedomoto on 27/01/17.
 */
public class VRPWorker implements Runnable {
    private final static Logger LOG = Logger.getLogger(VRPWorker.class.getName());

    private final App app;
    private final String brokerUrl;
    private final ScheduledExecutorService executor;

    private final String depotPrefix = "depot.";
    private Map<String, Future<?>> channelSolvers = new LinkedHashMap();
    private Map<String, Map<String, Object>> finishedChannelSolutions = new LinkedHashMap();

    public VRPWorker(App app, String brokerUrl) {
        this.app = app;
        this.brokerUrl = brokerUrl;
        this.executor = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        Executors.newFixedThreadPool(1).execute(this);
    }

    public void run() {
        try {
            final Jedis channelClient = new Jedis(new URI(brokerUrl));
            final Jedis channelPublisher = new Jedis(new URI(brokerUrl));

            Executors.newFixedThreadPool(1).execute(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            List<String> depotChannels = channelClient.pubsubChannels(String.format("%s*", depotPrefix));

                            boolean hasNew = false;
                            for(String depotChannel : depotChannels) {
                                String channel = depotChannel.replace(depotPrefix, "");
                                if(! channelSolvers.containsKey(channel) && ! finishedChannelSolutions.containsKey(channel)) {
                                    channelSolvers.put(channel, executor.submit(createSolver(channel, channelPublisher)));
                                    hasNew = true;
                                }
                            }

                            if(depotChannels.size() == 0 || !hasNew) Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private AbstractVRPSolver createSolver(final String currentChannel, final Jedis channelPublisher) throws SQLException {
        return new CoESVRPSolver(app, currentChannel) {
            public void onStarted(String channel, List<Long> depots, Set<Long> locations) {
                LOG.debug(String.format("Start solving channel %s", channel));
            }

            public void onSolution(String channel, Map<String, Object> routeVehicle, Map<String, Object> activity, double duration, double serviceTime) {
                Map<String, Object> solutionMap = new HashMap();
                solutionMap.putAll(routeVehicle);
                solutionMap.putAll(activity);
                solutionMap.put("duration", duration);
                solutionMap.put("service-time", serviceTime);

                Long receivers = channelPublisher.publish(String.format("%s%s", depotPrefix, channel), new Gson().toJson(solutionMap));
                LOG.debug(String.format("%s = %s receivers", channel, receivers));

                synchronized(channelSolvers) {
                    if (channelSolvers.containsKey(channel) && channel != currentChannel && receivers > 0) {
                        if(channelSolvers.get(channel).cancel(true)) {
                            LOG.debug(String.format("Solver %s is canceled", channel));
                        }
                    }
                }

                synchronized(finishedChannelSolutions) {
                    if(receivers > 0) {
                        finishedChannelSolutions.put(channel, solutionMap);

                        try {
                            CensusBlock bs = app.getCensusBlockDao().queryForId(Long.valueOf(String.valueOf(solutionMap.get("location"))));
                            bs.setAssignedTo((long) 1000);
                            bs.setAssignDate(new Date());
                            app.getCensusBlockDao().update(bs);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void onFinished(String channel, List<Long> depots, Set<Long> locations) {
                LOG.debug(String.format("Finish solving channel %s", channel));
            }
        };
    }
}
