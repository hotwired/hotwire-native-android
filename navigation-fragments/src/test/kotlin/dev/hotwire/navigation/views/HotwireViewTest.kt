package dev.hotwire.navigation.views

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout.LayoutParams
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.navigation.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HotwireViewTest {
    @Mock private lateinit var webView: WebView
    private lateinit var context: Context
    private lateinit var view: ViewGroup
    private lateinit var hotwireView: HotwireView

    @Before fun setup() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()
        view = LayoutInflater.from(context).inflate(R.layout.hotwire_view, null) as ViewGroup
        hotwireView = view.findViewById(R.id.hotwire_view)

        whenever(webView.layoutParams).thenReturn(
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    @Test fun refreshLayoutIsFirstChild() {
        assertThat(hotwireView.getChildAt(0) is HotwireSwipeRefreshLayout).isTrue()
    }

    @Test fun webviewAttachedToContainerInsideRefreshLayout() {
        val refreshLayout = hotwireView.webViewRefresh as HotwireSwipeRefreshLayout

        // Child at 0 is CircleImageView
        assertThat(refreshLayout.getChildAt(1)).isEqualTo(refreshLayout.webViewContainer)

        hotwireView.attachWebView(webView) {
            assertThat(refreshLayout.webViewContainer.getChildAt(0)).isEqualTo(webView)
        }
    }

    @Test fun refreshLayoutChecksWebViewInContainer() {
        val refreshLayout = HotwireSwipeRefreshLayout(context)
        val hotwireWebView = HotwireWebView(context, null)
        refreshLayout.webViewContainer.addView(hotwireWebView)

        assertThat(refreshLayout.canChildScrollUp()).isFalse()

        hotwireWebView.scrollY = 10
        assertThat(refreshLayout.canChildScrollUp()).isTrue()
    }

    // SwipeRefreshLayout only lays out the first child it finds, so a new WebView instance
    // must still be laid out after the previous one is detached
    @Test fun refreshLayoutLaysOutEachNewWebView() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val activityView = LayoutInflater.from(activity).inflate(R.layout.hotwire_view, null)
        val activityHotwireView = activityView.findViewById<HotwireView>(R.id.hotwire_view)
        val refreshLayout = activityHotwireView.webViewRefresh as HotwireSwipeRefreshLayout
        activity.setContentView(activityView)

        listOf(HotwireWebView(activity, null), HotwireWebView(activity, null)).forEach { hotwireWebView ->
            activityHotwireView.attachWebView(hotwireWebView) {}
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(hotwireWebView.parent).isEqualTo(refreshLayout.webViewContainer)

            refreshLayout.measure(
                MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(1920, MeasureSpec.EXACTLY)
            )
            refreshLayout.layout(0, 0, 1080, 1920)
            assertThat(hotwireWebView.measuredWidth).isEqualTo(1080)

            activityHotwireView.detachWebView(hotwireWebView) {}
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
