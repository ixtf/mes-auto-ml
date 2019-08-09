package org.git.ml;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP.BasicProperties;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ExceptionHandlers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.SendOptions;
import reactor.rabbitmq.Sender;

import java.time.Duration;
import java.util.Map;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Verticle1 extends AbstractVerticle {
    public static final String ADDRESS = Verticle1.class.getName();
    private static final SendOptions sendOptions = new SendOptions().exceptionHandler(
            new ExceptionHandlers.RetrySendingExceptionHandler(
                    Duration.ofHours(1), Duration.ofMinutes(5),
                    ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
            )
    );
    @Inject
    private Sender sender;
    @Inject
    @Named("product")
    private String product;

    @Override
    public void start(Future<Void> startFuture) {
        Daemon.INJECTOR.injectMembers(this);
        vertx.eventBus().<String>consumer(ADDRESS, reply -> {
            final String filePath = reply.body();
            vertx.fileSystem().readFile(filePath, ar -> {
                if (ar.failed()) {
                    final String msg = String.format("vertx.fileSystem().readFile(%s)", filePath);
                    log.error(msg, ar.cause());
                } else {
                    sendFile(filePath, ar.result().getBytes());
                }
            });
        }).completionHandler(startFuture::handle);
    }

    private void sendFile(String filePath, byte[] bytes) {
        final Map<String, Object> headers = Maps.newHashMap();
        headers.put("filePath", filePath);
        final BasicProperties basicProperties = new BasicProperties.Builder().headers(headers).build();
        final OutboundMessage outboundMessage = new OutboundMessage(Daemon.EXCHANGE, product, basicProperties, bytes);
        final Mono<OutboundMessage> outboundMessageMono = Mono.just(outboundMessage);
        sender.sendWithPublishConfirms(outboundMessageMono, sendOptions).subscribe(ret -> {
            if (!ret.isAck()) {
                log.error(filePath + "[ack=false]");
            }
        }, err -> {
            final String msg = String.format("sendWithPublishConfirms(%s)", filePath);
            log.error(msg, err);
        });
    }

}
