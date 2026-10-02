package io.github.graviton94.carpediem.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.graviton94.carpediem.MainActivity
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.DayPart
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.HaruArt
import io.github.graviton94.carpediem.ui.garden.SkyTime
import io.github.graviton94.carpediem.ui.garden.drawHaru
import io.github.graviton94.carpediem.ui.garden.meadow
import io.github.graviton94.carpediem.ui.garden.nightSky
import java.time.LocalDateTime

/** 숨 바로가기 (C1): 알림 · 위젯 · 타일이 함께 쓰는 ‘지금 때의 숨 1분’ 열기. */
internal fun breathIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra(MainActivity.EXTRA_OPEN, "breath")

/** 지금 때의 숨 이름 (하루를 여는 숨 → 잠시 쉬어가는 호흡 → …). */
internal fun breathName(context: Context, now: LocalDateTime): String = context.getString(when (DayPart.of(now.hour)) {
    DayPart.MORNING -> R.string.breath_part_morning; DayPart.DAY -> R.string.breath_part_day
    DayPart.EVENING -> R.string.breath_part_evening; DayPart.NIGHT -> R.string.breath_part_night
})

/**
 * 숨 바로가기 위젯 (C1): 눈을 감은 하루와 지금 때의 숨 이름. 누르면 고르는 창 없이 숨 1분이 바로 시작.
 * 아침 · 낮 · 저녁 · 밤에 이름만 바뀐다 (한 시간마다 다시 그림, Widgets.updateAll).
 */
class BreathWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = Store(context)
        val now = LocalDateTime.now()
        SkyTime.useCountry(context, store.profile?.countryCode)
        val night = SkyTime.isDark(now)
        val ink = if (night) Tokens.Garden.Year.Colors.plain else Tokens.Garden.Colors.ink
        val ready = store.profile != null
        val label = if (ready) context.getString(R.string.widget_breath_minute, breathName(context, now)) else context.getString(R.string.widget_empty)
        val seed = store.haruSeed
        provideContent {
            val size = LocalSize.current
            val bmp = breathBitmap(context, size.width, size.height, seed, night)
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(if (ready) breathIntent(context) else Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))) {
                Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Column(GlanceModifier.fillMaxWidth().padding(Tokens.Layout.widgetPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = TextStyle(color = ColorProvider(ink, ink), fontSize = Tokens.TypeScale.footnote.size, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
                }
            }
        }
    }
}

class BreathReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = BreathWidget() }

/** 둥근 판에 낮엔 풀빛 · 밤엔 남색, 가운데 아래에 눈을 감은 하루. */
private fun breathBitmap(context: Context, w: Dp, h: Dp, seed: Long, night: Boolean): Bitmap {
    val d = context.resources.displayMetrics.density
    val k = minOf(1f, 900f / maxOf(w.value * d, h.value * d, 1f))
    val bw = (w.value * d * k).toInt().coerceAtLeast(1); val bh = (h.value * d * k).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val r = Tokens.Radius.lg.value * d
    c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, bw.toFloat(), bh.toFloat()), r, r, Path.Direction.CW) })
    val art = HaruArt.of(seed, false)
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c), Size(bw.toFloat(), bh.toFloat())) {
        if (night) nightSky() else meadow()
        // 하루: 판의 짧은 쪽에 맞춰, 바닥은 아래에서 18%
        val hw = minOf(bw * 0.42f, bh * 0.5f); val hk = hw / art.meta.bbox.width
        val ground = bh * 0.86f
        translate(bw / 2f - art.meta.bbox.center.x * hk, ground - art.meta.ground * hk) { drawHaru(art, hk, lid = 1f) }
    }
    return bmp
}

/** 빠른 설정 타일 ‘숨, 쉼’ (C1): 누르면 알림 창이 접히며 숨 1분이 바로. */
class BreathTile : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; label = getString(R.string.breathTile_label); if (Build.VERSION.SDK_INT >= 29) subtitle = getString(R.string.breathTile_sub); updateTile() }
    }

    @Suppress("DEPRECATION")
    override fun onClick() {
        val intent = breathIntent(this)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else startActivityAndCollapse(intent)
    }
}
