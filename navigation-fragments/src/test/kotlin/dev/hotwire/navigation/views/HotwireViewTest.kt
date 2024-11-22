package dev.hotwire.navigation.views

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout.LayoutParams
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.navigation.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
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

    @Test fun webviewAttachedToRefreshLayout() {
        hotwireView.attachWebView(webView) {
            // Child at 0 is CircleImageView
            assertThat(hotwireView.webViewRefresh?.getChildAt(1)).isEqualTo(webView)
        }
    }
}
