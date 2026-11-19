/* Tiny JNI helper: lets the launcher chdir() the game process into the player's game folder.
 * The patched libnfsu2_android.so (scripts/patch_game_root.py) opens its data relative to ".". */
#include <jni.h>
#include <unistd.h>

JNIEXPORT jboolean JNICALL Java_io_mininxd_nfsu2_StorageFix_nativeChdir(JNIEnv* env, jclass clazz, jstring path) {
    (void) clazz;
    const char* utf = (*env)->GetStringUTFChars(env, path, NULL);
    if (!utf) return JNI_FALSE;
    int rc = chdir(utf);
    (*env)->ReleaseStringUTFChars(env, path, utf);
    return rc == 0 ? JNI_TRUE : JNI_FALSE;
}
