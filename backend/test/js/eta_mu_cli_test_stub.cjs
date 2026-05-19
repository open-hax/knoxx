class AuthStorage {
  static create(file) {
    return new AuthStorage(file)
  }

  constructor(file) {
    this.file = file
    this.keys = {}
  }

  setRuntimeApiKey(provider, token) {
    this.keys[provider] = token
  }
}

class ModelRegistry {
  constructor(authStorage, modelsFile) {
    this.authStorage = authStorage
    this.modelsFile = modelsFile
  }

  find(provider, model) {
    return { provider, model, id: model }
  }
}

class DefaultResourceLoader {
  constructor(options) {
    this.options = options
  }

  reload() {
    return Promise.resolve(this)
  }
}

class SettingsManager {
  static inMemory(settings) {
    return new SettingsManager(settings)
  }

  constructor(settings) {
    this.settings = settings
  }
}

class SessionManager {
  static inMemory(workspaceRoot) {
    return new SessionManager(workspaceRoot)
  }

  constructor(workspaceRoot) {
    this.workspaceRoot = workspaceRoot
    this.events = []
  }

  newSession(session) {
    this.session = session
    return session
  }

  appendModelChange(provider, model) {
    this.events.push({ type: "model", provider, model })
  }

  appendThinkingLevelChange(level) {
    this.events.push({ type: "thinking", level })
  }
}

function createAgentSession(options) {
  const session = {
    options,
    isStreaming: false,
    agent: {
      setAfterToolCall(fn) {
        this.afterToolCall = fn
      }
    },
    setThinkingLevel(level) {
      this.thinkingLevel = level
    }
  }

  return Promise.resolve({ session })
}

module.exports = {
  AuthStorage,
  ModelRegistry,
  DefaultResourceLoader,
  SettingsManager,
  SessionManager,
  createAgentSession
}
