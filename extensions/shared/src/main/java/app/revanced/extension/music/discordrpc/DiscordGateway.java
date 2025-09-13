package app.revanced.extension.music.discordrpc;

import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocketFactory;

/**
 * Discord Gateway WebSocket client for sending RPC presence updates
 * Based on Discord Gateway API v10
 */
public class DiscordGateway {
    private static final String TAG = "DiscordGateway";
    private static final String GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json";
    private static final String USER_AGENT = "DiscordBot (https://github.com/revanced/revanced-patches, 1.0.0)";
    
    private final String token;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private Thread heartbeatThread;
    private long heartbeatInterval = 45000; // Default 45 seconds
    
    public DiscordGateway(String token) {
        this.token = token;
    }
    
    /**
     * Connect to Discord Gateway
     */
    public void connect() throws Exception {
        if (connected.get()) {
            return;
        }
        
        Log.i(TAG, "Connecting to Discord Gateway...");
        
        // For simplicity, we'll use a basic HTTP approach to set presence
        // In a full implementation, you'd use WebSockets
        setPresenceViaHttp(null, true); // Clear any existing presence first
        connected.set(true);
    }
    
    /**
     * Update Discord presence
     */
    public void updatePresence(MusicMetadata metadata) throws Exception {
        if (!connected.get() || metadata == null || !metadata.isValid()) {
            return;
        }
        
        Log.d(TAG, "Updating presence: " + metadata);
        setPresenceViaHttp(metadata, false);
    }
    
    /**
     * Clear Discord presence
     */
    public void clearPresence() throws Exception {
        if (!connected.get()) {
            return;
        }
        
        Log.d(TAG, "Clearing presence");
        setPresenceViaHttp(null, true);
    }
    
    /**
     * Set presence via Discord HTTP API
     * Using custom status instead of activity for broader compatibility
     */
    private void setPresenceViaHttp(MusicMetadata metadata, boolean clear) throws Exception {
        URL url = new URL("https://discord.com/api/v10/users/@me/settings");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Authorization", token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000); // 10 seconds
            conn.setReadTimeout(10000);    // 10 seconds
            
            JSONObject payload = new JSONObject();
            
            if (clear || metadata == null || !metadata.isValid()) {
                // Clear presence
                payload.put("custom_status", JSONObject.NULL);
            } else {
                // Set presence with music info
                JSONObject customStatus = new JSONObject();
                
                String statusText = metadata.getDetails();
                String stateInfo = metadata.getState();
                
                if (statusText != null) {
                    if (stateInfo != null) {
                        statusText = statusText + " " + stateInfo;
                    }
                    
                    // Limit status text length to Discord's limit (128 characters)
                    if (statusText.length() > 128) {
                        statusText = statusText.substring(0, 125) + "...";
                    }
                    
                    customStatus.put("text", statusText);
                    customStatus.put("emoji_id", JSONObject.NULL);
                    customStatus.put("emoji_name", "\uD83C\uDFB5"); // Musical note emoji
                    customStatus.put("expires_at", JSONObject.NULL);
                } else {
                    // Fallback to just showing "Listening to YouTube Music"
                    customStatus.put("text", "Listening to YouTube Music");
                    customStatus.put("emoji_id", JSONObject.NULL);
                    customStatus.put("emoji_name", "\uD83C\uDFB5");
                    customStatus.put("expires_at", JSONObject.NULL);
                }
                
                payload.put("custom_status", customStatus);
            }
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, "Presence updated successfully");
            } else {
                Log.w(TAG, "Failed to update presence: HTTP " + responseCode);
                
                // Read error response for debugging
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    Log.w(TAG, "Response: " + response.toString());
                } catch (Exception e) {
                    Log.w(TAG, "Could not read error response", e);
                }
            }
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * Disconnect from Discord
     */
    public void disconnect() {
        if (!connected.get()) {
            return;
        }
        
        Log.i(TAG, "Disconnecting from Discord Gateway");
        
        try {
            // Clear presence before disconnecting
            setPresenceViaHttp(null, true);
        } catch (Exception e) {
            Log.w(TAG, "Failed to clear presence on disconnect", e);
        }
        
        connected.set(false);
        
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        
        closeSocket();
    }
    
    private void closeSocket() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing socket", e);
        }
    }
    
    public boolean isConnected() {
        return connected.get();
    }
}