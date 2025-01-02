package dev.hotwire.navigation.observers

import android.webkit.CookieManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.hotwire.navigation.util.dispatcherProvider
import kotlinx.coroutines.launch

internal class HotwireActivityObserver : DefaultLifecycleObserver {
    /**
     * Cookies may not be persisted to storage yet, since WebView
     * maintains its own internal timing to flush in-memory cookies
     * to persistent storage. Ensure that cookies are maintained
     * across app restarts.
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        owner.lifecycleScope.launch(dispatcherProvider.io) {
            persistWebViewCookies()
        }
    }

    private fun persistWebViewCookies() {
        CookieManager.getInstance().flush()
    }
}
