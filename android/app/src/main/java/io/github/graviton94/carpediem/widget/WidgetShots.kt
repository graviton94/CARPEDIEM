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

    /** 둘 수 있는 위젯 전부: 남은 날 · 오늘의 한 줄 (4×2) · 마음의 기록. */
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
        val dir = File(activity.getExternalFilesDir(null), "widgets").apply { mkdirs() }
        for (s in shots) runCatching {
            val out = render(activity, s)
            File(dir, "widget_preview_${s.name}.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { android.util.Log.e("WidgetShots", s.name, it) }
        File(dir, "done").writeText("ok")
    }
}
