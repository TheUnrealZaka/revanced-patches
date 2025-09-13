package app.revanced.extension.music.patches.misc.discordrpc;

import app.revanced.extension.shared.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Basic WebSocket connection implementation for Discord RPC
 */
public abstract class WebSocketConnection {
    
    private static final String TAG = "WebSocketConnection";
    private static final String WEBSOCKET_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    
    private final URI uri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private Socket socket;
    private OutputStream outputStream;
    private BufferedReader inputReader;
    private boolean connected = false;
    
    public WebSocketConnection(URI uri) {
        this.uri = uri;
    }
    
    public void connect() {
        executor.submit(() -> {
            try {
                performHandshake();
                startMessageLoop();
            } catch (Exception e) {
                Logger.printException(() -> TAG + ": Connection failed", e);
                onClose();
            }
        });
    }
    
    private void performHandshake() throws IOException {
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? (uri.getScheme().equals("wss") ? 443 : 80) : uri.getPort();
        
        // Create SSL socket for wss://
        if (uri.getScheme().equals("wss")) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = factory.createSocket(host, port);
        } else {
            socket = new Socket(host, port);
        }
        
        outputStream = socket.getOutputStream();
        inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        
        // Generate WebSocket key
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        String key = Base64.getEncoder().encodeToString(keyBytes);
        
        // Send WebSocket handshake
        String handshake = "GET " + uri.getPath() + "?" + uri.getQuery() + " HTTP/1.1\r\n" +
                "Host: " + host + "\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: " + key + "\r\n" +
                "Sec-WebSocket-Version: 13\r\n" +
                "\r\n";
        
        outputStream.write(handshake.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        
        // Read handshake response
        String line;
        boolean upgradeFound = false;
        while ((line = inputReader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("HTTP/1.1 101")) {
                upgradeFound = true;
            }
        }
        
        if (!upgradeFound) {
            throw new IOException("WebSocket handshake failed");
        }
        
        connected = true;
        Logger.printDebug(() -> TAG + ": WebSocket handshake successful");
    }
    
    private void startMessageLoop() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                String message = readFrame();
                if (message != null) {
                    onMessage(message);
                }
            }
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Message loop error", e);
        } finally {
            connected = false;
            onClose();
        }
    }
    
    private String readFrame() throws IOException {
        // Read WebSocket frame header
        int firstByte = inputReader.read();
        if (firstByte == -1) return null;
        
        int secondByte = inputReader.read();
        if (secondByte == -1) return null;
        
        boolean fin = (firstByte & 0x80) != 0;
        int opcode = firstByte & 0x0F;
        boolean masked = (secondByte & 0x80) != 0;
        long payloadLength = secondByte & 0x7F;
        
        // Handle extended payload length
        if (payloadLength == 126) {
            payloadLength = (inputReader.read() << 8) | inputReader.read();
        } else if (payloadLength == 127) {
            // For simplicity, we'll assume payload is never > Integer.MAX_VALUE
            inputReader.skip(4); // Skip first 4 bytes of 64-bit length
            payloadLength = (inputReader.read() << 24) | (inputReader.read() << 16) | 
                           (inputReader.read() << 8) | inputReader.read();
        }
        
        // Skip mask key if present (server shouldn't send masked frames)
        if (masked) {
            inputReader.skip(4);
        }
        
        // Read payload
        if (opcode == 1) { // Text frame
            char[] payload = new char[(int) payloadLength];
            int totalRead = 0;
            while (totalRead < payloadLength) {
                int read = inputReader.read(payload, totalRead, (int) payloadLength - totalRead);
                if (read == -1) break;
                totalRead += read;
            }
            return new String(payload, 0, totalRead);
        } else if (opcode == 8) { // Close frame
            connected = false;
            return null;
        }
        
        return null;
    }
    
    public void send(String message) throws IOException {
        if (!connected) {
            throw new IOException("WebSocket not connected");
        }
        
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        
        // Build WebSocket frame
        byte[] frame;
        if (payload.length < 126) {
            frame = new byte[2 + 4 + payload.length]; // header + mask + payload
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) (0x80 | payload.length); // masked + length
        } else if (payload.length < 65536) {
            frame = new byte[4 + 4 + payload.length]; // extended header + mask + payload
            frame[0] = (byte) 0x81; // FIN + text frame
            frame[1] = (byte) 0xFE; // masked + 126
            frame[2] = (byte) (payload.length >> 8);
            frame[3] = (byte) (payload.length & 0xFF);
        } else {
            throw new IOException("Payload too large");
        }
        
        // Generate mask
        byte[] mask = new byte[4];
        new SecureRandom().nextBytes(mask);
        
        // Add mask to frame
        int maskOffset = frame.length - payload.length - 4;
        System.arraycopy(mask, 0, frame, maskOffset, 4);
        
        // Mask payload and add to frame
        for (int i = 0; i < payload.length; i++) {
            frame[maskOffset + 4 + i] = (byte) (payload[i] ^ mask[i % 4]);
        }
        
        synchronized (outputStream) {
            outputStream.write(frame);
            outputStream.flush();
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void close() {
        connected = false;
        executor.shutdown();
        
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.printException(() -> TAG + ": Error closing socket", e);
        }
    }
    
    public abstract void onMessage(String message);
    public abstract void onClose();
}