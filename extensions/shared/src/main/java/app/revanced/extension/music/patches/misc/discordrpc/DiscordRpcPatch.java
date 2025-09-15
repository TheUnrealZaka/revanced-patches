package app.revanced.extension.music.patches.misc.discordrpc;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

import java.util.List;

/**
 * Discord RPC patch for YouTube Music.
 * Uses MediaSessionManager to monitor currently playing media.
 */
public class DiscordRpcPatch {
    
    private static final String TAG = "DiscordRPC";
    
    // Settings
    private static final BooleanSetting DISCORD_RPC_ENABLED = new BooleanSetting("revanced_discord_rpc_enabled", false);
    private static final StringSetting DISCORD_TOKEN = new StringSetting("revanced_discord_token", "");
    
    private static DiscordRpcManager rpcManager;
    private static MediaSessionManager mediaSessionManager;
    private static boolean initialized = false;
    
    /**
     * Initialize Discord RPC when the app starts
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        Logger.printDebug(() -> TAG + ": Initializing Discord RPC");
        
        if (!DISCORD_RPC_ENABLED.get()) {
            Logger.printDebug(() -> TAG + ": Discord RPC disabled in settings");
            return;
        }
        
        String token = DISCORD_TOKEN.get();
        if (token.isEmpty()) {
            Logger.printDebug(() -> TAG + ": Discord token not configured");
            return;
        }
        
        // Delay initialization to ensure context is available
        Utils.runOnMainThreadDelayed(() -> initializeWithRetry(0), 2000);
    }
    
    private static void initializeWithRetry(int attempt) {
        try {
            Context context = Utils.getContext();
            if (context == null) {
                if (attempt < 5) {
                    Logger.printDebug(() -> TAG + ": Context not available, retrying in 3 seconds (attempt " + (attempt + 1) + ")");
                    Utils.runOnMainThreadDelayed(() -> initializeWithRetry(attempt + 1), 3000);
                } else {
                    Logger.printDebug(() -> TAG + ": Context not available after 5 attempts, giving up");
                }
                return;
            }
            
            String token = DISCORD_TOKEN.get();
            
            // Initialize Discord RPC manager
            rpcManager = new DiscordRpcManager(token);
            
            // Set up media session monitoring
            setupMediaSessionMonitoring(context);
            
            Logger.printInfo(() -> TAG + ": Discord RPC initialized successfully");
            
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Error initializing Discord RPC", e);
        }
    }
    
    /**
     * Manual initialization method for fallback
     */
    public static void initializeManually() {
        Logger.printDebug(() -> TAG + ": Manual initialization requested");
        initialized = false; // Reset flag to allow re-initialization
        initialize();
    }
    
    private static void setupMediaSessionMonitoring(Context context) {
        try {
            mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            
            if (mediaSessionManager == null) {
                Logger.printDebug(() -> TAG + ": MediaSessionManager not available");
                return;
            }
            
            // Start monitoring media sessions
            Utils.runOnBackgroundThread(() -> {
                while (rpcManager != null) {
                    try {
                        checkCurrentPlayingMedia();
                        Thread.sleep(2000); // Check every 2 seconds
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Logger.printException(() -> TAG + ": Error in media monitoring loop", e);
                    }
                }
            });
            
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Error setting up media session monitoring", e);
        }
    }
    
    private static void checkCurrentPlayingMedia() {
        try {
            if (mediaSessionManager == null || rpcManager == null) return;
            
            Context context = Utils.getContext();
            if (context == null) return;
            
            // Get active media sessions
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(null);
            
            MediaController youtubeMediaController = null;
            for (MediaController controller : controllers) {
                String packageName = controller.getPackageName();
                if (packageName != null && packageName.equals(context.getPackageName())) {
                    // This is YouTube Music itself
                    youtubeMediaController = controller;
                    break;
                }
            }
            
            if (youtubeMediaController != null) {
                updateFromMediaController(youtubeMediaController);
            } else {
                // No YouTube Music session found, clear presence
                rpcManager.clearPresence();
            }
            
        } catch (SecurityException e) {
            Logger.printDebug(() -> TAG + ": Missing notification access permission for media session monitoring");
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Error checking current playing media", e);
        }
    }
    
    private static void updateFromMediaController(MediaController controller) {
        try {
            MediaMetadata metadata = controller.getMetadata();
            PlaybackState playbackState = controller.getPlaybackState();
            
            if (metadata == null || playbackState == null) {
                return;
            }
            
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            
            int state = playbackState.getState();
            long position = playbackState.getPosition();
            
            Logger.printDebug(() -> TAG + ": Track: " + title + " by " + artist + 
                            " (State: " + state + ", Position: " + position + ")");
            
            rpcManager.updatePresence(title, artist, album, duration, state, position);
            
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Error updating from media controller", e);
        }
    }
    
    /**
     * Shutdown Discord RPC
     */
    public static void shutdown() {
        if (rpcManager != null) {
            rpcManager.shutdown();
            rpcManager = null;
        }
        initialized = false;
    }
}