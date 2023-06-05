@file:JvmMultifileClass
@file:JvmName("UCSPlayerInternalKt")

package unics.player.internal

import unicstar.oknote.OkNote

@PublishedApi
internal const val _TAG = "UCSPlayer"

inline fun plogv(message: String) {
    OkNote.v(_TAG, message)
}

inline fun plogd(message: String) {
    OkNote.d(_TAG, message)
}

inline fun plogi(message: String) {
    OkNote.i(_TAG, message)
}

inline fun plogw(message: String) {
    OkNote.w(_TAG, message)
}

inline fun plogw(e: Throwable, message: String) {
    OkNote.w(_TAG, message, e)
}

inline fun ploge(message: String) {
    OkNote.e(_TAG, message)
}

inline fun ploge(e: Throwable, message: String) {
    OkNote.e(_TAG, message, e)
}

inline fun plogv(creator: () -> String) {
    OkNote.logv(_TAG, creator)
}

inline fun plogd(creator: () -> String) {
    OkNote.logd(_TAG, creator)
}

inline fun plogi(creator: () -> String) {
    OkNote.logi(_TAG, creator)
}

inline fun plogw(creator: () -> String) {
    OkNote.logw(_TAG, creator)
}

inline fun plogw(e: Throwable, creator: () -> String) {
    OkNote.logw(_TAG, e, creator)
}

inline fun ploge(creator: () -> String) {
    OkNote.loge(_TAG, creator)
}

inline fun ploge(e: Throwable, creator: () -> String) {
    OkNote.loge(_TAG, e, creator)
}

inline fun plogv2(subTag: String, creator: () -> String) {
    OkNote.logv(_TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogd2(subTag: String, creator: () -> String) {
    OkNote.logd(_TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogi2(subTag: String, creator: () -> String) {
    OkNote.logi(_TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogw2(subTag: String, creator: () -> String) {
    OkNote.logw(_TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun plogw2(subTag: String, e: Throwable, creator: () -> String) {
    OkNote.logw(_TAG, e) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun ploge2(subTag: String, creator: () -> String) {
    OkNote.loge(_TAG) {
        "$subTag :${creator.invoke()}"
    }
}

inline fun ploge2(subTag: String, e: Throwable, creator: () -> String) {
    OkNote.loge(_TAG, e) {
        "$subTag :${creator.invoke()}"
    }
}
