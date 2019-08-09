package org.git.ml;


import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public final class PathObservables {

    private PathObservables() {
    }

    public static Observable<WatchEvent<?>> watchRecursive(final Path path) {
        final boolean recursive = true;
        return new ObservableFactory(path, recursive).create();
    }

    public static Observable<WatchEvent<?>> watchNonRecursive(final Path path) {
        final boolean recursive = false;
        return new ObservableFactory(path, recursive).create();
    }

    private static class ObservableFactory {


        private final Map<WatchKey, Path> directoriesByKey = new HashMap<>();
        private final Path directory;
        private final boolean recursive;

        private ObservableFactory(final Path path, final boolean recursive) {
            directory = path;
            this.recursive = recursive;
        }

        private Observable<WatchEvent<?>> create() {
            return Observable.create(subscriber -> {
                boolean errorFree = true;
                try (WatchService watcher = directory.getFileSystem().newWatchService()) {
                    try {
                        if (recursive) {
                            registerAll(directory, watcher);
                        } else {
                            register(directory, watcher);
                        }
                    } catch (IOException exception) {
                        subscriber.onError(exception);
                        errorFree = false;
                    }
                    while (errorFree && !subscriber.isDisposed()) {
                        final WatchKey key;
                        try {
                            key = watcher.take();
                        } catch (InterruptedException exception) {
                            if (!subscriber.isDisposed()) {
                                subscriber.onError(exception);
                            }
                            errorFree = false;
                            break;
                        }
                        final Path dir = directoriesByKey.get(key);
                        for (final WatchEvent<?> event : key.pollEvents()) {
                            subscriber.onNext(event);
                            registerNewDirectory(subscriber, dir, watcher, event);
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            directoriesByKey.remove(key);
                            if (directoriesByKey.isEmpty()) {
                                break;
                            }
                        }
                    }
                }

                if (errorFree) {
                    subscriber.onComplete();
                }
            });
        }

        private void registerAll(final Path rootDirectory, final WatchService watcher) throws IOException {
            Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    register(dir, watcher);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        private void register(final Path dir, final WatchService watcher) throws IOException {
            final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            directoriesByKey.put(key, dir);
        }

        private void registerNewDirectory(
                final ObservableEmitter<WatchEvent<?>> subscriber,
                final Path dir,
                final WatchService watcher,
                final WatchEvent<?> event) {
            final Kind<?> kind = event.kind();
            if (recursive && kind.equals(ENTRY_CREATE)) {
                @SuppressWarnings("unchecked") final WatchEvent<Path> eventWithPath = (WatchEvent<Path>) event;
                final Path name = eventWithPath.context();
                final Path child = dir.resolve(name);
                try {
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        registerAll(child, watcher);
                    }
                } catch (final IOException exception) {
                    subscriber.onError(exception);
                }
            }
        }
    }
}