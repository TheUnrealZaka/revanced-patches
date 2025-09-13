package app.revanced.extension.music.patches.misc.discordrpc;

import android.media.session.PlaybackState;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages Discord RPC connection and presence updates
 */
public class DiscordRpcManager {
    
    private static final String TAG = "DiscordRpcManager";
    private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    
    private final String token;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private DiscordWebSocketClient webSocketClient;
    private String currentTrack;
    private String currentArtist;
    private String currentAlbum;
    private int playbackState = PlaybackState.STATE_NONE;
    private long startTime;
    private long duration;
    
    public DiscordRpcManager(String token) {
        this.token = token;
        connect();
    }
    
    private void connect() {
        executor.submit(() -> {
            try {
                Logger.printDebug(() -> TAG + ": Connecting to Discord gateway");
                
                URI uri = URI.create(GATEWAY_URL);
                webSocketClient = new DiscordWebSocketClient(uri, token) {
                    @Override
                    public void onReady() {
                        Logger.printInfo(() -> TAG + ": Connected to Discord gateway");
                        updatePresence();
                    }
                    
                    @Override
                    public void onDisconnected() {
                        Logger.printInfo(() -> TAG + ": Disconnected from Discord gateway");
                        // Attempt reconnection after 5 seconds
                        Utils.runOnMainThreadDelayed(() -> connect(), 5000);
                    }
                };
                
                webSocketClient.connect();
                
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Error connecting to Discord", e);
            }
        });
    }
    
    public void updatePresence(String title, String artist, String album, long durationMs, int state, long position) {
        // Update internal state
        boolean trackChanged = !equals(currentTrack, title) || 
                              !equals(currentArtist, artist) ||
                              !equals(currentAlbum, album) ||
                              duration != durationMs;
        
        if (trackChanged) {
            currentTrack = title;
            currentArtist = artist;
            currentAlbum = album;
            duration = durationMs;
            
            // Reset start time when track changes
            if (state == PlaybackState.STATE_PLAYING) {
                startTime = System.currentTimeMillis() - position;
            }
            
            Logger.printDebug(() -> TAG + ": Track changed to: " + currentTrack + " by " + currentArtist);
        }
        
        // Update playback state
        if (state == PlaybackState.STATE_PLAYING && playbackState != PlaybackState.STATE_PLAYING) {
            startTime = System.currentTimeMillis() - position;
        }
        
        playbackState = state;
        
        // Update Discord presence
        updatePresence();
    }
    
    private boolean equals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }
    
    private void updatePresence() {
        if (webSocketClient == null || !webSocketClient.isConnected()) {
            return;
        }
        
        // Don't show presence if no track is playing
        if (currentTrack == null || currentTrack.isEmpty()) {
            clearPresence();
            return;
        }
        
        // Don't show presence if paused (optional behavior)
        if (playbackState == PlaybackState.STATE_PAUSED || 
            playbackState == PlaybackState.STATE_STOPPED) {
            clearPresence();
            return;
        }
        
        executor.submit(() -> {
            try {
                JSONObject presence = buildPresence();
                webSocketClient.updatePresence(presence);
                Logger.printDebug(() -> TAG + ": Updated Discord presence");
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Error updating presence", e);
            }
        });
    }
    
    private JSONObject buildPresence() throws JSONException {
        JSONObject activity = new JSONObject();
        activity.put("name", "YouTube Music");
        activity.put("type", 2); // Listening activity type
        activity.put("state", formatArtistAndAlbum());
        activity.put("details", currentTrack != null ? currentTrack : "Unknown Track");
        
        // Add timestamps for progress tracking
        if (playbackState == PlaybackState.STATE_PLAYING && startTime > 0) {
            JSONObject timestamps = new JSONObject();
            timestamps.put("start", startTime);
            if (duration > 0) {
                timestamps.put("end", startTime + duration);
            }
            activity.put("timestamps", timestamps);
        }
        
        // Add YouTube Music branding
        JSONObject assets = new JSONObject();
        assets.put("large_image", "youtube-music");
        assets.put("large_text", "YouTube Music");
        activity.put("assets", assets);
        
        JSONObject[] activities = {activity};
        JSONObject presence = new JSONObject();
        presence.put("activities", activities);
        presence.put("afk", false);
        presence.put("since", System.currentTimeMillis());
        presence.put("status", "online");
        
        return presence;
    }
    
    private String formatArtistAndAlbum() {
        StringBuilder sb = new StringBuilder();
        
        if (currentArtist != null && !currentArtist.isEmpty()) {
            sb.append("by ").append(currentArtist);
        }
        
        if (currentAlbum != null && !currentAlbum.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" • ");
            }
            sb.append(currentAlbum);
        }
        
        return sb.length() > 0 ? sb.toString() : "Unknown Artist";
    }
    
    public void clearPresence() {
        if (webSocketClient == null || !webSocketClient.isConnected()) {
            return;
        }
        
        executor.submit(() -> {
            try {
                JSONObject[] emptyActivities = {};
                JSONObject clearPresence = new JSONObject();
                clearPresence.put("activities", emptyActivities);
                clearPresence.put("afk", false);
                clearPresence.put("since", System.currentTimeMillis());
                clearPresence.put("status", "online");
                
                webSocketClient.updatePresence(clearPresence);
                Logger.printDebug(() -> TAG + ": Cleared Discord presence");
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Error clearing presence", e);
            }
        });
    }
    
    public void shutdown() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        executor.shutdown();
    }
}