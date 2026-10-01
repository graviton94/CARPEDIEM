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

/**
 * 응원하기 결제 (Google Play, 한 번 결제 · 소모성 상품이라 여러 번 응원할 수 있음). 보상 · 잠긴 기능 없음.
 * Play Console 에 상품(support_tea · support_coffee · support_cake)이 아직 없거나 Play 에 닿지 않으면 ready = false 로 두고,
 * 화면은 지금처럼 ‘시험판’ 안내를 보인다. 결제 정보는 앱이 받지 않는다 (Play 가 처리).
 */
class Support(context: Context) {
    companion object { val IDS = listOf("support_tea", "support_coffee", "support_cake") }

    /** 상품 id → Play 가 알려 준 가격 글자 (예: ₩4,400). 비어 있으면 아직 준비 안 됨. */
    var prices by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    /** 방금 응원이 끝났을 때 true (화면이 고마움 한마디를 보이고 다시 false 로). */
    var thanked by mutableStateOf(false)
    val ready: Boolean get() = details.isNotEmpty()

    private var details: Map<String, ProductDetails> = emptyMap()
    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { r, list -> if (r.responseCode == BillingClient.BillingResponseCode.OK) list?.forEach(::consume) }
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
        client.queryProductDetailsAsync(q) { r, list ->
            if (r.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = list.associateBy { it.productId }
            prices = details.mapValues { it.value.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty() }
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
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ -> if (r.responseCode == BillingClient.BillingResponseCode.OK) thanked = true }
    }

    fun close() = runCatching { client.endConnection() }
}
