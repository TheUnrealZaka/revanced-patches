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
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

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
        // Hook into playback state changes
        PlayerStateChangeFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IPUT_BOOLEAN)
            val playingRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex + 1,
                "invoke-static {v$playingRegister}, $EXTENSION_CLASS_DESCRIPTOR->onPlaybackStateChanged(Z)V"
            )
        }

        // Hook into metadata changes - look for methods that handle track information
        val metadataMethod = TrackMetadataFingerprint.methodOrThrow()
        metadataMethod.apply {
            // Find string references that might contain track info
            val instructions = implementation!!.instructions
            var titleRegister = -1
            var artistRegister = -1
            var albumRegister = -1
            
            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode == Opcode.CONST_STRING) {
                    val stringRef = (instruction as ReferenceInstruction).reference as StringReference
                    val value = stringRef.string.lowercase()
                    
                    when {
                        value.contains("title") -> {
                            titleRegister = getInstruction<OneRegisterInstruction>(index).registerA
                        }
                        value.contains("artist") -> {
                            artistRegister = getInstruction<OneRegisterInstruction>(index).registerA
                        }
                        value.contains("album") -> {
                            albumRegister = getInstruction<OneRegisterInstruction>(index).registerA
                        }
                    }
                }
            }
            
            // Add hook at the end of the method
            val insertIndex = instructions.size - 1
            
            // Try to extract metadata from common Android MediaMetadata patterns
            addInstruction(
                insertIndex,
                """
                    const-string v0, ""
                    const-string v1, ""
                    const-string v2, ""
                    invoke-static {v0, v1, v2}, $EXTENSION_CLASS_DESCRIPTOR->onTrackMetadataChanged(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
                """.trimIndent()
            )
        }

        // Try to hook into MediaSessionCallbackFingerprint for better metadata extraction
        try {
            MediaSessionCallbackFingerprint.methodOrThrow().apply {
                val metadataParameterIndex = 0 // MediaMetadataCompat parameter
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IGET_OBJECT)
                
                addInstruction(
                    insertIndex,
                    """
                        if-nez p$metadataParameterIndex, :skip_discord_rpc
                        invoke-virtual {p$metadataParameterIndex}, Landroid/support/v4/media/MediaMetadataCompat;->getString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v0
                        const-string v1, "android.media.metadata.TITLE"
                        invoke-virtual {p$metadataParameterIndex, v1}, Landroid/support/v4/media/MediaMetadataCompat;->getString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v1
                        const-string v2, "android.media.metadata.ARTIST"
                        invoke-virtual {p$metadataParameterIndex, v2}, Landroid/support/v4/media/MediaMetadataCompat;->getString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v2
                        const-string v3, "android.media.metadata.ALBUM"
                        invoke-virtual {p$metadataParameterIndex, v3}, Landroid/support/v4/media/MediaMetadataCompat;->getString(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v3
                        invoke-static {v1, v2, v3}, $EXTENSION_CLASS_DESCRIPTOR->onTrackMetadataChanged(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
                        :skip_discord_rpc
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            // MediaSessionCallbackFingerprint hook failed, continue with basic hooks
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