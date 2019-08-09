package org.jzb.mes.auto.ml.receiver;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;

import java.io.File;

/**
 * @author jzb 2019-08-08
 */
public class ReceiverModule extends AbstractModule {
    @SneakyThrows
    @Provides
    @Singleton
    @Named("config")
    private JsonObject config() {
        final String configPath = System.getProperty("MES-AUTO-ML-RECEIVER");
        if (configPath == null) {
            return new JsonObject();
        }
        final File file = new File(configPath);
        if (file.exists()) {
            final JsonNode jsonNode = Json.mapper.readTree(file);
            return new JsonObject(jsonNode.toString());
        }
        return new JsonObject();
    }

    @Provides
    private Receiver Receiver(@Named("config") JsonObject rootConfig) {
        final JsonObject config = rootConfig.getJsonObject("sender", new JsonObject());
        final String host = config.getString("host", "192.168.0.38");
        final String username = config.getString("username", "admin");
        final String password = config.getString("password", "tomking");
        final String clientProvidedName = config.getString("clientProvidedName", "mes-auto-ml-receiver");

        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.useNio();
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        final ReceiverOptions receiverOptions = new ReceiverOptions()
                .connectionFactory(connectionFactory)
                .connectionSupplier(cf -> {
                    final Address address = new Address(host);
                    return cf.newConnection(new Address[]{address}, clientProvidedName);
                });
        return RabbitFlux.createReceiver(receiverOptions);
    }

    @Provides
    @Singleton
    @Named("dir")
    private String dir(@Named("config") JsonObject rootConfig) {
        return rootConfig.getString("dir", "/tmp");
    }
}
