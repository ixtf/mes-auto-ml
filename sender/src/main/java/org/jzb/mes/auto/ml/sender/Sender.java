package org.jzb.mes.auto.ml.sender;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Sender {
    public static final String EXCHANGE = "mes-auto-ml-exchange";
    public static final Injector INJECTOR = Guice.createInjector(new SenderModule());

    @SneakyThrows
    public static void main(String[] args) {
        final VertxOptions vertxOptions = new VertxOptions()
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        final Vertx vertx = Vertx.vertx(vertxOptions);
        deploySendFile(vertx).compose(it -> deployWatcher(vertx)).setHandler(ar -> {
            if (ar.failed()) {
                log.error("", ar.cause());
            } else {
                log.info("{} success", Sender.class);
            }
        });
    }

    private static Future<String> deploySendFile(Vertx vertx) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setInstances(10)
                .setWorker(true)
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        vertx.deployVerticle(SendFileVerticle.class.getName(), deploymentOptions, promise);
        return promise.future();
    }

    private static Future<String> deployWatcher(Vertx vertx) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setWorker(true)
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        vertx.deployVerticle(WatcherVerticle.class.getName(), deploymentOptions, promise);
        return promise.future();
    }
}
