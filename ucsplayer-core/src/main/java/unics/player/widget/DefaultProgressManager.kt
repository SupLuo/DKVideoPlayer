package unics.player.widget

import android.util.LruCache

/**
 * 默认进度管理器
 */
internal class DefaultProgressManager(
    private val keyGenerator: ProgressManager.KeyGenerator
) : ProgressManager {

    //保存100条记录
    private val mCache = LruCache<Long, Long?>(100)

    override fun saveProgress(url: String, progress: Long) {
        if (url.isEmpty())
            return
        if (progress == 0L) {
            clear(url)
            return
        }
        mCache.put(keyGenerator.generateKey(url), progress)
    }

    override fun getSavedProgress(url: String): Long {
        return if (url.isEmpty()) 0 else mCache[keyGenerator.generateKey(url)] ?: 0
    }

    override fun clear(url: String) {
        mCache.remove(keyGenerator.generateKey(url))
    }

    override fun clearAll() {
        mCache.evictAll()
    }

}