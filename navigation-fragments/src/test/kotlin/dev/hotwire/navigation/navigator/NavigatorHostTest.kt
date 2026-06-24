package dev.hotwire.navigation.navigator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.os.bundleOf
import dev.hotwire.navigation.activities.HotwireActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class NavigatorHostTest {

    private lateinit var activity: TestActivity
    private lateinit var host: NavigatorHost

    @Before
    fun setUp() {
        host = NavigatorHost()
    }

    @Test
    fun `does not modify a trusted self-originated intent`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://other.com/path"))
            putParcelableArrayListExtra(DEEPLINK_ARGS_KEY, arrayListOf(bundleOf(LOCATION_KEY to ATTACKER_URL)))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()
        Shadows.shadowOf(activity).setCallingPackage(activity.packageName)

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        // Trusted intents pass through untouched — neither the off-host location nor the args change.
        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://other.com/path")
        val args = activity.intent.getParcelableArrayListExtra<Bundle>(DEEPLINK_ARGS_KEY)
            ?.mapNotNull { it.getString(LOCATION_KEY) }.orEmpty()
        assertThat(args).contains(ATTACKER_URL)
    }

    @Test
    fun `reverts off-host start location for an untrusted intent`() {
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
    fun `keeps same-host start location for an untrusted intent`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://example.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/path")
    }

    // NavController merges deepLinkArgs over deepLinkExtras (last write wins); an untrusted intent's
    // args must not survive to override the validated start location.
    @Test
    fun `empties deepLinkArgs for an untrusted intent`() {
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

    // EXTRA_REFERRER is attacker-settable, so an intent carrying one is not trusted even when it
    // names our own package — it still gets sanitized.
    @Test
    fun `treats a spoofed-referrer intent as untrusted`() {
        val intent = Intent().apply {
            putExtra(DEEPLINK_EXTRAS_KEY, bundleOf(LOCATION_KEY to "https://other.com/path"))
        }
        activity = Robolectric.buildActivity(TestActivity::class.java, intent).get()
        activity.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${activity.packageName}"))

        host.activity = activity
        host.ensureDeeplinkStartLocationValid()

        assertThat(activity.intent.getBundleExtra(DEEPLINK_EXTRAS_KEY)?.getString(LOCATION_KEY))
            .isEqualTo("https://example.com/start")
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