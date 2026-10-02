package io.github.graviton94.carpediem.ui.garden

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.core.Tree

/** scripts/build_art.js 가 구운 정원 그림 (assets/garden). 한 번 읽으면 메모리에 둔다. */
object GardenArt {
    private val cache = HashMap<String, ImageBitmap>()

    fun image(context: Context, name: String): ImageBitmap = synchronized(cache) {
        cache.getOrPut(name) { context.assets.open("garden/$name").use { BitmapFactory.decodeStream(it).asImageBitmap() } }
    }

    private val bounds = HashMap<ImageBitmap, androidx.compose.ui.geometry.Rect>()
    /** 그림이 실제로 있는 범위 (0 … 1, 상자 대비). 누르는 자리를 그림에만 두려고. 한 번 재면 기억. */
    fun opaque(img: ImageBitmap): androidx.compose.ui.geometry.Rect = synchronized(bounds) {
        bounds.getOrPut(img) {
            val b = img.asAndroidBitmap(); val w = b.width; val h = b.height; val row = IntArray(w)
            var l = w; var t = h; var r = -1; var btm = -1
            for (y in 0 until h step 2) { b.getPixels(row, 0, w, 0, y, w, 1); for (x in 0 until w step 2) if ((row[x] ushr 24) > 40) { if (x < l) l = x; if (x > r) r = x; if (y < t) t = y; if (y > btm) btm = y } }
            if (r < 0) androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f) else androidx.compose.ui.geometry.Rect(l / w.toFloat(), t / h.toFloat(), (r + 1) / w.toFloat(), (btm + 1) / h.toFloat())
        }
    }

    /** 모두 읽어 두었는지 (정원은 다 읽은 뒤에 꾸밈을 그림: 그림 여러 장을 한꺼번에 화면 스레드에서 읽으면 멈춘 듯 보임). */
    fun loaded(names: List<String>) = synchronized(cache) { names.all { it in cache } }
    /** 지금 꾸밈에 필요한 그림 이름 (미리 읽기용). */
    fun decorNames(d: io.github.graviton94.carpediem.core.Decor): List<String> = buildList {
        add("tree_${d.tree.key}_${key(d.season)}_${d.stage.coerceIn(0, 3)}.webp"); d.prevTree?.let { add("tree_${it.key}_${key(d.season)}_${d.stage.coerceIn(0, 3)}.webp") }
        add("post_${key(d.season)}.webp"); listOf("chime", "bell", "lantern", "lantern_lit").take(d.hang.ordinal + if (d.hang == io.github.graviton94.carpediem.core.Hang.LANTERN) 1 else 0).forEach { add("post_$it.webp") }
        if (d.letter) add("post_letter.webp"); if (d.kite) { add("kite.webp"); add("kite_folded.webp") }
        add("card_${d.card.key}.webp"); add("moss_${key(d.season)}_${d.buds.coerceIn(0, 5)}.webp"); add("fiber.png")
    }

    fun key(s: Season) = when (s) { Season.SPRING -> "spring"; Season.SUMMER -> "summer"; Season.AUTUMN -> "autumn"; Season.WINTER -> "winter" }
    fun sky(context: Context, s: Season) = image(context, "sky_${key(s)}.jpg")
    fun strip(context: Context, s: Season) = image(context, "strip_${key(s)}.webp")
    fun obj(context: Context, id: String) = image(context, "obj_$id.png")
    fun sun(context: Context) = image(context, "sun.png")
    fun moon(context: Context) = image(context, "moon.png")
    fun sparkle(context: Context) = image(context, "sparkle.png")
    fun toothLine(context: Context) = image(context, "tooth_line.png")
    fun toothFill(context: Context) = image(context, "tooth_fill.png")
    fun paper(context: Context) = image(context, "paper.png")
    fun support(context: Context) = image(context, "support.jpg")
    fun fiber(context: Context) = image(context, "fiber.png")

    // 한지 정원의 자리 (design/art/src/hanji_export.js, 상자 · 기준점 = 토큰 garden.decor)
    fun tree(context: Context, t: Tree, s: Season, stage: Int) = image(context, "tree_${t.key}_${key(s)}_${stage.coerceIn(0, 3)}.webp")
    fun post(context: Context, s: Season) = image(context, "post_${key(s)}.webp")
    /** chime · bell · lantern · lantern_lit · letter */
    fun postPart(context: Context, part: String) = image(context, "post_$part.webp")
    fun moss(context: Context, s: Season, buds: Int) = image(context, "moss_${key(s)}_${buds.coerceIn(0, 5)}.webp")
    fun kite(context: Context) = image(context, "kite.webp")
    fun card(context: Context, key: String) = image(context, "card_$key.webp")
}
