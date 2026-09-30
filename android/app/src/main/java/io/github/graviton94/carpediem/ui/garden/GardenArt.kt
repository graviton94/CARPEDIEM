package io.github.graviton94.carpediem.ui.garden

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.graviton94.carpediem.core.Season

/** scripts/build_art.js 가 구운 정원 그림 (assets/garden). 한 번 읽으면 메모리에 둔다. */
object GardenArt {
    private val cache = HashMap<String, ImageBitmap>()

    fun image(context: Context, name: String): ImageBitmap = synchronized(cache) {
        cache.getOrPut(name) { context.assets.open("garden/$name").use { BitmapFactory.decodeStream(it).asImageBitmap() } }
    }

    fun key(s: Season) = when (s) { Season.SPRING -> "spring"; Season.SUMMER -> "summer"; Season.AUTUMN -> "autumn"; Season.WINTER -> "winter" }
    fun sky(context: Context, s: Season) = image(context, "sky_${key(s)}.jpg")
    fun strip(context: Context, s: Season) = image(context, "strip_${key(s)}.png")
    fun obj(context: Context, id: String) = image(context, "obj_$id.png")
    fun sun(context: Context) = image(context, "sun.png")
    fun moon(context: Context) = image(context, "moon.png")
    fun sparkle(context: Context) = image(context, "sparkle.png")
    fun toothLine(context: Context) = image(context, "tooth_line.png")
    fun toothFill(context: Context) = image(context, "tooth_fill.png")
    fun paper(context: Context) = image(context, "paper.png")
    fun support(context: Context) = image(context, "support.jpg")
}
