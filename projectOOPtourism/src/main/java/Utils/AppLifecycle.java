package Utils;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AppLifecycle {
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    private AppLifecycle() {
    }

    public static boolean beginShutdown() {
        return SHUTTING_DOWN.compareAndSet(false, true);
    }

    public static boolean isShuttingDown() {
        return SHUTTING_DOWN.get();
    }
}
