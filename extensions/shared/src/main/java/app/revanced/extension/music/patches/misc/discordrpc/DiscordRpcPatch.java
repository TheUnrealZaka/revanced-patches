package app.revanced.extension.music.patches.misc.discordrpc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class DiscordRpcPatch {
    private static final String DISCORD_PACKAGE = "com.discord";
    private static final String RPC_ACTION = "com.discord.rpc.UPDATE_PRESENCE";
    
    private static String currentTitle = "";
    private static String currentArtist = "";
    private static String currentAlbum = "";
    private static boolean isPlaying = false;
    
    /**
     * Called when track metadata is updated
     */
    public static void onTrackMetadataChanged(String title, String artist, String album) {
        if (!Settings.DISCORD_RPC_ENABLED.get()) {
            return;
        }
        
        try {
            currentTitle = title != null ? title : "";
            currentArtist = artist != null ? artist : "";
            currentAlbum = album != null ? album : "";
            
            Logger.printDebug(() -> "Discord RPC: Track changed - " + currentTitle + " by " + currentArtist);
            updateDiscordPresence();
        } catch (Exception ex) {
            Logger.printException(() -> "onTrackMetadataChanged failure", ex);
        }
    }
    
    /**
     * Called when metadata update is attempted - tries to extract info via reflection
     */
    public static void onMetadataUpdateAttempt(Object metadata) {
        if (!Settings.DISCORD_RPC_ENABLED.get()) {
            return;
        }
        
        try {
            if (metadata == null) return;
            
            // Try to extract metadata using reflection
            String title = extractMetadataString(metadata, "title", "TITLE");
            String artist = extractMetadataString(metadata, "artist", "ARTIST");  
            String album = extractMetadataString(metadata, "album", "ALBUM");
            
            // Only update if we got some meaningful data
            if (title != null && !title.trim().isEmpty()) {
                onTrackMetadataChanged(title, artist, album);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onMetadataUpdateAttempt failure", ex);
        }
    }
    
    /**
     * Helper method to extract metadata strings via reflection
     */
    private static String extractMetadataString(Object metadata, String... keys) {
        try {
            Class<?> metadataClass = metadata.getClass();
            
            // Try different method names for getting string values
            String[] methodNames = {
                "getString", "get", "getCharSequence", "getText"
            };
            
            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = metadataClass.getMethod(methodName, String.class);
                    
                    for (String key : keys) {
                        try {
                            Object result = method.invoke(metadata, "android.media.metadata." + key);
                            if (result != null) {
                                return result.toString();
                            }
                            
                            // Also try without prefix
                            result = method.invoke(metadata, key.toLowerCase());
                            if (result != null) {
                                return result.toString();
                            }
                        } catch (Exception ignored) {
                            // Try next key
                        }
                    }
                } catch (Exception ignored) {
                    // Try next method
                }
            }
        } catch (Exception ignored) {
            // Return null if extraction fails
        }
        return null;
    }
    
    /**
     * Called when playback state changes
     */
    public static void onPlaybackStateChanged(boolean playing) {
        if (!Settings.DISCORD_RPC_ENABLED.get()) {
            return;
        }
        
        try {
            isPlaying = playing;
            Logger.printDebug(() -> "Discord RPC: Playback state changed - " + (playing ? "playing" : "paused"));
            updateDiscordPresence();
        } catch (Exception ex) {
            Logger.printException(() -> "onPlaybackStateChanged failure", ex);
        }
    }
    
    /**
     * Updates Discord presence via broadcast intent
     */
    private static void updateDiscordPresence() {
        try {
            Context context = Utils.getContext();
            if (context == null) {
                Logger.printDebug(() -> "Discord RPC: Context is null");
                return;
            }
            
            // Check if Discord is installed
            if (!isDiscordInstalled(context)) {
                Logger.printDebug(() -> "Discord RPC: Discord not installed");
                return;
            }
            
            // Create presence data
            Bundle presenceData = new Bundle();
            presenceData.putString("application_id", "1174421807340478495"); // Example app ID for YT Music
            presenceData.putString("type", "0"); // PLAYING
            
            if (isPlaying && !currentTitle.isEmpty()) {
                presenceData.putString("name", "YouTube Music");
                presenceData.putString("details", currentTitle);
                
                if (!currentArtist.isEmpty()) {
                    String state = currentArtist;
                    if (!currentAlbum.isEmpty()) {
                        state += " • " + currentAlbum;
                    }
                    presenceData.putString("state", state);
                }
                
                presenceData.putString("large_image", "youtube_music");
                presenceData.putString("large_text", "YouTube Music");
                presenceData.putString("small_image", "play");
                presenceData.putString("small_text", "Playing");
                
                // Set start timestamp for elapsed time
                presenceData.putLong("start", System.currentTimeMillis());
            } else {
                // Clear presence when not playing
                presenceData.putString("name", "");
                presenceData.putString("details", "");
                presenceData.putString("state", "");
            }
            
            // Send broadcast intent to Discord
            Intent intent = new Intent(RPC_ACTION);
            intent.setPackage(DISCORD_PACKAGE);
            intent.putExtras(presenceData);
            
            context.sendBroadcast(intent);
            Logger.printDebug(() -> "Discord RPC: Presence updated - " + currentTitle);
            
        } catch (Exception ex) {
            Logger.printException(() -> "updateDiscordPresence failure", ex);
        }
    }
    
    /**
     * Checks if Discord is installed
     */
    private static boolean isDiscordInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(DISCORD_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Called when the player is cleared/stopped
     */
    public static void onPlayerCleared() {
        if (!Settings.DISCORD_RPC_ENABLED.get()) {
            return;
        }
        
        try {
            currentTitle = "";
            currentArtist = "";
            currentAlbum = "";
            isPlaying = false;
            
            updateDiscordPresence();
            Logger.printDebug(() -> "Discord RPC: Player cleared");
        } catch (Exception ex) {
            Logger.printException(() -> "onPlayerCleared failure", ex);
        }
    }
}