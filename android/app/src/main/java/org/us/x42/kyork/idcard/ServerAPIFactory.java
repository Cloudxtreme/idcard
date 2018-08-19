package org.us.x42.kyork.idcard;

public class ServerAPIFactory {
    private static final boolean DEBUG = true;
    private static final Object LOCK = new Object();
    private static ServerAPI instance;

    public static ServerAPI getAPI() {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            if (instance != null) {
                return instance;
            }
            if (DEBUG) {
                instance = new ServerAPIDebug();
            } else {
                throw new RuntimeException("NotImplemented");
            }
        }
        return instance;
    }
}
