package io.github.graviton94.carpediem.widget

import android.content.Context
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
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import java.time.LocalDateTime
import kotlin.math.max

/**
 * 하루 위젯 (2×2, 1.1.7): 내 하루가 크게 정원 땅 위에. 글자 없이 그림만, 때를 따라 바뀜 (하늘빛 · 해와 달 · 별, 밤엔 잠든 하루,
 * 오늘 한 줄을 남겼으면 웃음, 무거운 마음이면 살짝 아래를 봄, 생일엔 고깔). 누르면 정원으로.
 * (클래스 이름은 예전 ‘마음의 기록’ 위젯 그대로: 이미 둔 2×2 위젯이 사라지지 않고 하루로 바뀜)
 */
class RecordWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            val d = context.resources.displayMetrics.density
            val bmp = GardenWidgetArt.haru(context, max(1, (size.width.value * d).toInt()), max(1, (size.height.value * d).toInt()), LocalDateTime.now())
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(openGarden(context)))) {
                Image(ImageProvider(bmp), context.getString(io.github.graviton94.carpediem.R.string.widget_record_garden), GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            }
        }
    }
}

class RecordReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = RecordWidget() }
