package org.jzb.mes.auto.ml.sender;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ExceptionHandlers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.SendOptions;
import reactor.rabbitmq.Sender;

import java.time.Duration;

import static org.jzb.mes.auto.ml.sender.Sender.EXCHANGE;
import static org.jzb.mes.auto.ml.sender.Sender.INJECTOR;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class SendFileVerticle extends AbstractVerticle {
    public static final String ADDRESS = "mes-auto-ml:sendFile";
    @Inject
    private Sender sender;
    @Inject
    @Named("product")
    private String product;
    private static final SendOptions sendOptions = new SendOptions().exceptionHandler(
            new ExceptionHandlers.RetrySendingExceptionHandler(
                    Duration.ofHours(1), Duration.ofMinutes(5),
                    ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
            )
    );

    @Override
    public void start(Future<Void> startFuture) {
        INJECTOR.injectMembers(this);
        vertx.eventBus().<String>consumer(ADDRESS, reply -> {
            final String filePath = reply.body();
            readFile(filePath).setHandler(ar -> {
                if (ar.failed()) {
                    final String msg = String.format("vertx.fileSystem().readFile(%s)", filePath);
                    log.error(msg, ar.cause());
                    return;
                }
                final OutboundMessage outboundMessage = new OutboundMessage(EXCHANGE, product, ar.result());
                final Mono<OutboundMessage> outboundMessageMono = Mono.just(outboundMessage);
                sender.sendWithPublishConfirms(outboundMessageMono, sendOptions).subscribe(ret -> {
                    if (!ret.isAck()) {
                        log.error(filePath + "[ack=false]");
                    }
                }, err -> {
                    final String msg = String.format("sendWithPublishConfirms(%s)", filePath);
                    log.error(msg, err);
                });
            });
        }).completionHandler(startFuture::handle);
    }

    private Future<byte[]> readFile(String filePath) {
        final Promise<Buffer> promise = Promise.promise();
        vertx.fileSystem().readFile(filePath, promise);
        return promise.future().map(Buffer::getBytes);
    }
}
