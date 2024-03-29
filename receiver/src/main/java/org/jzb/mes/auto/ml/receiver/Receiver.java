package org.jzb.mes.auto.ml.receiver;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Receiver {
    public static final String QUEUE_FDY = "mes-auto-ml-queue-FDY";
    public static final String QUEUE_POY = "mes-auto-ml-queue-POY";
    public static final Injector INJECTOR = Guice.createInjector(new ReceiverModule());

    public static void start(String[] args) {
        final VertxOptions vertxOptions = new VertxOptions()
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        final Vertx vertx = Vertx.vertx(vertxOptions);
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setWorker(true)
                .setMaxWorkerExecuteTime(1)
                .setMaxWorkerExecuteTimeUnit(TimeUnit.DAYS);
        vertx.deployVerticle(ReceiverVerticle.class.getName(), deploymentOptions, ar -> {
            if (ar.failed()) {
                log.error("", ar.cause());
            } else {
                log.info("{} success", Receiver.class);
            }
        });
    }

    public static void stop(String[] args) {
        System.exit(0);
    }

    public static void main(String[] args) {
        Receiver.start(args);
    }
}
