package dev.hotwire.core.turbo.config

/**
 * Represents the current state of the path configuration loading process.
 * Observe [PathConfiguration.loadState] to receive updates as the configuration
 * is loaded from each source.
 */
sealed class PathConfigurationLoadState {
    /**
     * The initial state before any configuration has been loaded.
     */
    data object Idle : PathConfigurationLoadState()

    /**
     * The configuration was loaded from the locally bundled asset file.
     */
    data class BundledAssetLoaded(val configuration: PathConfigurationData) : PathConfigurationLoadState()

    /**
     * The configuration was loaded from a previously cached remote file.
     */
    data class CachedRemoteLoaded(val configuration: PathConfigurationData) : PathConfigurationLoadState()

    /**
     * The configuration was freshly loaded from the remote server.
     */
    data class RemoteLoaded(val configuration: PathConfigurationData) : PathConfigurationLoadState()
}
