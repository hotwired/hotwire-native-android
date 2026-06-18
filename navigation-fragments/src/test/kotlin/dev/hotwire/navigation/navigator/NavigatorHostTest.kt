package dev.hotwire.navigation.navigator

import android.R.attr.host
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import dev.hotwire.navigation.activities.HotwireActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigatorHostTest {

    private lateinit var activity: TestActivity
    private lateinit var host: NavigatorHost

    @Before
    fun setUp() {
        host = NavigatorHost()
    }

    @Test
    fun `reverts to config start location when deep link host differs`() {
        val extras = bundleOf(LOCATION_KEY to "https://other.com/path")
        val intent = Intent().apply { putExtra(DEEPLINK_EXTRAS_KEY, extras) }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val resultBundle = activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)
        assertThat(resultBundle?.getString(LOCATION_KEY)).isEqualTo("https://example.com/start")
    }

    @Test
    fun `does not change start location when deep link host matches config`() {
        val extras = bundleOf(LOCATION_KEY to "https://example.com/path")
        val intent = Intent().apply { putExtra(DEEPLINK_EXTRAS_KEY, extras) }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val resultBundle = activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)
        assertThat(resultBundle?.getString(LOCATION_KEY)).isEqualTo("https://example.com/path")
    }

    // NavController merges deepLinkArgs over the validated deepLinkExtras, so a location supplied
    // via deepLinkArgs must not survive validation.
    @Test
    fun `neutralizes attacker location smuggled via deepLinkArgs`() {
        val intent = Intent().apply {
            // Benign owned-domain location passes the existing deepLinkExtras host check.
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/ok"))
            // Attacker override that AndroidX would apply last.
            putParcelableArrayListExtra(
                DEEPLINK_ARGS_KEY,
                arrayListOf(bundleOf(LOCATION_KEY to ATTACKER_URL))
            )
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val survivingLocations = activity.intent
            .getParcelableArrayListExtra<Bundle>(DEEPLINK_ARGS_KEY)
            ?.mapNotNull { it.getString(LOCATION_KEY) }
            .orEmpty()
        assertThat(survivingLocations).doesNotContain(ATTACKER_URL)
    }

    // deepLinkArgs can carry arbitrary fragment arguments, not just location — none should survive
    // validation. (A location-only sanitizer would not satisfy this.)
    @Test
    fun `does not leak arbitrary attacker arguments smuggled via deepLinkArgs`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/ok"))
            putParcelableArrayListExtra(
                DEEPLINK_ARGS_KEY,
                arrayListOf(bundleOf(ATTACKER_ARG_KEY to ATTACKER_URL))
            )
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val survivingArgs = activity.intent
            .getParcelableArrayListExtra<Bundle>(DEEPLINK_ARGS_KEY)
            ?.mapNotNull { it.getString(ATTACKER_ARG_KEY) }
            .orEmpty()
        assertThat(survivingArgs).doesNotContain(ATTACKER_URL)
    }

    companion object {
        private const val DEEPLINK_ARGS_KEY = "android-support-nav:controller:deepLinkArgs"
        private const val ATTACKER_URL = "https://attacker.example/steal"
        private const val ATTACKER_ARG_KEY = "key_docs_and_files_api_url"
    }

    private class TestActivity : HotwireActivity() {
        private val navConfig = NavigatorConfiguration(
            name = "test",
            startLocation = "https://example.com/start",
            navigatorHostId = 0
        )

        override fun navigatorConfigurations(): List<NavigatorConfiguration> = listOf(navConfig)
    }
}