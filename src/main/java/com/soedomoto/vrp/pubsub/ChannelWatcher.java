package com.soedomoto.vrp.pubsub;

import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.*;

/**
 * Created by soedomoto on 19/01/17.
 */
public abstract class ChannelWatcher {
    protected SortedSet<String> availableChannels = new TreeSet();
    protected final Jedis jedis;

    public ChannelWatcher(String brokerUrl) throws URISyntaxException {
        this.jedis = new Jedis(new URI(brokerUrl));;
    }

    public ChannelWatcher watch(final String pattern) {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        Executors.newCachedThreadPool().execute(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        ScheduledFuture<?> task = executor
                                .schedule(new WatchExecutor(pattern), 1, TimeUnit.SECONDS);
                        task.get();
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    }
                }
            }
        });

        return this;
    }

    private class WatchExecutor implements Runnable {
        private final String pattern;

        public WatchExecutor(String pattern) {
            this.pattern = pattern;
        }

        public void run() {
            List<String> channels = jedis.pubsubChannels(pattern);

            for(String channel : availableChannels) {
                if(! channels.contains(channel)) {
                    availableChannels.remove(channel);
                    onChannelRemoved(channel);
                }
            }

            for(String channel : channels) {
                if(availableChannels.add(channel)) {
                    onChannelAdded(channel);
                }
            }
        }
    }

    public abstract void onChannelAdded(String channel);
    public abstract void onChannelRemoved(String channel);
}
