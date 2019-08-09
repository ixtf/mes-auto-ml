package org.git.ml;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public final class PathFlux {

    private PathFlux() {
    }

    public static Flux<WatchEvent<?>> watchRecursive(final Path path) {
        final boolean recursive = true;
        return new FluxFactory(path, recursive).create();
    }

    public static Flux<WatchEvent<?>> watchNonRecursive(final Path path) {
        final boolean recursive = false;
        return new FluxFactory(path, recursive).create();
    }

    private static class FluxFactory {
        private final Map<WatchKey, Path> directoriesByKey = new HashMap();
        private final Path directory;
        private final boolean recursive;

        private FluxFactory(final Path path, final boolean recursive) {
            directory = path;
            this.recursive = recursive;
        }

        private Flux<WatchEvent<?>> create() {
            return Flux.create(fluxSink -> {
                boolean errorFree = true;
                try (WatchService watcher = directory.getFileSystem().newWatchService()) {
                    try {
                        if (recursive) {
                            registerAll(directory, watcher);
                        } else {
                            register(directory, watcher);
                        }
                    } catch (IOException exception) {
                        fluxSink.error(exception);
                        errorFree = false;
                    }
                    while (errorFree && !fluxSink.isCancelled()) {
                        final WatchKey key;
                        try {
                            key = watcher.take();
                        } catch (InterruptedException exception) {
                            if (!fluxSink.isCancelled()) {
                                fluxSink.error(exception);
                            }
                            errorFree = false;
                            break;
                        }
                        final Path dir = directoriesByKey.get(key);
                        for (final WatchEvent<?> event : key.pollEvents()) {
                            fluxSink.next(event);
                            registerNewDirectory(fluxSink, dir, watcher, event);
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            directoriesByKey.remove(key);
                            if (directoriesByKey.isEmpty()) {
                                break;
                            }
                        }
                    }
                } catch (Throwable e) {
                    fluxSink.error(e);
                }

                if (errorFree) {
                    fluxSink.complete();
                }
            });
        }

        private void registerAll(final Path rootDirectory, final WatchService watcher) throws IOException {
            Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    register(dir, watcher);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        private void register(final Path dir, final WatchService watcher) throws IOException {
            final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            directoriesByKey.put(key, dir);
        }

        private void registerNewDirectory(final FluxSink<WatchEvent<?>> fluxSink, final Path dir, final WatchService watcher, final WatchEvent<?> event) {
            final Kind<?> kind = event.kind();
            if (recursive && kind.equals(ENTRY_CREATE)) {
                final Path name = (Path) event.context();
                final Path child = dir.resolve(name);
                try {
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        registerAll(child, watcher);
                    }
                } catch (final IOException exception) {
                    fluxSink.error(exception);
                }
            }
        }
    }
}