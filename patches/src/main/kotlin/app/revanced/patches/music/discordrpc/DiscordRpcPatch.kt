package app.revanced.patches.music.discordrpc

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.exception.PatchException
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.playertype.PlayerTypeHookPatch

private const val DISCORD_RPC_CLASS_DESCRIPTOR = "Lapp/revanced/extension/music/discordrpc/DiscordRpcHook;"

@Patch(
    name = "Discord RPC",
    description = "Adds Discord Rich Presence support for YouTube Music. Shows currently playing song, artist, and album on Discord.",
    dependencies = [PlayerTypeHookPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "7.19.53",
                "7.20.51", 
                "7.21.52",
                "7.22.54",
                "7.23.61",
                "7.24.51",
                "7.25.52"
            ]
        )
    ]
)
@Suppress("unused")
object DiscordRpcPatch : BytecodePatch(
    setOf(discordRpcFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        // Add initialization hook to the main activity
        discordRpcFingerprint.result?.let { result ->
            result.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {p0}, $DISCORD_RPC_CLASS_DESCRIPTOR->initializeDiscordRpc(Landroid/content/Context;)V
                    """
                )
            }
        } ?: throw PatchException("discordRpcFingerprint not found")
        
        // The Discord RPC manager will use Android's MediaSessionManager to detect media changes
        // This approach is more reliable than trying to hook specific YouTube Music methods
    }
}