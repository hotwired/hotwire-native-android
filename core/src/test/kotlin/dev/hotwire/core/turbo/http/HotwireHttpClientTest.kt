package dev.hotwire.core.turbo.http

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class HotwireHttpClientTest {
    @Test
    fun `shared client registers the cross-origin credential interceptor as a network interceptor`() {
        HotwireHttpClient.reset()

        assertThat(HotwireHttpClient.instance.networkInterceptors)
            .anyMatch { it is CrossOriginCredentialInterceptor }
    }
}
