(() => {
  // This represents the adapter that is installed on the webBridge
  // All adapters implement the same interface so the web doesn't need to
  // know anything specific about the client platform
  class NativeBridge {
    constructor() {
      this.supportedComponents = []
      this.adapterIsRegistered = false
    }

    register(component) {
      if (Array.isArray(component)) {
        this.supportedComponents = this.supportedComponents.concat(component)
      } else {
        this.supportedComponents.push(component)
      }

      if (!this.adapterIsRegistered) {
        this.registerAdapter()
      }
      this.notifyBridgeOfSupportedComponentsUpdate()
    }

    unregister(component) {
      const index = this.supportedComponents.indexOf(component)
      if (index != -1) {
        this.supportedComponents.splice(index, 1)
        this.notifyBridgeOfSupportedComponentsUpdate()
      }
    }

    registerAdapter() {
      this.adapterIsRegistered = true

      if (this.isBridgeAvailable) {
        this.webBridge.setAdapter(this)
      } else {
        document.addEventListener("web-bridge:ready", () => this.webBridge.setAdapter(this))
      }
    }

    notifyBridgeOfSupportedComponentsUpdate() {
      this.supportedComponentsUpdated()

      if (this.isBridgeAvailable) {
        this.webBridge.adapterDidUpdateSupportedComponents()
      }
    }

    supportsComponent(component) {
      return this.supportedComponents.includes(component)
    }

    // Reply to web with message
    replyWith(message) {
      if (this.isBridgeAvailable) {
        this.webBridge.receive(JSON.parse(message))
      }
    }

    // Receive from web
    receive(message) {
      this.postMessage(JSON.stringify(message))
    }

    get platform() {
      return "android"
    }

    // Native handler

    ready() {
      BridgeComponentsNative.bridgeDidInitialize()
    }

    supportedComponentsUpdated() {
      BridgeComponentsNative.bridgeDidUpdateSupportedComponents()
    }

    postMessage(message) {
      BridgeComponentsNative.bridgeDidReceiveMessage(message)
    }

    // Web global

    get isBridgeAvailable() {
      // Fallback to Strada for legacy Strada web JavaScript.
      return window.HotwireNative ?? window.Strada
    }

    get webBridge() {
      // Fallback to Strada for legacy Strada web JavaScript.
      return window.HotwireNative?.web ?? window.Strada.web
    }
  }

  if (document.readyState === 'interactive' || document.readyState === 'complete') {
    initializeBridge()
  } else {
    document.addEventListener("DOMContentLoaded", () => {
      initializeBridge()
    })
  }

  function initializeBridge() {
    window.nativeBridge = new NativeBridge()
    window.nativeBridge.ready()
  }
})()
