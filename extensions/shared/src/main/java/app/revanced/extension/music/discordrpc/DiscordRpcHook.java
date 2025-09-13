package app.revanced.extension.music.discordrpc;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.util.Log;

/**
 * Hook class that integrates Discord RPC with YouTube Music
 * Provides both initialization and fallback hooks for media updates
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
     * Fallback method for media metadata updates
     * This can be called from other hooks if MediaSessionManager doesn't work
     */
    public static void onMediaMetadataChanged(MediaMetadata metadata) {
        if (!initialized || rpcManager == null || metadata == null) {
            return;
        }
        
        try {
            String title = getMetadataString(metadata, MediaMetadata.METADATA_KEY_TITLE);
            String artist = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ARTIST);
            String album = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM);
            String artworkUrl = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ART_URI);
            
            if (artworkUrl == null || artworkUrl.isEmpty()) {
                artworkUrl = getMetadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
            }
            
            long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
            
            Log.d(TAG, "Fallback: Media metadata changed: " + title + " by " + artist);
            
            // Use the RPC manager directly for updates
            rpcManager.updateCurrentTrack(title, artist, album, artworkUrl, duration, 0, true);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing fallback media metadata", e);
        }
    }
    
    /**
     * Fallback method for playback state updates  
     * This can be called from other hooks if MediaSessionManager doesn't work
     */
    public static void onPlaybackStateChanged(PlaybackState playbackState) {
        if (!initialized || rpcManager == null || playbackState == null) {
            return;
        }
        
        try {
            int state = playbackState.getState();
            long position = playbackState.getPosition();
            boolean isPlaying = (state == PlaybackState.STATE_PLAYING);
            
            Log.d(TAG, "Fallback: Playback state changed: " + (isPlaying ? "playing" : "paused"));
            
            // For fallback, we'd need to combine with last known metadata
            // This is a simplified implementation
            if (!isPlaying && app.revanced.extension.music.settings.Settings.DISCORD_RPC_HIDE_ON_PAUSE.get()) {
                rpcManager.clearPresence();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing fallback playback state", e);
        }
    }
    
    /**
     * Manual update method for other hooks to use
     */
    public static void updateTrack(String title, String artist, String album, String artwork, boolean isPlaying) {
        if (!initialized || rpcManager == null) {
            return;
        }
        
        try {
            rpcManager.updateCurrentTrack(title, artist, album, artwork, 0, 0, isPlaying);
        } catch (Exception e) {
            Log.e(TAG, "Error in manual track update", e);
        }
    }
    
    /**
     * Helper method to safely get string metadata
     */
    private static String getMetadataString(MediaMetadata metadata, String key) {
        try {
            return metadata.getString(key);
        } catch (Exception e) {
            return null;
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