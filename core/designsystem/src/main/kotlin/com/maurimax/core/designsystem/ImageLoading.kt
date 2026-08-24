package com.maurimax.core.designsystem

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * The app's image loader.
 *
 * Two settings do the real work. A disk cache means a channel logo is fetched
 * once and is instant on every later launch, rather than re-downloaded each
 * time the catalogue opens. And cache headers are deliberately ignored: panels
 * commonly serve logos with no-cache or no validators at all, which defeats
 * caching entirely — the artwork is static, so honouring that would mean
 * re-fetching hundreds of unchanged images on every open.
 */
fun maurimaxImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("artwork"))
                .maxSizeBytes(96L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .build()
