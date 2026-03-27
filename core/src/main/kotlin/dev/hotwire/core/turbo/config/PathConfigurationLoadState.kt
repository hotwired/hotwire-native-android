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
    data object NotLoaded : PathConfigurationLoadState()

    /**
     * The configuration was successfully loaded from a source. Check the
     * specific subclass to determine the source: [BundledAssetLoaded],
     * [CachedRemoteLoaded], or [RemoteLoaded].
     */
    sealed class Loaded(open val configuration: PathConfigurationData) : PathConfigurationLoadState() {
        /**
         * The configuration was loaded from the locally bundled asset file.
         */
        data class BundledAssetLoaded(override val configuration: PathConfigurationData) : Loaded(configuration)

        /**
         * The configuration was loaded from a previously cached remote file.
         */
        data class CachedRemoteLoaded(override val configuration: PathConfigurationData) : Loaded(configuration)

        /**
         * The configuration was freshly loaded from the remote server.
         */
        data class RemoteLoaded(override val configuration: PathConfigurationData) : Loaded(configuration)
    }
}
