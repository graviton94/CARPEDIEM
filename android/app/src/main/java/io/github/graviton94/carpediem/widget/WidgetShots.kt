package io.github.graviton94.carpediem.widget

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import java.io.File

/**
 * 디버그 빌드 전용: 정원 위젯 둘을 실제 위젯 그대로 그린다.
 * cd.widgetShots 는 파일로 남기고 (화면 캡처 CI 가 꺼내 res/drawable-nodpi/widget_preview_*.png 로), 미리보기 화면은 앱 안에 늘어놓는다.
 */
object WidgetShots {
    class Shot(val name: String, val widget: GlanceAppWidget, val w: Int, val h: Int)

    /** 둘 수 있는 위젯 전부: 정원 (4×2) · 하루 (2×2). */
    val shots get() = listOf(
        Shot("line_garden", LineGardenWidget(), 320, 160), Shot("record_garden", RecordWidget(), 160, 160),
    )

    /** 위젯 하나를 홈 화면처럼 둥근 모서리로 그린 그림. */
    @OptIn(androidx.glance.ExperimentalGlanceApi::class)
    suspend fun render(context: Context, s: Shot): Bitmap {
        val d = context.resources.displayMetrics.density
        val rv = s.widget.compose(context, size = DpSize(s.w.dp, s.h.dp))
        val wPx = (s.w * d).toInt(); val hPx = (s.h * d).toInt()
        val host = FrameLayout(context)
        val v = rv.apply(context, host)
        host.addView(v, FrameLayout.LayoutParams(wPx, hPx))
        host.measure(View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, wPx, hPx)
        val raw = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(raw))
        val out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(out); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = 20 * d
        c.drawRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), r, r, p)
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        c.drawBitmap(raw, 0f, 0f, p)
        raw.recycle()
        return out
    }

    suspend fun save(activity: Activity) {
        var recapVideo: (() -> Pair<io.github.graviton94.carpediem.share.RecapCards.Art, List<io.github.graviton94.carpediem.core.Recap.Pick>>)? = null
        var recapYear: (() -> Pair<io.github.graviton94.carpediem.share.RecapCards.Art, List<io.github.graviton94.carpediem.core.Recap.Pick>>)? = null
        val dir = File(activity.getExternalFilesDir(null), "widgets").apply { mkdirs() }
        for (s in shots) runCatching {
            val out = render(activity, s)
            File(dir, "widget_preview_${s.name}.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { android.util.Log.e("WidgetShots", s.name, it) }
        // 때를 따라 바뀌는 그림 (1.1.7): 아침 · 낮 · 해 질 녘 · 늦은 밤의 정원 · 하루를 같은 날로 (확인용)
        val d = activity.resources.displayMetrics.density
        val day = java.time.LocalDate.now()
        for ((name, h, m) in listOf(Triple("0600", 6, 0), Triple("0700", 7, 0), Triple("1200", 12, 0), Triple("1730", 17, 30), Triple("1930", 19, 30), Triple("2300", 23, 0))) runCatching {
            val t = day.atTime(h, m)
            GardenWidgetArt.garden(activity, (320 * d).toInt(), (160 * d).toInt(), t).let { b -> File(dir, "time_garden_$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            GardenWidgetArt.haru(activity, (160 * d).toInt(), (160 * d).toInt(), t).let { b -> File(dir, "time_haru_$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }.onFailure { android.util.Log.e("WidgetShots", "time $name", it) }
        // 지난 하루 카드 (1.1.11): 사진 · 질문 · 보낸 사람이 있는 날, 글만 있는 날 (확인용)
        runCatching {
            val sz = io.github.graviton94.carpediem.data.Photos.SIZE
            val src = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
            Canvas(src).apply {
                drawRect(0f, 0f, sz.toFloat(), sz.toFloat(), Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 0f, sz.toFloat(), intArrayOf(0xFF3D7BD9.toInt(), 0xFFFF9A3C.toInt(), 0xFFE2483A.toInt()), floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP) })
                drawCircle(sz * 0.7f, sz * 0.46f, sz * 0.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFE27A.toInt() })
                drawRect(0f, sz * 0.75f, sz.toFloat(), sz.toFloat(), Paint().apply { color = 0xFF1E3B22.toInt() })
            }
            val photo = io.github.graviton94.carpediem.data.Photos.render(src, 20300, 2)
            val mom = io.github.graviton94.carpediem.core.Person("mom", "엄마", io.github.graviton94.carpediem.core.Kind.PERSON, seed = 4242L, metOn = day.minusYears(20))
            val withPhoto = io.github.graviton94.carpediem.core.DayLine(day.minusDays(40), "퇴근길에 본 노을이 엄마가 좋아하던 색이라 사진으로 남겼어요.", io.github.graviton94.carpediem.core.Feeling.THANKS, to = "mom")
            val plain = io.github.graviton94.carpediem.core.DayLine(day.minusDays(12), "오늘은 아무 일도 없었지만, 그래서 좋았다.", io.github.graviton94.carpediem.core.Feeling.CALM)
            val q = "오늘 가장 오래 바라본 것은?"
            val seed = 2718281L
            // 그 달의 정원 견본: 지난 달에 한 줄 열닷새 (마음 고루)
            val month = withPhoto.date.withDayOfMonth(1)
            val fs = io.github.graviton94.carpediem.core.Feeling.entries
            val sample = (1..month.lengthOfMonth()).filter { it % 2 == 0 || it == withPhoto.date.dayOfMonth }.map { d -> io.github.graviton94.carpediem.core.DayLine(month.withDayOfMonth(d), "·", fs[d % fs.size]) }
                .map { if (it.date == withPhoto.date) withPhoto else it }
            val book = io.github.graviton94.carpediem.data.Store(activity).constellations
            val days = io.github.graviton94.carpediem.core.Constellations.monthDays(sample, month.year, month.monthValue)
            val title = "${month.monthValue}월의 정원"; val sub = "한 줄 ${sample.size}"
            io.github.graviton94.carpediem.share.ShareCards.day(activity, book, days, seed, false, day, title, sub, withPhoto, io.github.graviton94.carpediem.share.ShareCards.feelingName(activity, withPhoto.feeling), q, photo, seed, mom)
                .let { b -> File(dir, "share_day_photo.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            io.github.graviton94.carpediem.share.ShareCards.day(activity, book, days, seed, true, day, title, sub, plain.copy(date = month.withDayOfMonth(12)), io.github.graviton94.carpediem.share.ShareCards.feelingName(activity, plain.feeling), null, null, seed, null)
                .let { b -> File(dir, "share_day_text.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            // 달 · 해 카드 (1.1.12): 사진 있는 달 · 사진 없는 달 (대표 한 줄) · 해, 그리고 달 영상 몇 장면 (확인용)
            val Rc = io.github.graviton94.carpediem.core.Recap
            val special: (java.time.LocalDate) -> String? = { if (it == withPhoto.date) "엄마 생신" else null }
            val all = Rc.days(sample, month, month.plusMonths(1).minusDays(1), day, special) { it == withPhoto.date }.map { if (it.date == withPhoto.date) it.copy(line = withPhoto) else it }
            val label: (io.github.graviton94.carpediem.core.Recap.Pick) -> String = { pk -> listOfNotNull(pk.special, "${pk.date.monthValue}월 ${pk.date.dayOfMonth}일", pk.line.feeling?.let { io.github.graviton94.carpediem.ui.Labels.feeling(activity, it) }).joinToString(" · ") }
            fun art(picks: List<io.github.graviton94.carpediem.core.Recap.Pick>, night: Boolean, photos: Map<java.time.LocalDate, Bitmap>, year: Boolean = false) = io.github.graviton94.carpediem.share.RecapCards.Art(
                activity, month.year, if (year) null else month.monthValue, if (year) "${month.year}년" else "${month.monthValue}월", if (year) "${month.year}년의 정원" else title,
                if (year) "한 줄 214개 · 사진 18 · 가장 많던 마음 · 고마움" else "거문고자리 · 한 줄 ${sample.size}개 · 사진 1",
                if (year) io.github.graviton94.carpediem.core.Lines.yearDays(sample.map { it.copy(date = it.date.withMonth(1 + (it.date.dayOfMonth % 12))) } + sample, month.year) else days,
                book, seed, seed, night, day, io.github.graviton94.carpediem.core.Feeling.THANKS, "고마움", picks, photos,
                if (year) mapOf(withPhoto.date.monthValue to "엄마 생신", 5 to "첫 출근") else emptyMap(), label)
            val picks = Rc.month(all)
            // 사진 셋 · 둘 (겹쳐 놓기 확인)
            val three = picks.map { it.copy(photo = true) }
            io.github.graviton94.carpediem.share.RecapCards.month(art(three, false, three.associate { it.date to photo }))
                .let { b -> File(dir, "share_month_photo.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            io.github.graviton94.carpediem.share.RecapCards.month(art(three.take(2), true, three.associate { it.date to photo }))
                .let { b -> File(dir, "share_month_photo2.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            io.github.graviton94.carpediem.share.RecapCards.month(art(picks.map { it.copy(photo = false) }, true, emptyMap()))
                .let { b -> File(dir, "share_month_text.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            io.github.graviton94.carpediem.share.RecapCards.year(art(Rc.yearLines(all), false, emptyMap(), year = true))
                .let { b -> File(dir, "share_year.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            recapVideo = { art(picks, false, mapOf(withPhoto.date to photo)) to picks }
            recapYear = { art(Rc.year(all), false, mapOf(withPhoto.date to photo), year = true) to Rc.year(all) }
        }.onFailure { android.util.Log.e("WidgetShots", "share day", it) }
        File(dir, "done").writeText("ok")
        // 달 영상 (1.1.12): 실제로 만들어 몇 장면을 그림으로 · 길이 · 소리 트랙 (확인용, 끝나면 video_done)
        runCatching {
            val (a, picks) = recapVideo?.invoke() ?: return@runCatching
            val f = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                io.github.graviton94.carpediem.share.RecapVideo.make(a, io.github.graviton94.carpediem.share.RecapCards.month(a), io.github.graviton94.carpediem.share.RecapVideo.scenes(true, picks), "shot-month") {}
            }
            val mmr = android.media.MediaMetadataRetriever(); mmr.setDataSource(f.path)
            val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val audio = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            for (sec in listOf(1, 6, 9, 14, 19, 25, 28)) mmr.getFrameAtTime(sec * 1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { b -> File(dir, "video_month_%02d.png".format(sec)).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            mmr.release()
            File(dir, "video_month.txt").writeText("duration_ms=$dur audio=$audio bytes=${f.length()}\n")
        }.onFailure { android.util.Log.e("WidgetShots", "recap video", it); File(dir, "video_month.txt").writeText("error: $it\n${it.stackTraceToString().take(2000)}") }
        // 해 영상: 열두 달이 차오르는 장면 확인 (앞 몇 초만 그림으로)
        runCatching {
            val (a, picks) = recapYear?.invoke() ?: return@runCatching
            val f = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                io.github.graviton94.carpediem.share.RecapVideo.make(a, io.github.graviton94.carpediem.share.RecapCards.year(a), io.github.graviton94.carpediem.share.RecapVideo.scenes(false, picks), "shot-year") {}
            }
            val mmr = android.media.MediaMetadataRetriever(); mmr.setDataSource(f.path)
            for (sec in listOf(2, 7, 11, 14)) mmr.getFrameAtTime(sec * 1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { b -> File(dir, "video_year_%02d.png".format(sec)).outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            File(dir, "video_year.txt").writeText("duration_ms=${mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)} bytes=${f.length()}\n")
            mmr.release()
        }.onFailure { android.util.Log.e("WidgetShots", "recap year", it); File(dir, "video_year.txt").writeText("error: $it\n${it.stackTraceToString().take(2000)}") }
        File(dir, "video_done").writeText("ok")
    }
}
