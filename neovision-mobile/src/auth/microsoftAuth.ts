import * as Crypto from 'expo-crypto';
import * as WebBrowser from 'expo-web-browser';
import { ENV } from '../config/env';

WebBrowser.maybeCompleteAuthSession();

function base64url(bytes: Uint8Array): string {
  let str = '';
  for (const b of bytes) str += String.fromCharCode(b);
  return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

async function generatePKCE() {
  const verifierBytes = await Crypto.getRandomBytesAsync(32);
  const verifier = base64url(verifierBytes);
  const digest = await Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    verifier,
    { encoding: Crypto.CryptoEncoding.BASE64 }
  );
  const challenge = digest.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  return { verifier, challenge };
}

export class MicrosoftAuthCancelledError extends Error {
  constructor() { super('El usuario cancelo el login con Microsoft'); }
}

export async function getMicrosoftAccessToken(): Promise<string> {
  const state = base64url(await Crypto.getRandomBytesAsync(16));
  const { verifier, challenge } = await generatePKCE();

  const params = new URLSearchParams({
    client_id:             ENV.CLIENT_ID,
    response_type:         'code',
    redirect_uri:          ENV.REDIRECT_URI,
    scope:                 ENV.SCOPES,
    state,
    code_challenge:        challenge,
    code_challenge_method: 'S256',
  });

  const result = await WebBrowser.openAuthSessionAsync(
    `${ENV.AUTH_URL}?${params.toString()}`,
    ENV.REDIRECT_URI
  );

  if (result.type !== 'success') throw new MicrosoftAuthCancelledError();

  const url = new URL(result.url);
  const code = url.searchParams.get('code');
  if (!code) throw new Error('No se recibió código de autorización');

  const tokenBody = new URLSearchParams({
    client_id:     ENV.CLIENT_ID,
    grant_type:    'authorization_code',
    code,
    redirect_uri:  ENV.REDIRECT_URI,
    code_verifier: verifier,
  });

  const res = await fetch(ENV.TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: tokenBody.toString(),
  });
  const data = await res.json();
  if (!data.access_token) {
    throw new Error(data.error_description || 'No se pudo obtener el token de Microsoft');
  }
  return data.access_token as string;
}
