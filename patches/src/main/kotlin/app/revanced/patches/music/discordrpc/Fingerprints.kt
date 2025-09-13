package app.revanced.patches.music.discordrpc

import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * Fingerprint for Discord RPC initialization point - YouTube Music's main activity onCreate
 */
internal val discordRpcFingerprint = legacyFingerprint(
    name = "discordRpcFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && classDef.type.contains("MusicActivity")
    }
)