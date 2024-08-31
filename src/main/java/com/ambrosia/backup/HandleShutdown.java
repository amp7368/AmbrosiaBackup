package com.ambrosia.backup;

import com.ambrosia.backup.shutdown.ShutdownExports;
import java.util.List;
import org.apache.logging.log4j.message.FormattedMessage;

public interface HandleShutdown {

    static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(HandleShutdown::shutdown));
    }

    private static void shutdown() {
        List<HandleShutdown> shutdownHooks = List.of(ShutdownExports.get());
        shutdownHooks.forEach(HandleShutdown::tryStartShutdown);
        Thread currentThread = Thread.currentThread();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread == currentThread) continue;
            thread.interrupt();
        }
        shutdownHooks.forEach(HandleShutdown::tryEndShutdown);
    }

    default void startShutdown() throws Exception {
    }

    private void tryStartShutdown() {
        try {
            startShutdown();
        } catch (Exception e) {
            log(e);
        }
    }

    private void log(Exception e) {
        FormattedMessage msg = new FormattedMessage("Exception when shutting down {}", this.name());
        Ambrosia.logger().error(msg, e);
    }

    String name();

    default void endShutdown() throws Exception {
    }

    private void tryEndShutdown() {
        try {
            endShutdown();
        } catch (Exception e) {
            log(e);
        }
    }
}
