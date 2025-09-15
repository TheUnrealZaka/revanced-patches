package app.revanced.extension.music.patches.misc.discordrpc;

import app.revanced.extension.shared.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simplified Discord WebSocket client for RPC functionality
 */
public abstract class DiscordWebSocketClient {
    
    private static final String TAG = "DiscordWebSocket";
    
    // Discord Gateway opcodes
    private static final int OP_DISPATCH = 0;
    private static final int OP_HEARTBEAT = 1;
    private static final int OP_IDENTIFY = 2;
    private static final int OP_PRESENCE_UPDATE = 3;
    private static final int OP_HELLO = 10;
    private static final int OP_HEARTBEAT_ACK = 11;
    
    private final URI uri;
    private final String token;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private WebSocketConnection connection;
    private int sequenceNumber = 0;
    private String sessionId;
    private boolean identified = false;
    private long heartbeatInterval = 0;
    
    public DiscordWebSocketClient(URI uri, String token) {
        this.uri = uri;
        this.token = token;
    }
    
    public void connect() {
        try {
            Logger.printDebug(() -> TAG + ": Connecting to " + uri);
            connection = new WebSocketConnection(uri) {
                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }
                
                @Override
                public void onClose() {
                    identified = false;
                    heartbeatExecutor.shutdownNow();
                    onDisconnected();
                }
            };
            connection.connect();
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Connection error", e);
        }
    }
    
    private void handleMessage(String message) {
        try {
            JSONObject payload = new JSONObject(message);
            int op = payload.getInt("op");
            
            if (payload.has("s") && !payload.isNull("s")) {
                sequenceNumber = payload.getInt("s");
            }
            
            switch (op) {
                case OP_HELLO:
                    handleHello(payload);
                    break;
                case OP_HEARTBEAT_ACK:
                    Logger.printDebug(() -> TAG + ": Heartbeat acknowledged");
                    break;
                case OP_DISPATCH:
                    handleDispatch(payload);
                    break;
                default:
                    Logger.printDebug(() -> TAG + ": Received opcode: " + op);
                    break;
            }
        } catch (JSONException e) {
            Logger.printException(() -> TAG + ": Error parsing message", e);
        }
    }
    
    private void handleHello(JSONObject payload) throws JSONException {
        JSONObject data = payload.getJSONObject("d");
        heartbeatInterval = data.getLong("heartbeat_interval");
        
        Logger.printDebug(() -> TAG + ": Received HELLO, heartbeat interval: " + heartbeatInterval);
        
        // Start heartbeat
        startHeartbeat();
        
        // Send identify
        sendIdentify();
    }
    
    private void handleDispatch(JSONObject payload) throws JSONException {
        String eventType = payload.getString("t");
        
        if ("READY".equals(eventType)) {
            JSONObject data = payload.getJSONObject("d");
            sessionId = data.getString("session_id");
            identified = true;
            
            Logger.printInfo(() -> TAG + ": Successfully identified with Discord");
            onReady();
        }
    }
    
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Heartbeat error", e);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    private void sendHeartbeat() throws JSONException {
        JSONObject heartbeat = new JSONObject();
        heartbeat.put("op", OP_HEARTBEAT);
        heartbeat.put("d", sequenceNumber > 0 ? sequenceNumber : JSONObject.NULL);
        
        send(heartbeat);
        Logger.printDebug(() -> TAG + ": Sent heartbeat");
    }
    
    private void sendIdentify() throws JSONException {
        JSONObject identify = new JSONObject();
        identify.put("op", OP_IDENTIFY);
        
        JSONObject data = new JSONObject();
        data.put("token", token);
        data.put("intents", 0); // No intents needed for RPC
        
        JSONObject properties = new JSONObject();
        properties.put("$os", "android");
        properties.put("$browser", "ReVanced Extended");
        properties.put("$device", "ReVanced Extended");
        data.put("properties", properties);
        
        identify.put("d", data);
        
        send(identify);
        Logger.printDebug(() -> TAG + ": Sent identify");
    }
    
    public void updatePresence(JSONObject presence) throws JSONException {
        if (!identified) {
            Logger.printDebug(() -> TAG + ": Not identified yet, skipping presence update");
            return;
        }
        
        JSONObject presenceUpdate = new JSONObject();
        presenceUpdate.put("op", OP_PRESENCE_UPDATE);
        presenceUpdate.put("d", presence);
        
        send(presenceUpdate);
    }
    
    private void send(JSONObject message) {
        if (connection != null && connection.isConnected()) {
            try {
                connection.send(message.toString());
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Error sending message", e);
            }
        }
    }
    
    public boolean isConnected() {
        return connection != null && connection.isConnected() && identified;
    }
    
    public void close() {
        heartbeatExecutor.shutdownNow();
        if (connection != null) {
            connection.close();
        }
    }
    
    public abstract void onReady();
    public abstract void onDisconnected();
}