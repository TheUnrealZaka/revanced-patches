# Discord RPC for YouTube Music

This patch adds Discord Rich Presence support to YouTube Music, allowing your Discord status to show what you're currently listening to.

## Features

- **Song Information**: Shows current song title in Discord presence
- **Artist & Album**: Optionally display artist and/or album information
- **Play/Pause Status**: Shows playing or paused state
- **Timestamps**: Optional track progress timestamps
- **Privacy Controls**: Hide presence when paused or disable entirely

## Setup

1. **Get Discord Token**:
   - Go to Discord Developer Portal (https://discord.com/developers/applications)
   - Create a new application or use an existing one
   - Go to "Bot" section and copy the token

2. **Configure Settings**:
   - Open YouTube Music settings
   - Navigate to ReVanced → Discord RPC
   - Enable Discord RPC
   - Enter your Discord token
   - Configure display preferences

## Settings

- **Enable Discord RPC**: Main toggle for the feature
- **Discord Token**: Your Discord bot token for authentication
- **Show Artist**: Include artist name in presence
- **Show Album**: Include album name in presence  
- **Show Timestamps**: Display track progress/duration
- **Hide on Pause**: Clear presence when music is paused

## Privacy & Security

- Your Discord token is stored locally on your device
- Only basic song metadata is sent to Discord
- No personal information or listening history is transmitted
- You can disable the feature at any time

## Troubleshooting

- **Presence not showing**: Verify your Discord token is correct
- **Permission errors**: Ensure the Discord bot has appropriate permissions
- **Connection issues**: Check your internet connection
- **Missing metadata**: Some songs may have limited information available

## Technical Details

This implementation uses Android's MediaSessionManager to detect currently playing music, similar to how other Discord RPC apps work. It communicates with Discord via the official Discord API.

## Credits

Inspired by the [Kizzy](https://github.com/dead8309/Kizzy) Discord RPC implementation for Android.