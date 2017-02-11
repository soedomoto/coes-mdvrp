package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.solution.Point;
import com.soedomoto.vrp.model.solution.Visit;
import com.soedomoto.vrp.solver.AbstractVRPSolver;
import com.soedomoto.vrp.solver.CoESVRPSolver;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final String depotPrefix = "depot.";
    private final Jedis cache;
    private Map<String, Future<?>> channelSolverThreads = new LinkedHashMap();
    private Map<String, Visit> finishedChannelSolutions = new LinkedHashMap();

    public VRPWorker(App app, String brokerUrl) throws URISyntaxException {
        this.app = app;
        this.brokerUrl = brokerUrl;
        this.cache = new Jedis(new URI(brokerUrl));
    }

    public void start() {
        Executors.newFixedThreadPool(1).execute(this);
    }

    public void run() {
        try {
            final Jedis channelReader = new Jedis(new URI(brokerUrl));
            final Jedis channelPublisher = new Jedis(new URI(brokerUrl));

            Executors.newFixedThreadPool(1).execute(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            List<String> depotChannels = channelReader.pubsubChannels(String.format("%s*", depotPrefix));

                            boolean hasNew = false;
                            for(String depotChannel : depotChannels) {
                                String channel = depotChannel.replace(depotPrefix, "");

                                Visit last = finishedChannelSolutions.get(channel);
                                if(last != null && last.from.id.longValue() == last.to.id.longValue()) {
                                    channelSolverThreads.remove(channel);
                                }

                                if(!channelSolverThreads.containsKey(channel)) {
                                    Future<?> thread = executor.submit(createSolver(channel, channelPublisher));
                                    channelSolverThreads.put(channel, thread);
                                    hasNew = true;
                                }
                            }

                            if(depotChannels.size() == 0 || !hasNew) Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (SQLException e) {
                            LOG.error(e.getMessage(), e);
                        } catch (URISyntaxException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            });
        } catch (URISyntaxException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private AbstractVRPSolver createSolver(final String currentCh, final Jedis chPublisher) throws SQLException, URISyntaxException {
        return new CoESVRPSolver(app, currentCh, brokerUrl) {
            public void onStarted(String channel, List<Long> depots, Set<Long> locations) {
                LOG.debug(String.format("Start solving channel %s", channel));
            }

            public void onSolution(String channel, Point depot, Point destination, double duration, double serviceTime) {
                Visit visit = new Visit();
                visit.from = depot;
                visit.to = destination;
                visit.duration = duration;
                visit.serviceTime = serviceTime;

                String strVisit = new Gson().toJson(visit);
                Long receivers = chPublisher.publish(String.format("%s%s", depotPrefix, channel), strVisit);
                LOG.debug(String.format("%s = %s receivers", channel, receivers));

                synchronized(channelSolverThreads) {
                    if (channelSolverThreads.containsKey(channel) && channel != currentCh && receivers > 0) {
                        if(channelSolverThreads.get(channel).cancel(true)) {
                            LOG.debug(String.format("Solver %s is canceled", channel));
                        }
                    }
                }

                synchronized(finishedChannelSolutions) {
                    if(receivers > 0) {
                        finishedChannelSolutions.put(channel, visit);
                        cache.set(String.format("location.%s.assign-date", visit.to.id), String.valueOf(System.currentTimeMillis()));

//                        try {
//                            CensusBlock bs = app.getCensusBlockDao().queryForId(visit.to.id);
//                            bs.setAssignedTo((long) 1000);
//                            bs.setAssignDate(new Date());
//                            app.getCensusBlockDao().update(bs);
//                        } catch (SQLException e) {
//                            e.printStackTrace();
//                        }
                    }
                }
            }

            public void onFinished(String channel, List<Long> depots, Set<Long> locations) {
                LOG.debug(String.format("Finish solving channel %s", channel));
            }
        };
    }
}
