package app.revanced.patches.music.misc.discordrpc

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.DISCORD_RPC
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch

@Suppress("unused")
val discordRpcPatch = bytecodePatch(
    DISCORD_RPC.title,
    DISCORD_RPC.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        // For now, simply register the patch and create a hook point
        // The actual implementation will be done through the extension system
        
        // Add a basic initialization hook - this will be called when YouTube Music starts
        classes.forEach { classDef ->
            if (classDef.type.endsWith("/MusicApplication;") || 
                classDef.type.endsWith("/YouTubeMusicApplication;")) {
                
                classDef.methods.find { it.name == "onCreate" }?.let { method ->
                    method.addInstructions(
                        0, """
                            # Initialize Discord RPC when app starts
                            invoke-static {}, Lapp/revanced/extension/music/patches/misc/discordrpc/DiscordRpcPatch;->initialize()V
                            """
                    )
                }
            }
        }

        updatePatchStatus(DISCORD_RPC)
    }
}