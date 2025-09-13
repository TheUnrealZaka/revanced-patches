package app.revanced.extension.music.discordrpc;

import android.content.Context;
import android.util.Log;

/**
 * Hook class that integrates Discord RPC with YouTube Music
 */
public final class DiscordRpcHook {
    private static final String TAG = "DiscordRpcHook";
    private static DiscordRpcManager rpcManager;
    private static boolean initialized = false;
    
    /**
     * Initialize Discord RPC with the application context
     * This is called when YouTube Music starts up
     */
    public static void initializeDiscordRpc(Context context) {
        if (initialized) {
            return;
        }
        
        try {
            rpcManager = DiscordRpcManager.getInstance();
            rpcManager.initialize(context);
            initialized = true;
            Log.i(TAG, "Discord RPC initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Discord RPC", e);
        }
    }
    
    /**
     * Cleanup Discord RPC
     */
    public static void cleanup() {
        if (rpcManager != null) {
            rpcManager.cleanup();
            rpcManager = null;
        }
        initialized = false;
    }
}