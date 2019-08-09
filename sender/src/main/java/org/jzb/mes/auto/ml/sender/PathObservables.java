package org.jzb.mes.auto.ml.sender;


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

    /**
     * Creates an observable that watches the given directory and all its subdirectories. Directories
     * that are created after subscription are watched, too.
     *
     * @param path Root directory to be watched
     * @return Observable that emits an event for each filesystem event.
     * @throws IOException
     */
    public static Observable<WatchEvent<?>> watchRecursive(final Path path) {
        final boolean recursive = true;
        return new ObservableFactory(path, recursive).create();
    }

    /**
     * Creates an observable that watches the given path but not its subdirectories.
     *
     * @param path Path to be watched
     * @return Observable that emits an event for each filesystem event.
     */
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
                        // reset key and remove from set if directory is no longer accessible
                        boolean valid = key.reset();
                        if (!valid) {
                            directoriesByKey.remove(key);
                            // nothing to be watched
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

        /**
         * Register the rootDirectory, and all its sub-directories.
         */
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

        // register newly created directory to watching in recursive mode
        private void registerNewDirectory(
                final ObservableEmitter<WatchEvent<?>> subscriber,
                final Path dir,
                final WatchService watcher,
                final WatchEvent<?> event) {
            final Kind<?> kind = event.kind();
            if (recursive && kind.equals(ENTRY_CREATE)) {
                // Context for directory entry event is the file name of entry
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