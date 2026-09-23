import { generateKeyPairSync, createPrivateKey, createPublicKey, createSign, createVerify } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const privateKeyFile = resolve(root, 'artifacts/preset-catalog-private-key.pem');
const payloadFile = resolve(root, 'catalog/preset-catalog.payload.json');
const envelopeFile = resolve(root, 'webApp/src/wasmJsMain/resources/preset-catalog.json');

const command = process.argv[2];
if (command === 'init-key') {
  if (existsSync(privateKeyFile)) throw new Error(`Signing key already exists: ${privateKeyFile}`);
  mkdirSync(dirname(privateKeyFile), { recursive: true });
  const pair = generateKeyPairSync('ec', {
    namedCurve: 'prime256v1',
    publicKeyEncoding: { type: 'spki', format: 'der' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
  });
  writeFileSync(privateKeyFile, pair.privateKey, { mode: 0o600, flag: 'wx' });
  console.log(`Private key saved to ${privateKeyFile}. Back it up securely; never commit it.`);
  console.log(`Public key (base64 DER): ${pair.publicKey.toString('base64')}`);
} else if (command === 'sign') {
  const payload = readFileSync(payloadFile);
  const data = JSON.parse(payload.toString('utf8'));
  if (data.schemaVersion !== 2 || !Number.isSafeInteger(data.sequence) || data.sequence <= 10 ||
      typeof data.catalogVersion !== 'string' || typeof data.positionAsOf !== 'string' ||
      !data.positions || Array.isArray(data.positions) || typeof data.positions !== 'object' ||
      !Array.isArray(data.programs)) {
    throw new Error('The v2 catalog payload is incomplete.');
  }
  const privateKey = createPrivateKey(readFileSync(privateKeyFile));
  const publicKeyBase64 = createPublicKey(privateKey).export({ type: 'spki', format: 'der' }).toString('base64');
  if (!readFileSync(resolve(root, 'app/build.gradle.kts'), 'utf8').includes(publicKeyBase64) ||
      !readFileSync(resolve(root, 'webApp/src/wasmJsMain/resources/supabase-config.js'), 'utf8').includes(publicKeyBase64)) {
    throw new Error('The signing key does not match the public key pinned by both apps.');
  }
  const signature = createSign('SHA256').update(payload).end().sign(privateKey);
  const verified = createVerify('SHA256').update(payload).end().verify(createPublicKey(privateKey), signature);
  if (!verified) throw new Error('Signature verification failed.');
  const envelope = {
    format: 'app.veshinantam.preset-catalog',
    keyId: 'veshinantam-preset-v1',
    payload: payload.toString('base64'),
    signature: signature.toString('base64'),
  };
  const output = JSON.stringify(envelope, null, 2) + '\n';
  if (Buffer.byteLength(output) > 1024 * 1024) throw new Error('Signed catalog exceeds the 1 MiB download limit.');
  writeFileSync(envelopeFile, output);
  console.log(`Signed sequence ${data.sequence} into ${envelopeFile}`);
} else {
  throw new Error('Use: node scripts/preset-catalog.mjs init-key | sign');
}
