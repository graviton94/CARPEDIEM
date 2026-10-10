package io.github.graviton94.carpediem.ui

import android.app.Activity
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.graviton94.carpediem.R

/**
 * 기록 전체가 파일로 나가거나 바뀌는 순간 (백업 · 불러오기) 에만 폰의 화면 잠금 (지문 · 얼굴 · PIN) 을 한 번 확인한다.
 * 앱을 열고 쓰는 동안은 묻지 않는다. 폰에 잠금이 없으면 그대로 진행.
 * 돌려받은 함수에 할 일을 넘기면, 확인을 마친 뒤에만 그 일을 한다.
 */
@Composable
fun rememberDeviceCheck(): (() -> Unit) -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val p = pending; pending = null
        if (r.resultCode == Activity.RESULT_OK) p?.invoke()
    }
    return { action ->
        val km = ctx.getSystemService(KeyguardManager::class.java)
        @Suppress("DEPRECATION")
        val intent = if (km?.isDeviceSecure == true) km.createConfirmDeviceCredentialIntent(ctx.getString(R.string.lock_title), ctx.getString(R.string.lock_body)) else null
        if (intent == null) action() else { pending = action; launcher.launch(intent) }
    }
}
