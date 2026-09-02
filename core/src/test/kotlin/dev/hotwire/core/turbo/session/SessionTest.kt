package dev.hotwire.core.turbo.session

import android.os.Build
import android.webkit.HttpAuthHandler
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.HttpError.ServerError
import dev.hotwire.core.turbo.errors.LoadError
import dev.hotwire.core.turbo.errors.WebError
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitAction
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class SessionTest : BaseRepositoryTest() {
    @Mock
    private lateinit var callback: SessionCallback

    @Mock
    private lateinit var webView: HotwireWebView

    @Mock
    private lateinit var activity: AppCompatActivity
    private lateinit var session: Session
    private lateinit var visit: Visit

    @Before
    override fun setup() {
        super.setup()

        MockitoAnnotations.openMocks(this)

        activity = buildActivity(TurboTestActivity::class.java).get()
        Hotwire.config.clearTrustedLocations()
        Hotwire.config.registerTrustedLocation(baseUrl())
        session = Session("test", activity, webView)
        // Robolectric reports WebMessageListener as unsupported; the channel
        // is considered installed so tests reach the trust gates behind it.
        session.turboSessionChannelInstalled = true
        whenever(webView.url).thenReturn(baseUrl())
        visit = Visit(
            location = baseUrl(),
            destinationIdentifier = 1,
            restoreWithCachedSnapshot = false,
            reload = false,
            callback = callback,
            identifier = "",
            options = VisitOptions()
        )

        val visitDestination = object : VisitDestination {
            override fun isActive() = true
            override fun activityResultLauncher(requestCode: Int) = null
            override fun activityPermissionResultLauncher(requestCode: Int) = null
        }

        whenever(callback.visitDestination()).thenReturn(visitDestination)
    }

    @After
    fun teardownTrustedLocations() {
        Hotwire.config.clearTrustedLocations()
    }

    @Test
    fun `session is always new instance`() {
        val session = Session("test", activity, webView)
        val newSession = Session("test", activity, webView)

        assertThat(session).isNotEqualTo(newSession)
    }

    @Test
    fun `visit proposed to location fires callback`() {
        val options = VisitOptions()
        val newLocation = "${visit.location}/page"

        session.currentVisit = visit
        session.visitProposedToLocation(newLocation, options.toJson())

        verify(callback).visitProposedToLocation(newLocation, options)
    }

    @Test
    fun `turbo session messages from an untrusted origin are dropped`() {
        session.currentVisit = visit

        listOf(
            envelope("visitProposedToLocation", "${visit.location}/page", VisitOptions().toJson()),
            envelope("turboIsReady", true),
            envelope("visitStarted", "12345", true, false, visit.location)
        ).forEach {
            session.onTurboSessionMessage(it, sourceOrigin = "https://evil.attacker.com", isMainFrame = true)
        }

        verify(callback, never()).visitProposedToLocation(any(), any())
        assertThat(session.isReady).isFalse()
        assertThat(session.currentVisit?.identifier).isEmpty()
    }

    @Test
    fun `turbo session messages from a sub frame are dropped`() {
        session.currentVisit = visit

        session.onTurboSessionMessage(
            envelope("visitProposedToLocation", "${visit.location}/page", VisitOptions().toJson()),
            sourceOrigin = baseUrl(),
            isMainFrame = false
        )

        verify(callback, never()).visitProposedToLocation(any(), any())
    }

    @Test
    fun `turbo session messages from a trusted main frame are dispatched`() {
        val options = VisitOptions()
        val newLocation = "${visit.location}/page"
        session.currentVisit = visit

        session.onTurboSessionMessage(
            envelope("visitProposedToLocation", newLocation, options.toJson()),
            sourceOrigin = baseUrl(),
            isMainFrame = true
        )

        verify(callback).visitProposedToLocation(newLocation, options)
    }

    @Test
    fun `malformed turbo session messages are dropped`() {
        session.currentVisit = visit

        listOf(
            "not json",
            """{"args":[]}""",
            envelope("noSuchMethod"),
            envelope("visitProposedToLocation")
        ).forEach {
            session.onTurboSessionMessage(it, sourceOrigin = baseUrl(), isMainFrame = true)
        }

        verify(callback, never()).visitProposedToLocation(any(), any())
    }

    private fun envelope(name: String, vararg args: Any): String {
        return buildJsonObject {
            put("name", name)
            putJsonArray("args") {
                args.forEach {
                    when (it) {
                        is Boolean -> add(it)
                        is Number -> add(it)
                        else -> add(it.toString())
                    }
                }
            }
        }.toString()
    }

    @Test
    fun `cold boot page finished on an untrusted origin surfaces an error and resets`() {
        session.currentVisit = visit
        session.isColdBooting = true

        webViewClient().onPageFinished(webView, "https://evil.attacker.com/page")

        verify(callback).onReceivedError(LoadError.UntrustedOrigin)
        assertThat(session.isColdBooting).isFalse()
        assertThat(session.coldBootVisitIdentifier).isEmpty()
    }

    @Test
    fun `cold boot without the message channel surfaces an unsupported error`() {
        session.turboSessionChannelInstalled = false
        session.currentVisit = visit
        session.isColdBooting = true

        webViewClient().onPageFinished(webView, "${visit.location}/page")

        verify(callback).onReceivedError(LoadError.WebViewNotSupported)
        assertThat(session.isColdBooting).isFalse()
    }

    @Test
    fun `cold boot page finished on a trusted origin does not surface an error`() {
        session.currentVisit = visit
        session.isColdBooting = true

        webViewClient().onPageFinished(webView, "${visit.location}/page")

        verify(callback, never()).onReceivedError(any())
    }

    private fun webViewClient(): WebViewClient {
        val captor = argumentCaptor<WebViewClient>()
        verify(webView).webViewClient = captor.capture()
        return captor.lastValue
    }

    @Test
    fun `http auth challenges from an untrusted host are cancelled`() {
        val handler: HttpAuthHandler = mock()
        session.currentVisit = visit

        webViewClient().onReceivedHttpAuthRequest(webView, handler, "evil.attacker.com", "realm")

        verify(handler).cancel()
        verify(callback, never()).onReceivedHttpAuthRequest(any(), any(), any())
    }

    @Test
    fun `http auth challenges from a trusted host are forwarded before the page commits`() {
        Hotwire.config.registerTrustedLocation("https://37signals.com")
        val handler: HttpAuthHandler = mock()
        session.currentVisit = visit
        whenever(webView.url).thenReturn(null)

        webViewClient().onReceivedHttpAuthRequest(webView, handler, "37signals.com", "realm")

        verify(callback).onReceivedHttpAuthRequest(handler, "37signals.com", "realm")
        verify(handler, never()).cancel()
    }

    @Test
    fun `visit started saves current visit identifier`() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitStarted(
            visitIdentifier = visitIdentifier,
            visitHasCachedSnapshot = true,
            visitIsPageRefresh = false,
            location = visit.location
        )

        assertThat(session.currentVisit?.identifier).isEqualTo(visitIdentifier)
    }

    @Test
    fun `visit failed to load calls adapter`() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboFailedToLoad()

        verify(callback).onReceivedError(LoadError.NotPresent)
    }

    @Test
    fun `visit request failed with status code calls adapter`() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitRequestFailedWithStatusCode(visit.location, visitIdentifier, true, 500)

        verify(callback).requestFailedWithError(
            visitHasCachedSnapshot = true,
            error = ServerError.InternalServerError
        )
    }

    @Test
    fun `visit request failed with non http status code calls adapter for cross origin redirect`() {
        val redirectLocation = "https://example.com/"
        val visitIdentifier = "12345"

        enqueueResponse(
            fileName = "empty-body.json",
            responseCode = 301,
            headers = mapOf("Location" to redirectLocation)
        )

        enqueueResponse(
            fileName = "empty-body.json",
            responseCode = 200
        )

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitRequestFailedWithNonHttpStatusCode(visit.location, visitIdentifier, true)

        verify(callback).visitProposedToCrossOriginRedirect(redirectLocation)
    }

    @Test
    fun `visit request failed with non http status code calls adapter without redirect`() {
        enqueueResponse(
            fileName = "empty-body.json",
            responseCode = 404
        )

        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitRequestFailedWithNonHttpStatusCode(visit.location, visitIdentifier, true)

        verify(callback).requestFailedWithError(
            visitHasCachedSnapshot = true,
            error = HttpError.from(WebError.Unknown.errorCode)
        )
    }

    @Test
    fun `visit request failed with non http status code calls adapter without redirect fails visit`() {
        enqueueResponse(
            fileName = "empty-body.json",
            responseCode = 200
        )

        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitRequestFailedWithNonHttpStatusCode(visit.location, visitIdentifier, true)

        verify(callback).requestFailedWithError(
            visitHasCachedSnapshot = true,
            error = HttpError.from(WebError.Unknown.errorCode)
        )
    }

    @Test
    fun `visit completed calls adapter`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitCompleted(visitIdentifier, restorationIdentifier)

        verify(callback).visitCompleted(false)
    }

    @Test
    fun `visit completed saves restoration identifier`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"
        assertThat(session.restorationIdentifiers.size()).isEqualTo(0)

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitCompleted(visitIdentifier, restorationIdentifier)

        assertThat(session.restorationIdentifiers.size()).isEqualTo(1)
    }

    @Test
    fun `visit form submission started fires callback`() {
        session.currentVisit = visit
        session.formSubmissionStarted(visit.location)

        verify(callback).formSubmissionStarted(visit.location)
    }

    @Test
    fun `visit form submission finished fires callback`() {
        session.currentVisit = visit
        session.formSubmissionFinished(visit.location)

        verify(callback).formSubmissionFinished(visit.location)
    }

    @Test
    fun `page loaded saves restoration identifier`() {
        val restorationIdentifier = "67890"
        assertThat(session.restorationIdentifiers.size()).isEqualTo(0)

        session.currentVisit = visit
        session.pageLoaded(restorationIdentifier)

        assertThat(session.restorationIdentifiers.size()).isEqualTo(1)
    }

    @Test
    fun `pending visit is visited when ready`() {
        session.currentVisit = visit
        session.visitPending = true

        session.turboIsReady(true)
        assertThat(session.visitPending).isFalse()
    }

    @Test
    fun `reset to cold boot`() {
        session.currentVisit = visit
        session.isReady = true
        session.isColdBooting = false
        session.reset()

        assertThat(session.isReady).isFalse()
        assertThat(session.isColdBooting).isFalse()
    }

    @Test
    fun `reset to cold boot clears identifiers`() {
        val visitIdentifier = "12345"
        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.coldBootVisitIdentifier = "0"
        session.reset()

        assertThat(session.coldBootVisitIdentifier).isEmpty()
        assertThat(session.currentVisit?.identifier).isEmpty()
    }

    @Test
    fun `cold boot visit with reload reloads the web view when it is on the visit location`() {
        whenever(webView.url).thenReturn(visit.location)

        session.visit(visit.copy(reload = true))

        verify(webView).reload()
    }

    @Test
    fun `cold boot visit with reload loads the visit location when the web view is on a different location`() {
        whenever(webView.url).thenReturn("${visit.location}/modal")

        session.visit(visit.copy(reload = true))

        verify(webView, never()).reload()
        verify(webView).loadUrl(visit.location)
    }

    @Test
    fun `restore current visit`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.pageLoaded(restorationIdentifier)

        assertThat(session.restoreCurrentVisit(callback)).isTrue()
        verify(callback, times(2)).visitCompleted(false)
        verify(webView, times(1)).restoreCurrentVisit()
    }

    @Test
    fun `restore current visit fails with no restoration identifier`() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)

        assertThat(session.restoreCurrentVisit(callback)).isFalse()
        verify(callback, times(1)).visitCompleted(false)
    }

    @Test
    fun `restore visit with restoration identifier uses restore visit`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"
        val restoreVisit = visit.copy(
            options = VisitOptions(action = VisitAction.RESTORE)
        )

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.pageLoaded(restorationIdentifier)
        session.visit(restoreVisit)

        verify(webView, times(1)).visitLocation(
            location = restoreVisit.location,
            options = restoreVisit.options,
            restorationIdentifier = "67890"
        )
    }

    @Test
    fun `restore visit with no restoration identifier uses advance visit`() {
        val visitIdentifier = "12345"
        val restoreVisit = visit.copy(
            options = VisitOptions(action = VisitAction.RESTORE)
        )

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.visit(restoreVisit)

        verify(webView, times(1)).visitLocation(
            location = restoreVisit.location,
            options = restoreVisit.options.copy(action = VisitAction.ADVANCE),
            restorationIdentifier = ""
        )
    }

    @Test
    fun `advance visit does not use restoration identifier`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"
        val advanceVisit = visit.copy(
            options = VisitOptions(action = VisitAction.ADVANCE)
        )

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.pageLoaded(restorationIdentifier)
        session.visit(advanceVisit)

        verify(webView, times(1)).visitLocation(
            location = advanceVisit.location,
            options = advanceVisit.options,
            restorationIdentifier = ""
        )
    }

    @Test
    fun `replace visit does not use restoration identifier`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"
        val replaceVisit = visit.copy(
            options = VisitOptions(action = VisitAction.REPLACE)
        )

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.pageLoaded(restorationIdentifier)
        session.visit(replaceVisit)

        verify(webView, times(1)).visitLocation(
            location = replaceVisit.location,
            options = replaceVisit.options,
            restorationIdentifier = ""
        )
    }

    @Test
    fun `restore current visit fails with session not ready`() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.pageLoaded(restorationIdentifier)
        session.turboIsReady(false)

        assertThat(session.restoreCurrentVisit(callback)).isFalse()
        verify(callback, never()).visitCompleted(false)
        verify(callback).requestFailedWithError(false, LoadError.NotReady)
    }

    @Test
    fun `webView is not null`() {
        assertThat(session.webView).isNotNull
    }
}

internal class TurboTestActivity : AppCompatActivity()
