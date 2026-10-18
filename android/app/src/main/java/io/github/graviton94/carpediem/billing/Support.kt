package io.github.graviton94.carpediem.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * 응원하기 결제 (Google Play, 한 번 결제 · 소모성 상품이라 여러 번 응원할 수 있음). 보상 · 잠긴 기능 없음.
 * Play Console 의 상품 (support_tea ₩1,500 · support_coffee ₩4,400 · support_cake ₩9,800, 2026-10 등록) 을 받아 오기 전이거나 Play 에 닿지 않으면 ready = false 로 두고,
 * 화면은 ‘잠시 뒤 다시’ 를 보인다. 기다리는 결제는 pending, 남은 결제는 다음에 열 때 마저 소모. 결제 정보는 앱이 받지 않는다 (Play 가 처리).
 */
class Support(context: Context) {
    companion object { val IDS = listOf("support_tea", "support_coffee", "support_cake") }

    /** 상품 id → Play 가 알려 준 가격 글자 (예: ₩4,400). 비어 있으면 아직 준비 안 됨. */
    var prices by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    /** 방금 응원이 끝났을 때 true (화면이 고마움 한마디를 보이고 다시 false 로). */
    var thanked by mutableStateOf(false)
    /** 응원이 끝난 상품 id (고마움의 흔적을 남기려고). 화면 스레드에서 부름. */
    var onSupported: ((String) -> Unit)? = null
    /** 방금 응원한 상품 id (고마움 한마디에 무엇이 놓였는지). */
    var lastId by mutableStateOf<String?>(null)
    val ready: Boolean get() = prices.isNotEmpty()
    /** 결제는 했지만 아직 확인 전 (편의점 결제 등 기다리는 결제). 확인되면 앱을 열 때 마저 받음. */
    var pending by mutableStateOf(false)
    /** Play 에서 상품을 받아 왔을 때 (화면 스레드). 결제 프로필이 막혀 있으면 오지 않음. */
    var onReady: (() -> Unit)? = null

    private var details: Map<String, ProductDetails> = emptyMap()
    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .setListener { r, list ->
            when (r.responseCode) {
                BillingClient.BillingResponseCode.OK -> list?.forEach(::consume)
                // 예전에 끝나지 못한 같은 응원이 남아 있으면: 마저 받아 쓰고 (소모) 다시 응원할 수 있게
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
                else -> {}   // 취소 · 오류는 조용히 (화면은 그대로)
            }
        }
        .build()

    fun connect() {
        if (client.isReady) { query(); return }
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(r: BillingResult) { if (r.responseCode == BillingClient.BillingResponseCode.OK) query() }
                override fun onBillingServiceDisconnected() {}
            })
        }
    }

    private fun query() {
        val q = QueryProductDetailsParams.newBuilder().setProductList(IDS.map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()
        }).build()
        client.queryProductDetailsAsync(q) { r, result ->
            if (r.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = result.productDetailsList.associateBy { it.productId }
            prices = details.mapValues { it.value.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty() }
            if (details.isNotEmpty()) android.os.Handler(android.os.Looper.getMainLooper()).post { onReady?.invoke() }
        }
        restore()
    }

    /** 앱이 닫혀 있는 동안 끝난 응원 (기다리던 결제 등) 도 마저 받아 씀. */
    private fun restore() {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { r, list ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) list.forEach(::consume)
        }
    }

    /** 응원하기 (상품이 준비되어 있을 때만 true). */
    fun buy(activity: Activity, id: String): Boolean {
        val d = details[id] ?: return false
        val p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()
        return client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(p)).build()).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** 소모성: 받자마자 써서 다음에 또 응원할 수 있게. 보상은 없고 고마움 한마디만. */
    private fun consume(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PENDING) { android.os.Handler(android.os.Looper.getMainLooper()).post { pending = true }; return }
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
            if (r.responseCode != BillingClient.BillingResponseCode.OK) return@consumeAsync
            val id = p.products.firstOrNull()
            android.os.Handler(android.os.Looper.getMainLooper()).post { lastId = id; pending = false; thanked = true; id?.let { onSupported?.invoke(it) } }
        }
    }

    fun close() = runCatching { client.endConnection() }
}
