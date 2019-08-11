package org.git.ml;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.SecureRandom;
import java.util.Map;

import static org.git.ml.Daemon.CONFIG;
import static reactor.rabbitmq.Utils.singleConnectionMono;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Module extends AbstractModule {
    @SneakyThrows
    public static Map readConfig(String configPath) {
        log.info(configPath);
        if (configPath == null) {
            return Maps.newHashMap();
        }
        final File file = new File(configPath);
        if (!file.exists()) {
            return Maps.newHashMap();
        }
        if (!file.getName().endsWith(".json")) {
            @Cleanup final FileInputStream fis = new FileInputStream(file);
            @Cleanup final ObjectInputStream ois = new ObjectInputStream(fis);
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("des");
            final Cipher cipher = Cipher.getInstance("des");
            final DESKeySpec keySpec = new DESKeySpec((byte[]) ois.readObject());
            final SecretKey secretKey = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new SecureRandom());
            final byte[] bytes = cipher.doFinal((byte[]) ois.readObject());
            return Json.mapper.readValue(bytes, Map.class);
        } else {
            return Json.mapper.readValue(file, Map.class);
        }
    }

    @Provides
    @Singleton
    @Named("config")
    private JsonObject config() {
        final Map map = readConfig(System.getProperty(CONFIG));
        return new JsonObject(map);
    }

    @Provides
    @Singleton
    private Sender Sender(@Named("config") JsonObject config) {
        final String host = config.getString("host");
        final String username = config.getString("username");
        final String password = config.getString("password");
        final String clientProvidedName = config.getString("clientProvidedName");

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
                .resourceManagementScheduler(Schedulers.elastic())
                .channelPool(ChannelPoolFactory.createChannelPool(connectionMono, channelPoolOptions));
        return RabbitFlux.createSender(senderOptions);
    }

    @Provides
    @Singleton
    @Named("product")
    private String product(@Named("config") JsonObject config) {
        return config.getString("product");
    }

    @Provides
    @Singleton
    @Named("watchDir")
    private String watchDir(@Named("config") JsonObject config) {
        return config.getString("watchDir");
    }

}
