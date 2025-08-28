package dev.hotwire.core.turbo.session

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.BaseRepositoryTest
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.HttpError.ServerError
import dev.hotwire.core.turbo.errors.LoadError
import dev.hotwire.core.turbo.errors.WebError
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.core.turbo.webview.HotwireWebView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
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
        session = Session("test", activity, webView)
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
