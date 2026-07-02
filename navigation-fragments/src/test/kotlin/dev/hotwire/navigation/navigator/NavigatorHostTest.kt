package dev.hotwire.navigation.navigator

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
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://other.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/start")
    }

    @Test
    fun `does not change start location when deep link host matches config`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/path")
    }

    // NavController merges deepLinkArgs over deepLinkExtras (last write wins); the intent's args
    // must not survive to override the validated start location.
    @Test
    fun `empties deepLinkArgs so they cannot override the start location`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/ok"))
            putParcelableArrayListExtra(DEEPLINK_ARGS_KEY, arrayListOf(bundleOf(LOCATION_KEY to ATTACKER_URL)))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        val survivingArgs = activity.intent.getParcelableArrayListExtra<Bundle>(DEEPLINK_ARGS_KEY)
            ?.mapNotNull { it.getString(LOCATION_KEY) }.orEmpty()
        assertThat(survivingArgs).doesNotContain(ATTACKER_URL)
    }

    companion object {
        private const val ATTACKER_URL = "https://attacker.example/steal"
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