# Discord RPC Patch for YouTube Music (ReVanced Extended)

This patch adds Discord Rich Presence support to YouTube Music, showing your currently playing music on Discord with "Listening to YouTube Music" status.

## Features

- **Rich Presence Integration**: Shows track name, artist, album, and playback progress on Discord
- **Activity Type**: Uses proper "Listening" activity type (type 2) 
- **Real-time Updates**: Updates presence when tracks change or playback state changes
- **Privacy Friendly**: Only shows presence when music is actively playing (paused tracks are hidden by default)
- **Secure Token Handling**: Uses Discord user token (not bot token) for authentication

## Setup Instructions

### 1. Enable the Patch

1. Open YouTube Music ReVanced Extended
2. Go to Settings → RVX → Discord RPC
3. Enable "Enable Discord RPC"

### 2. Get Your Discord Token

**Desktop Method:**
1. Open Discord in browser at discord.com/app
2. Press F12 to open Developer Tools
3. Go to Application tab
4. Navigate to Local Storage → discord.com
5. Find "token" key and copy its value (without quotes)

**Mobile Method:**
1. Install Firefox browser on your device
2. Go to discord.com/app in Firefox
3. Enable desktop mode in browser settings
4. Follow the same steps as desktop method

### 3. Configure the Token

1. In YouTube Music settings, go to Discord RPC
2. Tap "Discord Token"
3. Paste your token (should start with "mfa." or be a long alphanumeric string)
4. Save the settings

### 4. Verify It's Working

1. Play any song in YouTube Music
2. Check your Discord profile - you should see "Listening to YouTube Music" with track details
3. If it doesn't appear, try manually initializing via "Initialize Discord RPC" button

## Troubleshooting

### Token Issues
- Make sure your token is valid and hasn't expired
- Discord tokens can expire, you may need to get a new one periodically
- Never share your token with anyone - it's like your Discord password

### Permission Issues
- The patch requires notification access permission for media monitoring
- Grant notification access when prompted by the system

### Connection Issues
- Check your internet connection
- Discord may temporarily block connections - wait a few minutes and try again
- If the connection fails repeatedly, regenerate your Discord token

### Manual Initialization
- If automatic initialization fails, use the "Initialize Discord RPC" button in settings
- Check app logs for error messages (requires debug logging enabled)

## Privacy and Security

- Your Discord token is stored locally on your device only
- The patch connects directly to Discord's servers, no third-party services involved
- Only basic track information is shared (title, artist, album, playback status)
- Your token gives access to your Discord account - keep it secure

## Technical Details

- Uses Discord Gateway WebSocket API v10
- Implements proper authentication, heartbeat, and reconnection logic
- Monitors media sessions via Android's MediaSessionManager
- Follows Discord's Rich Presence guidelines
- Integrates with ReVanced Extended settings system

## Limitations

- Requires notification access permission for media session monitoring
- Discord token must be manually obtained (Discord doesn't provide public API for this)
- Discord may rate limit connections if too many requests are made
- Token may expire and need to be refreshed periodically

## Support

If you encounter issues:
1. Check that Discord RPC is enabled in settings
2. Verify your token is correct and hasn't expired
3. Ensure notification access permission is granted
4. Try manual initialization
5. Check app logs with debug logging enabled

Remember: This patch shows your music listening activity on Discord. Make sure you're comfortable with this level of sharing before enabling it.