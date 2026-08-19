package com.y.citycapsule.core.media

enum class ImageLoadPriority { VISIBLE, PREFETCH }

data class ImageLoadMetrics(
    val uniqueRequestsStarted: Long = 0,
    val deduplicatedSubscribers: Long = 0,
    val warmedCacheSubscribers: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val cancelledRequests: Long = 0,
    val activeRequests: Int = 0,
    val queuedRequests: Int = 0
)

fun interface ImageLoadLease {
    fun release()
}

/**
 * Cross-platform admission control in front of the platform image loader.
 * The platform adapter remains responsible for byte caching and size-aware decoding.
 */
class ImageLoadCoordinator(private val maxConcurrentRequests: Int = 3) {
    private data class Subscriber(val id: Long, val onReady: (Boolean) -> Unit)
    private data class Job(
        val url: String,
        var priority: ImageLoadPriority,
        val subscribers: MutableList<Subscriber>,
        var ownerId: Long? = null
    )

    private val queued = mutableListOf<Job>()
    private val active = mutableMapOf<String, Job>()
    private val warmedUrls = mutableListOf<String>()
    private var nextSubscriberId = 1L
    private var started = 0L
    private var deduplicated = 0L
    private var warmedSubscribers = 0L
    private var succeeded = 0L
    private var failed = 0L
    private var cancelled = 0L

    fun acquire(
        url: String,
        priority: ImageLoadPriority,
        onReady: (Boolean) -> Unit
    ): ImageLoadLease {
        val normalized = url.trim()
        if (normalized.isEmpty()) {
            onReady(false)
            return ImageLoadLease {}
        }
        val subscriber = Subscriber(nextSubscriberId++, onReady)
        if (normalized in warmedUrls) {
            warmedSubscribers++
            onReady(true)
            return ImageLoadLease {}
        }
        val existing = active[normalized] ?: queued.firstOrNull { it.url == normalized }
        if (existing != null) {
            existing.subscribers += subscriber
            if (priority == ImageLoadPriority.VISIBLE) existing.priority = priority
            deduplicated++
            sortQueue()
        } else {
            queued += Job(normalized, priority, mutableListOf(subscriber))
            sortQueue()
            drain()
        }
        return ImageLoadLease { release(normalized, subscriber.id) }
    }

    fun complete(url: String, success: Boolean) {
        val job = active.remove(url.trim()) ?: return
        if (success) {
            succeeded++
            warmedUrls.remove(job.url)
            warmedUrls += job.url
            while (warmedUrls.size > MAX_WARMED_URLS) warmedUrls.removeAt(0)
        } else {
            failed++
        }
        job.subscribers.filterNot { it.id == job.ownerId }.forEach { it.onReady(success) }
        drain()
    }

    fun metrics(): ImageLoadMetrics = ImageLoadMetrics(
        uniqueRequestsStarted = started,
        deduplicatedSubscribers = deduplicated,
        warmedCacheSubscribers = warmedSubscribers,
        successfulRequests = succeeded,
        failedRequests = failed,
        cancelledRequests = cancelled,
        activeRequests = active.size,
        queuedRequests = queued.size
    )

    private fun release(url: String, subscriberId: Long) {
        val queuedJob = queued.firstOrNull { it.url == url }
        if (queuedJob != null) {
            queuedJob.subscribers.removeAll { it.id == subscriberId }
            if (queuedJob.subscribers.isEmpty()) queued.remove(queuedJob)
            return
        }
        val activeJob = active[url] ?: return
        activeJob.subscribers.removeAll { it.id == subscriberId }
        if (activeJob.ownerId != subscriberId) return
        val replacement = activeJob.subscribers.firstOrNull()
        if (replacement != null) {
            activeJob.ownerId = replacement.id
            replacement.onReady(true)
        } else {
            active.remove(url)
            cancelled++
            drain()
        }
    }

    private fun drain() {
        while (active.size < maxConcurrentRequests && queued.isNotEmpty()) {
            val job = queued.removeAt(0)
            val owner = job.subscribers.firstOrNull() ?: continue
            job.ownerId = owner.id
            active[job.url] = job
            started++
            owner.onReady(true)
        }
    }

    private fun sortQueue() {
        queued.sortBy { if (it.priority == ImageLoadPriority.VISIBLE) 0 else 1 }
    }

    private companion object {
        const val MAX_WARMED_URLS = 128
    }
}

object PlaceImageLoadRuntime {
    const val INITIAL_VISIBLE_LIMIT = 6
    val coordinator = ImageLoadCoordinator(maxConcurrentRequests = 3)
}
