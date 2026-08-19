export function safeGet(anahtar, varsayilan = null) {
  try {
    const deger = localStorage.getItem(anahtar)
    if (deger === null || deger === undefined) return varsayilan
    return JSON.parse(deger)
  } catch {
    try {
      localStorage.removeItem(anahtar)
    } catch {
      /* empty */
    }
    return varsayilan
  }
}

export function safeSet(anahtar, deger) {
  try {
    localStorage.setItem(anahtar, JSON.stringify(deger))
  } catch {
    /* empty */
  }
}
