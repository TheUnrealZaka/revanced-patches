package app.revanced.patches.music.misc.discordrpc

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val playbackStateChangedFingerprint = legacyFingerprint(
    name = "playbackStateChangedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_4,
        Opcode.IF_NE
    ),
    strings = listOf("play", "pause")
)

internal val metadataUpdateFingerprint = legacyFingerprint(
    name = "metadataUpdateFingerprint", 
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("title", "artist")
)