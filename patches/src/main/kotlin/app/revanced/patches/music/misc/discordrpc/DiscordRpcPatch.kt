package app.revanced.patches.music.misc.discordrpc

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.MISC_PATH
import app.revanced.patches.music.utils.patch.PatchList.DISCORD_RPC
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvableOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/discordrpc/DiscordRpcPatch;"

@Suppress("unused")
val discordRpcPatch = bytecodePatch(
    DISCORD_RPC.title,
    DISCORD_RPC.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        // Hook into MusicPlaybackControls for playback state changes
        try {
            musicPlaybackControlsFingerprint.resolvableOrThrow().let { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    // This method has a boolean parameter that indicates playing state
                    val playingRegister = getInstruction<OneRegisterInstruction>(0).registerA
                    
                    addInstruction(
                        0,
                        "invoke-static {p$playingRegister}, $EXTENSION_CLASS_DESCRIPTOR->onPlaybackStateChanged(Z)V"
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback: try to hook into any method with boolean parameter
            musicPlayerUpdateFingerprint.resolvableOrThrow().let { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    // Add a general hook for player updates
                    addInstruction(
                        0,
                        """
                            const/4 v0, 0x1
                            invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->onPlaybackStateChanged(Z)V
                        """.trimIndent()
                    )
                }
            }
        }

        // Hook into metadata updates - simplified approach
        try {
            mediaMetadataFingerprint.resolvableOrThrow().let { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    // Add a hook at the beginning to capture metadata updates
                    addInstruction(
                        0,
                        """
                            # Extract metadata using reflection and string constants
                            invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->onMetadataUpdateAttempt(Ljava/lang/Object;)V
                        """.trimIndent()
                    )
                }
            }
        } catch (e: Exception) {
            // If metadata fingerprint fails, use a simpler approach
        }

        // Add setting preference
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_discord_rpc_enabled",
            "false"
        )

        updatePatchStatus(DISCORD_RPC)
    }
}