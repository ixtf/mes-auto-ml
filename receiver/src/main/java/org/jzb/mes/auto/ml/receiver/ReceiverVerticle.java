package org.jzb.mes.auto.ml.receiver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.Delivery;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.ExceptionHandlers;
import reactor.rabbitmq.Receiver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.jzb.mes.auto.ml.receiver.Receiver.*;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class ReceiverVerticle extends AbstractVerticle {
    @Inject
    private Receiver receiver;
    @Inject
    @Named("dir")
    private String DIR;
    private static final ConsumeOptions consumeOptions = new ConsumeOptions()
            .exceptionHandler(new ExceptionHandlers.RetryAcknowledgmentExceptionHandler(
                    Duration.ofMinutes(10), Duration.ofSeconds(5),
                    ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
            ));

    @Override
    public void start() {
        INJECTOR.injectMembers(this);
        receiver.consumeAutoAck(QUEUE_POY, consumeOptions).subscribe(this::savePOY, err -> log.error("", err));
        receiver.consumeAutoAck(QUEUE_FDY, consumeOptions).subscribe(this::saveFDY, err -> log.error("", err));
    }

    private void savePOY(Delivery delivery) {
        final byte[] bytes = delivery.getBody();
        saveFile("POY", bytes);
    }

    private void saveFDY(Delivery delivery) {
        final byte[] bytes = delivery.getBody();
        saveFile("FDY", bytes);
    }

    @SneakyThrows
    private void saveFile(String product, byte[] bytes) {
        final LocalDate ld = LocalDate.now();
        final int year = ld.getYear();
        final int month = ld.getMonthValue();
        final int day = ld.getDayOfMonth();
        final Path dirPath = Paths.get(DIR, product, "" + year, "" + month, "" + day);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        final String uuid = UUID.randomUUID().toString();
        final Path path = dirPath.resolve(uuid);
        vertx.fileSystem().writeFile(path.toString(), Buffer.buffer(bytes), ar -> {
            if (ar.failed()) {
                final String msg = String.format("saveFile(%s)", path);
                log.error(msg, ar.cause());
            }
        });
    }

}
