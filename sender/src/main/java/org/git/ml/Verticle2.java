package org.git.ml;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vertx.core.AbstractVerticle;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.Objects;
import java.util.Optional;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

/**
 * @author jzb 2019-08-08
 */
@Slf4j
public class Verticle2 extends AbstractVerticle {
    @Inject
    @Named("watchDir")
    private String watchDir;

    @Override
    public void start() {
        Daemon.INJECTOR.injectMembers(this);
        PathFlux.watchRecursive(Paths.get(watchDir))
                .subscribeOn(Schedulers.newSingle("PathFlux", true))
                .filter(it -> ENTRY_CREATE == it.kind())
                .map(WatchEvent::context)
                .filter(Path.class::isInstance)
                .map(Path.class::cast)
                .subscribe(this::handle);
    }

    @SneakyThrows()
    private void handle(Path path) {
        try {
            Optional.ofNullable(path).stream()
                    // 多级目录只返回相对路径的文件名
                    .map(Path::toString)
                    .map(it -> findRealPath(Paths.get(watchDir), it))
                    .filter(Objects::nonNull)
                    .filter(it -> !Files.isDirectory(path))
                    .map(Path::toString)
                    .forEach(it -> vertx.eventBus().send(Verticle1.ADDRESS, it));
        } catch (Throwable e) {
            log.error("", e);
        }
    }

    @SneakyThrows
    private Path findRealPath(Path dir, String fileName) {
        final Path result = dir.resolve(fileName);
        if (Files.exists(result)) {
            return result;
        }
        return Files.list(dir)
                .filter(Files::isDirectory)
                .map(subDir -> findRealPath(subDir, fileName))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
