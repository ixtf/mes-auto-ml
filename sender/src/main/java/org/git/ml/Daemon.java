package org.git.ml;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Daemon {
    public static final String EXCHANGE = "mes-auto-ml-exchange";
    public static final Injector INJECTOR = Guice.createInjector(new Module());

    public static void start() {
        final VertxOptions vertxOptions = new VertxOptions()
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        final Vertx vertx = Vertx.vertx(vertxOptions);
        deploySendFile(vertx).compose(it -> deployWatcher(vertx)).setHandler(ar -> {
            if (ar.failed()) {
                log.error("", ar.cause());
            } else {
                log.info("{} success", Daemon.class);
            }
        });
    }

    public static void stop() {
        System.exit(0);
    }

    private static Future<String> deploySendFile(Vertx vertx) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setInstances(10)
                .setWorker(true)
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        vertx.deployVerticle(Verticle1.class.getName(), deploymentOptions, promise);
        return promise.future();
    }

    private static Future<String> deployWatcher(Vertx vertx) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setWorker(true)
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        vertx.deployVerticle(Verticle2.class.getName(), deploymentOptions, promise);
        return promise.future();
    }

    @SneakyThrows
    public static void main(String[] args) {
        Daemon.start();

        final FileOutputStream fos = new FileOutputStream("/tmp/oos/test.data");
        final ObjectOutputStream oos = new ObjectOutputStream(fos);
        final ObjectNode objectNode = Json.mapper.createObjectNode()
                .put("test", "test");
        oos.write(objectNode.toString().getBytes(UTF_8));
    }
}
