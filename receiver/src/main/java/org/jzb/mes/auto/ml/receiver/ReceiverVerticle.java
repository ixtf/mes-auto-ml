package org.jzb.mes.auto.ml.receiver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Delivery;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.ExceptionHandlers;
import reactor.rabbitmq.Receiver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.jzb.mes.auto.ml.receiver.Receiver.*;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class ReceiverVerticle extends AbstractVerticle {
    private static final ConsumeOptions consumeOptions = new ConsumeOptions()
            .exceptionHandler(new ExceptionHandlers.RetryAcknowledgmentExceptionHandler(
                    Duration.ofMinutes(10), Duration.ofSeconds(5),
                    ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
            ));
    @Inject
    private Receiver receiver;
    @Inject
    @Named("dir")
    private String DIR;

    @Override
    public void start() {
        INJECTOR.injectMembers(this);
        receiver.consumeAutoAck(QUEUE_POY, consumeOptions).subscribe(it -> saveFile("POY", it), err -> log.error("", err));
        receiver.consumeAutoAck(QUEUE_FDY, consumeOptions).subscribe(it -> saveFile("FDY", it), err -> log.error("", err));
    }

    private void saveFile(String product, Delivery delivery) {
        Mono.fromRunnable(() -> {
            final Path dirPath = generateDirPath(product);
            final Path originalFilePath = originalFilePath(delivery);
            final Path savePath = dirPath.resolve(originalFilePath);
            final byte[] bytes = delivery.getBody();
            vertx.fileSystem().writeFile(savePath.toString(), Buffer.buffer(bytes), ar -> {
                if (ar.failed()) {
                    final String msg = String.format("saveFile(%s)", savePath);
                    log.error(msg, ar.cause());
                }
            });
        }).doOnError(err -> log.error("", err)).subscribe();
    }

    private Path originalFilePath(Delivery delivery) {
        final BasicProperties basicProperties = delivery.getProperties();
        final Map<String, Object> headers = basicProperties.getHeaders();
        final String filePath = (String) headers.get("filePath");
        return Paths.get(filePath);
    }

    @SneakyThrows
    private Path generateDirPath(String product) {
        final LocalDate ld = LocalDate.now();
        final int year = ld.getYear();
        final int month = ld.getMonthValue();
        final int day = ld.getDayOfMonth();
        final Path dirPath = Paths.get(DIR, product, "" + year, "" + month, "" + day);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        return dirPath;
    }

}
