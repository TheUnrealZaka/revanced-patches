package app.revanced.patches.music.misc.discordrpc

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
        // The Discord RPC extension has its own initialization logic
        // that activates when needed, so we just need to register the patch
        // Users can manually trigger initialization through settings if needed
        
        updatePatchStatus(DISCORD_RPC)
    }
}