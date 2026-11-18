#include "gamepad_camera.h"

#include <android/log.h>
#include <dlfcn.h>
#include <cmath>
#include <mutex>
#include <atomic>
#include <algorithm>

#define LOG_TAG "NFSU2-GamepadCamera"

static void (*s_cam_stick_fn)(float, float) = nullptr;
static void (*s_garage_stick_fn)(float, float) = nullptr;
static void (*s_roam_stick_fn)(float, float) = nullptr;
static std::mutex s_dispatch_mutex;
static std::atomic<bool> s_symbols_resolved{false};

static void resolve_camera_symbols() {
    if (s_symbols_resolved.load(std::memory_order_acquire)) return;
    std::lock_guard<std::mutex> lock(s_dispatch_mutex);
    if (s_symbols_resolved.load(std::memory_order_relaxed)) return;

    // 1. Resolve from libgame_core.so / libgraphics_cache.so
    void* h_core = dlopen("libgame_core.so", RTLD_NOLOAD);
    if (!h_core) h_core = dlopen("libgame_core.so", RTLD_NOW | RTLD_GLOBAL);
    if (!h_core) {
        void* h_gfx = dlopen("libgraphics_cache.so", RTLD_NOLOAD);
        if (!h_gfx) h_gfx = dlopen("libgraphics_cache.so", RTLD_NOW | RTLD_GLOBAL);
        if (h_gfx) h_core = h_gfx;
    }

    if (h_core) {
        if (!s_cam_stick_fn)
            s_cam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(h_core, "nfs_camera_stick_input"));
        if (!s_garage_stick_fn)
            s_garage_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(h_core, "nfs_garage_stick_input"));
    }

    if (!s_cam_stick_fn)
        s_cam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(RTLD_DEFAULT, "nfs_camera_stick_input"));
    if (!s_garage_stick_fn)
        s_garage_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(RTLD_DEFAULT, "nfs_garage_stick_input"));

    // 2. Resolve from libnfsu2_roam.so
    void* h_roam = dlopen("libnfsu2_roam.so", RTLD_NOLOAD);
    if (!h_roam) h_roam = dlopen("libnfsu2_roam.so", RTLD_NOW | RTLD_GLOBAL);
    if (h_roam) {
        if (!s_roam_stick_fn)
            s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(h_roam, "roam_stick_input"));
        if (!s_roam_stick_fn)
            s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(h_roam, "nfs_roam_stick_input"));
        if (!s_roam_stick_fn)
            s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(h_roam, "Java_io_mininxd_nfsu2_AdhocRoamClient_nativeRoamStick"));
    }
    if (!s_roam_stick_fn)
        s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(RTLD_DEFAULT, "roam_stick_input"));
    if (!s_roam_stick_fn)
        s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(RTLD_DEFAULT, "nfs_roam_stick_input"));
    if (!s_roam_stick_fn)
        s_roam_stick_fn = reinterpret_cast<void(*)(float, float)>(dlsym(RTLD_DEFAULT, "Java_io_mininxd_nfsu2_AdhocRoamClient_nativeRoamStick"));

    if (s_cam_stick_fn && s_roam_stick_fn) {
        s_symbols_resolved.store(true, std::memory_order_release);
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG,
            "Resolved camera stick handlers: cam=%p, garage=%p, roam=%p",
            (void*)s_cam_stick_fn, (void*)s_garage_stick_fn, (void*)s_roam_stick_fn);
    }
}

void nfs_gamepad_camera_init(void) {
    resolve_camera_symbols();
}

void nfs_gamepad_camera_feed(float rx, float ry) {
    resolve_camera_symbols();

    if (s_cam_stick_fn) {
        s_cam_stick_fn(rx, ry);
    }
    if (s_roam_stick_fn) {
        s_roam_stick_fn(rx, ry);
    }
    if (s_garage_stick_fn) {
        s_garage_stick_fn(rx, ry);
    }
}

void nfs_gamepad_camera_on_motion(const AInputEvent* event) {
    if (!event) return;
    if (AInputEvent_getType(event) != AINPUT_EVENT_TYPE_MOTION) return;

    int32_t src = AInputEvent_getSource(event);
    bool is_gamepad_source = (src & (AINPUT_SOURCE_JOYSTICK | AINPUT_SOURCE_GAMEPAD | AINPUT_SOURCE_CLASS_JOYSTICK)) != 0;
    if (!is_gamepad_source) return;

    // Sample potential right stick axes:
    // Android standard right stick: AXIS_Z (X) and AXIS_RZ (Y)
    // Custom / rotation right stick: AXIS_RX (X) and AXIS_RY (Y)
    float rx_rot = AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_RX, 0);
    float ry_rot = AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_RY, 0);
    float rx_z   = AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_Z, 0);
    float ry_rz  = AMotionEvent_getAxisValue(event, AMOTION_EVENT_AXIS_RZ, 0);

    float chosen_x = 0.0f;
    float chosen_y = 0.0f;

    if (std::fabs(rx_rot) > 0.05f || std::fabs(ry_rot) > 0.05f) {
        chosen_x = rx_rot;
        chosen_y = ry_rot;
    } else if (rx_z > -0.85f && ry_rz > -0.85f) {
        chosen_x = rx_z;
        chosen_y = ry_rz;
    } else {
        chosen_x = 0.0f;
        chosen_y = 0.0f;
    }

    // Apply radial deadzone
    float mag = std::hypot(chosen_x, chosen_y);
    if (mag < 0.15f) {
        chosen_x = 0.0f;
        chosen_y = 0.0f;
    } else {
        chosen_x = std::max(-1.0f, std::min(1.0f, chosen_x));
        chosen_y = std::max(-1.0f, std::min(1.0f, chosen_y));
    }

    nfs_gamepad_camera_feed(chosen_x, chosen_y);
}

extern "C" JNIEXPORT void JNICALL
Java_io_mininxd_nfsu2_GameActivity_nativeFeedGamepadStick(JNIEnv*, jclass, jfloat rx, jfloat ry) {
    nfs_gamepad_camera_feed(rx, ry);
}
