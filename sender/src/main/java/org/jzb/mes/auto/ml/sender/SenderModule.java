package org.jzb.mes.auto.ml.sender;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.*;

import java.io.File;
import java.util.Map;

import static reactor.rabbitmq.Utils.singleConnectionMono;

/**
 * @author jzb 2019-08-08
 */
public class SenderModule extends AbstractModule {
    @SneakyThrows
    @Provides
    @Singleton
    @Named("config")
    private JsonObject config() {
        final String configPath = System.getProperty("MES-AUTO-ML-SENDER");
        if (configPath == null) {
            return new JsonObject();
        }
        final File file = new File(configPath);
        if (file.exists()) {
            final Map map = Json.mapper.readValue(file, Map.class);
            return new JsonObject(map);
        }
        return new JsonObject();
    }

    @Provides
    @Singleton
    private Sender Sender(@Named("config") JsonObject config) {
        final String host = config.getString("host", "192.168.0.38");
        final String username = config.getString("username", "admin");
        final String password = config.getString("password", "tomking");
        final String clientProvidedName = config.getString("clientProvidedName", "mes-auto-ml-sender");

        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        final Mono<? extends Connection> connectionMono = singleConnectionMono(() -> {
            final Address address = new Address(host);
            final Address[] addrs = {address};
            return connectionFactory.newConnection(addrs, clientProvidedName);
        });
        final ChannelPoolOptions channelPoolOptions = new ChannelPoolOptions().maxCacheSize(10);
        final SenderOptions senderOptions = new SenderOptions()
                .connectionFactory(connectionFactory)
                .connectionMono(connectionMono)
                .channelPool(ChannelPoolFactory.createChannelPool(connectionMono, channelPoolOptions))
                .resourceManagementScheduler(Schedulers.elastic());
        return RabbitFlux.createSender(senderOptions);
    }

    @Provides
    @Singleton
    @Named("product")
    private String product(@Named("config") JsonObject config) {
        return config.getString("product", "FDY");
    }

    @Provides
    @Singleton
    @Named("watchDir")
    private String watchDir(@Named("config") JsonObject config) {
        return config.getString("watchDir", "/tmp/watchDirTest");
    }
}
