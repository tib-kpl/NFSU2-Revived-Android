#pragma once

#include <android/input.h>
#include <jni.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initializes the native gamepad input & camera bridge.
 */
void nfs_gamepad_bridge_init(JavaVM* vm);

/**
 * Handles native motion events (analog sticks, triggers, hats).
 * Evaluates the right analog stick for camera rotation and delivers telemetry to InputLog.
 */
void nfs_gamepad_bridge_on_motion(const AInputEvent* event);

/**
 * Handles native key events (buttons, d-pad presses).
 * Delivers button telemetry to InputLog.
 */
void nfs_gamepad_bridge_on_key(const AInputEvent* event);

/**
 * Feeds right stick coordinates directly to camera handlers.
 */
void nfs_gamepad_bridge_feed_camera(float rx, float ry);

/**
 * Direct registration point for roam_android.c's roam_stick_input.
 */
void nfs_register_roam_stick_handler(void (*handler)(float, float));

#ifdef __cplusplus
}
#endif
