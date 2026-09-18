(() => {
  const CACHE_KEY = 'veshinantam.preset-catalog.v1';
  const MAX_BYTES = 1024 * 1024;
  const FORMAT = 'app.veshinantam.preset-catalog';
  const KEY_ID = 'veshinantam-preset-v1';
  const config = window.VESHINANTAM_PRESET_UPDATES || {};
  let activePayload = null;

  const decodeBase64 = value => Uint8Array.from(atob(value), character => character.charCodeAt(0));
  const readLength = (bytes, offset) => {
    const first = bytes[offset];
    if (first < 0x80) return { length: first, bytes: 1 };
    const count = first & 0x7f;
    if (count < 1 || count > 2 || offset + count >= bytes.length) throw new Error('Invalid DER length');
    let length = 0;
    for (let index = 0; index < count; index++) length = (length << 8) | bytes[offset + 1 + index];
    return { length, bytes: count + 1 };
  };
  const derEcdsaToRaw = signature => {
    let offset = 0;
    if (signature[offset++] !== 0x30) throw new Error('Invalid DER sequence');
    const sequence = readLength(signature, offset); offset += sequence.bytes;
    if (offset + sequence.length !== signature.length || signature[offset++] !== 0x02) throw new Error('Invalid DER signature');
    const rLength = readLength(signature, offset); offset += rLength.bytes;
    let r = signature.slice(offset, offset + rLength.length); offset += rLength.length;
    if (signature[offset++] !== 0x02) throw new Error('Invalid DER signature');
    const sLength = readLength(signature, offset); offset += sLength.bytes;
    let s = signature.slice(offset, offset + sLength.length); offset += sLength.length;
    if (offset !== signature.length) throw new Error('Invalid DER signature');
    while (r.length > 32 && r[0] === 0) r = r.slice(1);
    while (s.length > 32 && s[0] === 0) s = s.slice(1);
    if (r.length > 32 || s.length > 32) throw new Error('Invalid ECDSA value');
    const raw = new Uint8Array(64);
    raw.set(r, 32 - r.length); raw.set(s, 64 - s.length);
    return raw;
  };
  const verify = async envelopeText => {
    if (new TextEncoder().encode(envelopeText).length > MAX_BYTES) throw new Error('Catalog is too large');
    const envelope = JSON.parse(envelopeText);
    if (envelope.format !== FORMAT || envelope.keyId !== KEY_ID) throw new Error('Unexpected catalog envelope');
    const payloadBytes = decodeBase64(envelope.payload);
    if (payloadBytes.length > MAX_BYTES) throw new Error('Catalog payload is too large');
    const keyBytes = decodeBase64(config.publicKey || '');
    const key = await crypto.subtle.importKey('spki', keyBytes, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
    const valid = await crypto.subtle.verify(
      { name: 'ECDSA', hash: 'SHA-256' }, key, derEcdsaToRaw(decodeBase64(envelope.signature)), payloadBytes
    );
    if (!valid) throw new Error('Catalog signature is invalid');
    const payload = JSON.parse(new TextDecoder().decode(payloadBytes));
    if (payload.schemaVersion !== 1 || typeof payload.catalogVersion !== 'string' ||
        !Number.isSafeInteger(payload.sequence) || payload.sequence < 1 ||
        !/^\d{4}-\d{2}-\d{2}$/.test(payload.positionAsOf) ||
        !payload.positions || Array.isArray(payload.positions) || typeof payload.positions !== 'object') {
      throw new Error('Catalog payload is invalid');
    }
    return payload;
  };
  const accept = async envelopeText => {
    const payload = await verify(envelopeText);
    if (!activePayload || payload.sequence > activePayload.sequence) activePayload = payload;
    return payload;
  };

  window.veshinantamReadPresetCatalog = () => activePayload ? JSON.stringify(activePayload) : null;
  window.veshinantamDiscardPresetCatalog = () => {
    activePayload = null;
    localStorage.removeItem(CACHE_KEY);
  };
  window.veshinantamPresetCatalogReady = window.veshinantamStorageReady.then(async () => {
    const cached = localStorage.getItem(CACHE_KEY);
    if (cached && config.publicKey) {
      try { await accept(cached); } catch (_) { localStorage.removeItem(CACHE_KEY); }
    }
    let state = {};
    try { state = JSON.parse(window.veshinantamReadState() || '{}'); } catch (_) { /* Use defaults. */ }
    if (!state.automaticPresetUpdates || !config.endpoint || !config.publicKey) return;
    try {
      const endpoint = new URL(config.endpoint, location.href);
      if (endpoint.protocol !== 'https:') return;
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 15000);
      let response;
      try {
        response = await fetch(endpoint, { headers: { Accept: 'application/json' }, redirect: 'error', signal: controller.signal });
      } finally {
        clearTimeout(timeout);
      }
      if (!response.ok) return;
      const declaredLength = Number(response.headers.get('content-length') || 0);
      if (declaredLength > MAX_BYTES) return;
      const envelope = await response.text();
      const payload = await accept(envelope);
      if (payload.sequence > 8) localStorage.setItem(CACHE_KEY, envelope);
    } catch (_) { /* The verified cached or bundled catalog remains active. */ }
  });
})();
