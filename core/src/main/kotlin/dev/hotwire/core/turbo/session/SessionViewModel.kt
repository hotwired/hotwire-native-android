package dev.hotwire.core.turbo.session

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.hotwire.core.turbo.visit.TurboVisitOptions

/**
 * Serves as a shared ViewModel to exchange data between [Session] and various other
 * internal classes. Typically used to share navigational events.
 */
internal class SessionViewModel : ViewModel() {
    /**
     * Represents visit options for the current visit. Typically consumed by a delegate to execute
     * a navigation action. Can only be consumed once.
     */
    var visitOptions: SessionEvent<TurboVisitOptions>? = null
        private set

    /**
     * A one-time event that can be observed to determine if a closing modal has returned a result
     * to be proceed. Can only be consumed once.
     */
    val modalResult: MutableLiveData<SessionEvent<SessionModalResult>> by lazy {
        MutableLiveData<SessionEvent<SessionModalResult>>()
    }

    /**
     * Convenience method to check if the modal result has already been consumed.
     */
    val modalResultExists: Boolean
        get() = modalResult.value?.hasBeenHandled == false

    /**
     * A one-time event that can be observed to determine when a dialog has been cancelled.
     */
    val dialogResult: MutableLiveData<SessionEvent<SessionDialogResult>> by lazy {
        MutableLiveData<SessionEvent<SessionDialogResult>>()
    }

    /**
     * Wraps the visit options in a [SessionEvent] to ensure it can only be consumed once.
     */
    fun saveVisitOptions(options: TurboVisitOptions) {
        visitOptions = SessionEvent(options)
    }

    /**
     * Wraps a modal result in a [SessionEvent] and updates the LiveData value.
     */
    fun sendModalResult(result: SessionModalResult) {
        modalResult.value = SessionEvent(result)
    }

    /**
     * Wraps a dialog result in a [SessionEvent] and updates the LiveData value.
     */
    fun sendDialogResult() {
        dialogResult.value = SessionEvent(SessionDialogResult(true))
    }

    companion object {
        fun get(sessionName: String, activity: FragmentActivity): SessionViewModel {
            return ViewModelProvider(activity).get(
                sessionName, SessionViewModel::class.java
            )
        }
    }
}
