@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.ijk

import unicstar.oknote.OkNote

internal const val TAG = "IJKKernel"

internal inline fun ilogv(creator: () -> String) {
    OkNote.logv(TAG, creator)
}

internal inline fun ilogd(creator: () -> String) {
    OkNote.logd(TAG, creator)
}

internal inline fun ilogi(creator: () -> String) {
    OkNote.logi(TAG, creator)
}

internal inline fun ilogw(creator: () -> String) {
    OkNote.logw(TAG, creator)
}

internal inline fun ilogw(e: Throwable, creator: () -> String) {
    OkNote.logw(TAG, e, creator)
}

internal inline fun iloge(creator: () -> String) {
    OkNote.loge(TAG, creator)
}

internal inline fun iloge(e: Throwable, creator: () -> String) {
    OkNote.loge(TAG, e, creator)
}
