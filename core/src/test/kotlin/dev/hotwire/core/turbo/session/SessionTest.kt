package dev.hotwire.core.turbo.session

import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.whenever
import dev.hotwire.core.turbo.errors.HttpError.ServerError
import dev.hotwire.core.turbo.errors.LoadError
import dev.hotwire.core.turbo.util.toJson
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.core.turbo.visit.Visit
import dev.hotwire.core.turbo.visit.VisitDestination
import dev.hotwire.core.turbo.visit.VisitOptions
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class SessionTest {
    @Mock
    private lateinit var callback: SessionCallback
    @Mock
    private lateinit var webView: HotwireWebView
    @Mock
    private lateinit var activity: AppCompatActivity
    private lateinit var session: Session
    private lateinit var visit: Visit

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        activity = buildActivity(TurboTestActivity::class.java).get()
        session = Session("test", activity, webView)
        visit = Visit(
            location = "https://turbo.hotwired.dev",
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
    fun getNewIsAlwaysNewInstance() {
        val session = Session("test", activity, webView)
        val newSession = Session("test", activity, webView)

        assertThat(session).isNotEqualTo(newSession)
    }

    @Test
    fun visitProposedToLocationFiresCallback() {
        val options = VisitOptions()
        val newLocation = "${visit.location}/page"

        session.currentVisit = visit
        session.visitProposedToLocation(newLocation, options.toJson())

        verify(callback).visitProposedToLocation(newLocation, options)
    }

    @Test
    fun visitProposedToCrossOriginRedirectFiresCallback() {
        val location = "${visit.location}/page"
        val redirectLocation = "https://example.com/page"

        session.currentVisit = visit
        session.visitProposedToCrossOriginRedirect(location, redirectLocation, visit.identifier)

        verify(callback).visitProposedToCrossOriginRedirect(redirectLocation)
    }

    @Test
    fun visitStartedSavesCurrentVisitIdentifier() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitStarted(
            visitIdentifier = visitIdentifier,
            visitHasCachedSnapshot = true,
            visitIsPageRefresh = false,
            location = "https://turbo.hotwired.dev"
        )

        assertThat(session.currentVisit?.identifier).isEqualTo(visitIdentifier)
    }

    @Test
    fun visitFailedToLoadCallsAdapter() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboFailedToLoad()

        verify(callback).onReceivedError(LoadError.NotPresent)
    }

    @Test
    fun visitRequestFailedWithStatusCodeCallsAdapter() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitRequestFailedWithStatusCode(visit.location, visitIdentifier, true, 500)

        verify(callback).requestFailedWithError(
            visitHasCachedSnapshot =  true,
            error = ServerError.InternalServerError
        )
    }

    @Test
    fun visitCompletedCallsAdapter() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitCompleted(visitIdentifier, restorationIdentifier)

        verify(callback).visitCompleted(false)
    }

    @Test
    fun visitCompletedSavesRestorationIdentifier() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"
        assertThat(session.restorationIdentifiers.size()).isEqualTo(0)

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.visitCompleted(visitIdentifier, restorationIdentifier)

        assertThat(session.restorationIdentifiers.size()).isEqualTo(1)
    }

    @Test
    fun visitFormSubmissionStartedFiresCallback() {
        session.currentVisit = visit
        session.formSubmissionStarted(visit.location)

        verify(callback).formSubmissionStarted(visit.location)
    }

    @Test
    fun visitFormSubmissionFinishedFiresCallback() {
        session.currentVisit = visit
        session.formSubmissionFinished(visit.location)

        verify(callback).formSubmissionFinished(visit.location)
    }

    @Test
    fun pageLoadedSavesRestorationIdentifier() {
        val restorationIdentifier = "67890"
        assertThat(session.restorationIdentifiers.size()).isEqualTo(0)

        session.currentVisit = visit
        session.pageLoaded(restorationIdentifier)

        assertThat(session.restorationIdentifiers.size()).isEqualTo(1)
    }

    @Test
    fun pendingVisitIsVisitedWhenReady() {
        session.currentVisit = visit
        session.visitPending = true

        session.turboIsReady(true)
        assertThat(session.visitPending).isFalse()
    }

    @Test
    fun resetToColdBoot() {
        session.currentVisit = visit
        session.isReady = true
        session.isColdBooting = false
        session.reset()

        assertThat(session.isReady).isFalse()
        assertThat(session.isColdBooting).isFalse()
    }

    @Test
    fun resetToColdBootClearsIdentifiers() {
        val visitIdentifier = "12345"
        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.coldBootVisitIdentifier = "0"
        session.reset()

        assertThat(session.coldBootVisitIdentifier).isEmpty()
        assertThat(session.currentVisit?.identifier).isEmpty()
    }

    @Test
    fun restoreCurrentVisit() {
        val visitIdentifier = "12345"
        val restorationIdentifier = "67890"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)
        session.pageLoaded(restorationIdentifier)

        assertThat(session.restoreCurrentVisit(callback)).isTrue()
        verify(callback, times(2)).visitCompleted(false)
    }

    @Test
    fun restoreCurrentVisitFailsWithNoRestorationIdentifier() {
        val visitIdentifier = "12345"

        session.currentVisit = visit.copy(identifier = visitIdentifier)
        session.turboIsReady(true)

        assertThat(session.restoreCurrentVisit(callback)).isFalse()
        verify(callback, times(1)).visitCompleted(false)
    }

    @Test
    fun restoreCurrentVisitFailsWithSessionNotReady() {
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
    fun webViewIsNotNull() {
        assertThat(session.webView).isNotNull
    }
}

internal class TurboTestActivity : AppCompatActivity()
