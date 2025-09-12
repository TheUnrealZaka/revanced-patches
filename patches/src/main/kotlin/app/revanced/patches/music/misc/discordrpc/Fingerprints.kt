package app.revanced.patches.music.misc.discordrpc

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val musicPlaybackControlsFingerprint = legacyFingerprint(
    name = "musicPlaybackControlsDiscordRpc",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControls;")
    }
)

internal val musicPlayerUpdateFingerprint = legacyFingerprint(
    name = "musicPlayerUpdateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("w_st"),
    customFingerprint = { method, _ ->
        method.name == "a" && method.definingClass.contains("Player")
    }
)

internal val mediaMetadataFingerprint = legacyFingerprint(
    name = "mediaMetadataFingerprint", 
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { method, classDef ->
        method.parameterTypes.firstOrNull()?.contains("MediaMetadata") == true ||
        classDef.type.contains("MediaSession") ||
        method.name.contains("onMetadataChanged")
    }
)