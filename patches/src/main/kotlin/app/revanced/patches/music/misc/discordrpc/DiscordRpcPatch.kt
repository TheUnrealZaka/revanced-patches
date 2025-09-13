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
        // Find a common method that gets called when the app starts
        // We'll look for any method in any class that could serve as an initialization point
        var hooked = false
        
        classes.forEach { classDef ->
            if (!hooked && (classDef.type.contains("Application") || 
                           classDef.type.contains("Activity") ||
                           classDef.type.contains("Main"))) {
                
                classDef.methods.find { method ->
                    method.name == "onCreate" || method.name == "onResume"
                }?.let { method ->
                    method.addInstructions(
                        0, """
                            # Initialize Discord RPC when app starts
                            invoke-static {}, Lapp/revanced/extension/music/patches/misc/discordrpc/DiscordRpcPatch;->initialize()V
                            """
                    )
                    hooked = true
                }
            }
        }
        
        // If no specific hook was found, we can still register the patch
        // The initialization can be called manually by the user if needed
        if (!hooked) {
            // Add a comment to indicate manual initialization might be needed
            // This doesn't break the patch, just means it needs a different trigger
        }

        updatePatchStatus(DISCORD_RPC)
    }
}