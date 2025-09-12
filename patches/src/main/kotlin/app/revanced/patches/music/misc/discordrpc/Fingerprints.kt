package app.revanced.patches.music.misc.discordrpc

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PlayerStateChangeFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name.contains("onPlaybackStateChanged") ||
        methodDef.name.contains("setPlayWhenReady") ||
        methodDef.name.contains("onPlayerStateChanged")
    }
)

internal object TrackMetadataFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC.value,
    opcodes = listOf(
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name.contains("onMetadataChanged") ||
        methodDef.name.contains("updateMetadata") ||
        methodDef.name.contains("setMediaMetadata")
    }
)

internal object MusicPlayerFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC.value,
    customFingerprint = { methodDef, classDef ->
        classDef.sourceFile?.contains("MusicPlayer") == true ||
        classDef.type.contains("MusicPlayer") ||
        methodDef.name.contains("updateNowPlaying")
    }
)

internal object MediaSessionCallbackFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC.value,
    parameters = listOf("Landroid/support/v4/media/MediaMetadataCompat;"),
    customFingerprint = { methodDef, _ ->
        methodDef.name.contains("onMetadataChanged") ||
        methodDef.name.contains("updateMetadata")
    }
)