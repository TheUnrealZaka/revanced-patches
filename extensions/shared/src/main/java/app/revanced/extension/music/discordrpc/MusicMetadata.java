package app.revanced.extension.music.discordrpc;

/**
 * Data class to hold music metadata for Discord RPC
 */
public class MusicMetadata {
    public final String title;
    public final String artist;
    public final String album;
    public final String thumbnailUrl;
    public final long duration;
    public final long position;
    public final boolean isPlaying;
    public final long timestamp;
    
    public MusicMetadata(String title, String artist, String album, String thumbnailUrl, 
                         long duration, long position, boolean isPlaying) {
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.album = album != null ? album : "";
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.duration = duration;
        this.position = position;
        this.isPlaying = isPlaying;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the display name for the activity
     */
    public String getActivityName() {
        return "YouTube Music";
    }
    
    /**
     * Get the details string for Discord presence
     */
    public String getDetails() {
        if (title.isEmpty()) {
            return null;
        }
        return title;
    }
    
    /**
     * Get the state string for Discord presence based on user settings
     */
    public String getState() {
        // Check user preferences for what to show
        boolean showArtist = app.revanced.extension.music.settings.Settings.DISCORD_RPC_SHOW_ARTIST.get();
        boolean showAlbum = app.revanced.extension.music.settings.Settings.DISCORD_RPC_SHOW_ALBUM.get();
        
        if (!showArtist && !showAlbum) {
            return null;
        }
        
        if (showArtist && showAlbum && !artist.isEmpty() && !album.isEmpty()) {
            return "by " + artist + " • " + album;
        } else if (showArtist && !artist.isEmpty()) {
            return "by " + artist;
        } else if (showAlbum && !album.isEmpty()) {
            return album;
        } else {
            return null;
        }
    }
    
    /**
     * Get start timestamp for presence based on user settings
     */
    public Long getStartTimestamp() {
        boolean showTimestamps = app.revanced.extension.music.settings.Settings.DISCORD_RPC_SHOW_TIMESTAMPS.get();
        if (!showTimestamps || !isPlaying || position < 0) {
            return null;
        }
        return timestamp - position;
    }
    
    /**
     * Get end timestamp for presence based on user settings
     */
    public Long getEndTimestamp() {
        boolean showTimestamps = app.revanced.extension.music.settings.Settings.DISCORD_RPC_SHOW_TIMESTAMPS.get();
        if (!showTimestamps || !isPlaying || duration <= 0 || position < 0) {
            return null;
        }
        long remaining = duration - position;
        return timestamp + remaining;
    }
    
    /**
     * Get large image URL for presence
     */
    public String getLargeImageUrl() {
        return thumbnailUrl.isEmpty() ? null : thumbnailUrl;
    }
    
    /**
     * Get large image text for presence
     */
    public String getLargeImageText() {
        if (title.isEmpty()) {
            return null;
        }
        return title + (artist.isEmpty() ? "" : " by " + artist);
    }
    
    /**
     * Get small image key for presence (play/pause icon)
     */
    public String getSmallImageKey() {
        return isPlaying ? "play" : "pause";
    }
    
    /**
     * Get small image text for presence
     */
    public String getSmallImageText() {
        return isPlaying ? "Playing" : "Paused";
    }
    
    /**
     * Check if this metadata is valid for showing presence
     */
    public boolean isValid() {
        return !title.isEmpty();
    }
    
    @Override
    public String toString() {
        return "MusicMetadata{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", isPlaying=" + isPlaying +
                '}';
    }
}