// Safe localStorage helpers with quota protection
export function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function safeSetItem(key: string, value: string): boolean {
  try {
    localStorage.setItem(key, value);
    return true;
  } catch {
    // Quota exceeded — try clearing old knoxx keys
    try {
      const keysToRemove: string[] = [];
      for (let i = 0; i < localStorage.length; i++) {
        const k = localStorage.key(i);
        if (k && k.startsWith("knoxx_") && k !== key) {
          keysToRemove.push(k);
        }
      }
      // Remove oldest half
      keysToRemove.slice(0, Math.floor(keysToRemove.length / 2)).forEach((k) => {
        try { localStorage.removeItem(k); } catch { /* ignore */ }
      });
      // Try again
      localStorage.setItem(key, value);
      return true;
    } catch {
      return false;
    }
  }
}

export function safeRemoveItem(key: string): void {
  try { localStorage.removeItem(key); } catch { /* ignore */ }
}

export function clearKnoxxStorage(): void {
  const keysToRemove: string[] = [];
  for (let i = 0; i < localStorage.length; i++) {
    const k = localStorage.key(i);
    if (k && k.startsWith("knoxx_")) {
      keysToRemove.push(k);
    }
  }
  keysToRemove.forEach((k) => {
    try { localStorage.removeItem(k); } catch { /* ignore */ }
  });
}
