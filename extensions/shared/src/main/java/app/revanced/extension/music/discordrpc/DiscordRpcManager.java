package app.revanced.extension.music.discordrpc;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.revanced.extension.music.settings.Settings;

/**
 * Main manager class for Discord RPC functionality in YouTube Music.
 * Uses MediaSessionManager to detect music playback similar to Kizzy's approach.
 */
public class DiscordRpcManager {
    private static final String TAG = "DiscordRPC";
    
    private static DiscordRpcManager instance;
    private Context context;
    private ExecutorService executor;
    private Handler mainHandler;
    private DiscordGateway gateway;
    private MediaSessionManager mediaSessionManager;
    private MediaController currentController;
    private MusicMetadata currentTrack;
    private boolean isEnabled = false;
    private boolean isConnected = false;
    
    private DiscordRpcManager() {
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized DiscordRpcManager getInstance() {
        if (instance == null) {
            instance = new DiscordRpcManager();
        }
        return instance;
    }
    
    /**
     * Initialize the Discord RPC manager with application context
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        isEnabled = Settings.DISCORD_RPC_ENABLED.get();
        
        if (isEnabled) {
            String token = Settings.DISCORD_TOKEN.get();
            if (token != null && !token.isEmpty()) {
                connectToDiscord(token);
                setupMediaSessionListener();
            } else {
                Log.w(TAG, "Discord RPC enabled but no token provided");
            }
        }
    }
    
    /**
     * Setup MediaSessionManager to listen for active media sessions
     */
    private void setupMediaSessionListener() {
        try {
            mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                // Use a simple ComponentName - we don't need notification listener for this approach
                ComponentName component = new ComponentName(context, context.getClass());
                
                // Get active sessions and register callback
                updateActiveSession();
                
                // Set up periodic checking for active sessions
                mainHandler.postDelayed(sessionChecker, 5000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup media session listener", e);
        }
    }
    
    private final Runnable sessionChecker = new Runnable() {
        @Override
        public void run() {
            if (isEnabled && mediaSessionManager != null) {
                updateActiveSession();
                mainHandler.postDelayed(this, 5000); // Check every 5 seconds
            }
        }
    };
    
    /**
     * Update the active media session controller
     */
    private void updateActiveSession() {
        try {
            // Note: In newer Android versions, we might need notification access permission
            // to get active sessions. For this implementation, we'll try to get sessions
            // but gracefully handle SecurityException
            ComponentName component = new ComponentName(context, context.getClass());
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(component);
            MediaController ytMusicController = null;
            
            for (MediaController controller : controllers) {
                if (controller.getPackageName().equals(context.getPackageName())) {
                    ytMusicController = controller;
                    break;
                }
            }
            
            if (ytMusicController != currentController) {
                if (currentController != null) {
                    currentController.unregisterCallback(mediaCallback);
                }
                
                currentController = ytMusicController;
                
                if (currentController != null) {
                    currentController.registerCallback(mediaCallback);
                    // Get initial state
                    updatePresenceFromController();
                } else {
                    clearPresence();
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to access media sessions, using fallback approach");
            // Fallback: we'll rely on the hook methods if they get called
        } catch (Exception e) {
            Log.e(TAG, "Error updating active session", e);
        }
    }
    
    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            super.onMetadataChanged(metadata);
            Log.d(TAG, "Media metadata changed");
            updatePresenceFromController();
        }
        
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            super.onPlaybackStateChanged(state);
            Log.d(TAG, "Playback state changed");
            updatePresenceFromController();
        }
    };
    
    /**
     * Update Discord presence from current media controller
     */
    private void updatePresenceFromController() {
        if (currentController == null) {
            clearPresence();
            return;
        }
        
        try {
            MediaMetadata metadata = currentController.getMetadata();
            PlaybackState playbackState = currentController.getPlaybackState();
            
            if (metadata == null) {
                clearPresence();
                return;
            }
            
            String title = getMetadataString(metadata, MediaMetadata.METADATA_KEY_TITLE);
            String artist = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ARTIST);
            String album = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM);
            String artworkUrl = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ART_URI);
            
            if (artworkUrl == null || artworkUrl.isEmpty()) {
                artworkUrl = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
            }
            
            long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            long position = playbackState != null ? playbackState.getPosition() : 0;
            boolean isPlaying = playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING;
            
            updateCurrentTrack(title, artist, album, artworkUrl, duration, position, isPlaying);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating presence from controller", e);
        }
    }
    
    private String getMetadataString(MediaMetadata metadata, String key) {
        try {
            return metadata.getString(key);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Connect to Discord gateway
     */
    private void connectToDiscord(String token) {
        executor.execute(() -> {
            try {
                gateway = new DiscordGateway(token);
                gateway.connect();
                isConnected = true;
                Log.i(TAG, "Connected to Discord successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to connect to Discord", e);
                isConnected = false;
            }
        });
    }
    
    /**
     * Update the current playing track information
     */
    public void updateCurrentTrack(String title, String artist, String album, String thumbnailUrl, long duration, long position, boolean isPlaying) {
        if (!isEnabled || !isConnected) {
            return;
        }
        
        // Check if we should hide during pause
        if (!isPlaying && Settings.DISCORD_RPC_HIDE_ON_PAUSE.get()) {
            clearPresence();
            return;
        }
        
        currentTrack = new MusicMetadata(title, artist, album, thumbnailUrl, duration, position, isPlaying);
        
        executor.execute(() -> {
            try {
                if (gateway != null && currentTrack.isValid()) {
                    gateway.updatePresence(currentTrack);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update Discord presence", e);
            }
        });
    }
    
    /**
     * Clear the Discord presence
     */
    public void clearPresence() {
        if (!isConnected) {
            return;
        }
        
        executor.execute(() -> {
            try {
                if (gateway != null) {
                    gateway.clearPresence();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear Discord presence", e);
            }
        });
    }
    
    /**
     * Enable or disable Discord RPC
     */
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        Settings.DISCORD_RPC_ENABLED.save(enabled);
        
        if (!enabled && isConnected) {
            clearPresence();
            disconnect();
        } else if (enabled && !isConnected) {
            String token = Settings.DISCORD_TOKEN.get();
            if (token != null && !token.isEmpty()) {
                connectToDiscord(token);
                setupMediaSessionListener();
            }
        }
    }
    
    /**
     * Set Discord token
     */
    public void setDiscordToken(String token) {
        Settings.DISCORD_TOKEN.save(token);
        
        if (isEnabled) {
            if (isConnected) {
                disconnect();
            }
            connectToDiscord(token);
        }
    }
    
    /**
     * Disconnect from Discord
     */
    private void disconnect() {
        executor.execute(() -> {
            try {
                if (currentController != null) {
                    currentController.unregisterCallback(mediaCallback);
                    currentController = null;
                }
                
                if (gateway != null) {
                    gateway.disconnect();
                    gateway = null;
                }
                
                mainHandler.removeCallbacks(sessionChecker);
                isConnected = false;
                Log.i(TAG, "Disconnected from Discord");
            } catch (Exception e) {
                Log.e(TAG, "Error during Discord disconnect", e);
            }
        });
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (isConnected) {
            clearPresence();
            disconnect();
        }
        
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
}