package com.soedomoto.vrp.pubsub;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.model.Enumerator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.Executors;

/**
 * Created by soedomoto on 26/01/17.
 */
public class ChannelSubscriber {
    private final String brokerUrl;
    private final App app;

    public ChannelSubscriber(App app, String brokerUrl) throws URISyntaxException {
        this.app = app;
        this.brokerUrl = brokerUrl;
    }

    public void visit() {
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            public void run() {
                try {
                    Jedis jedis = new Jedis(new URI(brokerUrl));
                    jedis.psubscribe(new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            try {
                                String enumeratorId = channel.replace("enumerator.", "").replace(".visit", "");
                                String[] msgs = message.split("\\s+");
                                String customerId = msgs[0];
                                String timestamp = msgs[1];

                                Date date = new Date(Long.valueOf(timestamp));

                                CensusBlock bs = app.getCensusBlockDao().queryForId(Long.valueOf(customerId));
                                bs.setVisitedBy(Long.valueOf(enumeratorId));
                                bs.setVisitDate(date);
                                int status = app.getCensusBlockDao().update(bs);

                                Enumerator enumerator = app.getEnumeratorDao().queryForId(Long.valueOf(enumeratorId));
                                enumerator.setDepot(Long.valueOf(customerId));
                                app.getEnumeratorDao().update(enumerator);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }, "enumerator.*.visit");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void clientLogger(final String clientLogDir) {
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            public void run() {
                try {
                    Jedis jedis = new Jedis(new URI(brokerUrl));
                    jedis.psubscribe(new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            String enumeratorId = channel.replace("log.", "");

                            try {
                                File clientLogFile = new File(clientLogDir + File.separator + enumeratorId + ".log");
                                clientLogFile.getParentFile().mkdirs();
                                PrintWriter out = new PrintWriter(new FileWriter(clientLogFile, true));
                                out.println(message);
                                out.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, "log.*");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
