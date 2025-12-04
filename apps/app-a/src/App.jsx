import React, { useEffect, useState } from "react";
import { keycloakConfig } from "./keycloakConfig";

const STORAGE_KEY_TOKEN = "appA_access_token";
const STORAGE_KEY_EXPIRES_AT = "appA_expires_at";
const STORAGE_KEY_ID_TOKEN = "appA_id_token";
const PKCE_VERIFIER_PREFIX = "appA_pkce_verifier_";
const PKCE_STATE_PREFIX = "appA_pkce_state_";

// ---------- Helpers PKCE ----------

function base64UrlEncode(arrayBuffer) {
  const bytes = new Uint8Array(arrayBuffer);
  let binary = "";
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function generateRandomString(length = 43) {
  const bytes = new Uint8Array(length);
  window.crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes.buffer);
}

async function generatePkcePair() {
  const verifier = generateRandomString(43); // 43–128 chars
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await window.crypto.subtle.digest("SHA-256", data);
  const challenge = base64UrlEncode(digest);
  return { verifier, challenge };
}

// ---------- Sessão local ----------

function getStoredSession() {
  const token = localStorage.getItem(STORAGE_KEY_TOKEN);
  const expiresAtStr = localStorage.getItem(STORAGE_KEY_EXPIRES_AT);

  if (!token || !expiresAtStr) return null;

  const expiresAt = parseInt(expiresAtStr, 10);
  const now = Date.now();

  if (Number.isNaN(expiresAt) || now >= expiresAt) {
    localStorage.removeItem(STORAGE_KEY_TOKEN);
    localStorage.removeItem(STORAGE_KEY_EXPIRES_AT);
    localStorage.removeItem(STORAGE_KEY_ID_TOKEN);
    return null;
  }

  return { token, expiresAt };
}

function saveSession(tokenResponse) {
  const { access_token, expires_in, id_token } = tokenResponse;
  const now = Date.now();
  const expiresAt = now + expires_in * 1000; // segundos → ms

  localStorage.setItem(STORAGE_KEY_TOKEN, access_token);
  localStorage.setItem(STORAGE_KEY_EXPIRES_AT, String(expiresAt));
  if (id_token) {
    localStorage.setItem(STORAGE_KEY_ID_TOKEN, id_token);
  }
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEY_TOKEN);
  localStorage.removeItem(STORAGE_KEY_EXPIRES_AT);
  localStorage.removeItem(STORAGE_KEY_ID_TOKEN);
}

// ---------- Fluxo OIDC ----------

function buildAuthUrl({ codeChallenge, state }) {
  const { baseUrl, realm, clientId } = keycloakConfig;
  const redirectUri = `${window.location.origin}/callback`;

  const params = new URLSearchParams();
  params.set("client_id", clientId);
  params.set("response_type", "code"); // Authorization Code Flow
  params.set("redirect_uri", redirectUri);
  params.set("scope", "openid");
  params.set("code_challenge", codeChallenge);
  params.set("code_challenge_method", "S256");
  params.set("state", state);

  return `${baseUrl}/realms/${realm}/protocol/openid-connect/auth?${params.toString()}`;
}

async function exchangeCodeForToken(code, codeVerifier) {
  const { baseUrl, realm, clientId } = keycloakConfig;
  const redirectUri = `${window.location.origin}/callback`;

  const params = new URLSearchParams();
  params.set("grant_type", "authorization_code");
  params.set("code", code);
  params.set("client_id", clientId);
  params.set("redirect_uri", redirectUri);
  params.set("code_verifier", codeVerifier);

  const response = await fetch(
    `${baseUrl}/realms/${realm}/protocol/openid-connect/token`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: params.toString(),
    }
  );

  const text = await response.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = text;
  }

  if (!response.ok) {
    console.error("Erro ao trocar code por token:", body);
    let msg = "Failed to exchange code for token";
    if (body && typeof body === "object") {
      if (body.error_description) msg = body.error_description;
      else if (body.error) msg = body.error;
    }
    throw new Error(msg);
  }

  return body; // { access_token, expires_in, id_token, ... }
}

// ---------- Componentes ----------

function Dashboard({ onLogout }) {
  const token = localStorage.getItem(STORAGE_KEY_TOKEN);

  return (
    <div style={{ fontFamily: "sans-serif", padding: "2rem" }}>
      <h1>Welcome to Application A</h1>
      <p>Sessão ativa nesta aplicação enquanto o token for válido.</p>

      {token && (
        <>
          <p style={{ marginTop: "1rem" }}>
            Access token (truncado, apenas para debug):
          </p>
          <code>{token.substring(0, 80)}...</code>
        </>
      )}

      <div style={{ marginTop: "1.5rem" }}>
        <button
          onClick={onLogout}
          style={{
            padding: "0.5rem 1rem",
            borderRadius: "8px",
            border: "none",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          Logout
        </button>
      </div>
    </div>
  );
}

function CallbackPage({ onAuthenticated }) {
  const [status, setStatus] = useState("Processando autenticação...");
  const [error, setError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get("code");
    const errorParam = params.get("error");
    const state = params.get("state");

    if (errorParam) {
      setStatus("");
      setError(`Erro na autenticação: ${errorParam}`);
      return;
    }

    if (!code || !state) {
      setStatus("");
      setError("Missing authorization code or state.");
      return;
    }

    const verifierKey = `${PKCE_VERIFIER_PREFIX}${state}`;
    const codeVerifier = sessionStorage.getItem(verifierKey);

    if (!codeVerifier) {
      setStatus("");
      setError("PKCE code_verifier não encontrado (state inválido ou expirado).");
      return;
    }

    sessionStorage.removeItem(verifierKey);
    sessionStorage.removeItem(`${PKCE_STATE_PREFIX}${state}`);

    (async () => {
      try {
        setStatus("Trocando código por token...");
        const tokenResponse = await exchangeCodeForToken(code, codeVerifier);
        saveSession(tokenResponse);
        setStatus("Sessão criada com sucesso. Redirecionando...");

        onAuthenticated();
        window.history.replaceState({}, "", "/");
      } catch (e) {
        console.error(e);
        setStatus("");
        setError(e.message || "Falha ao trocar código por token.");
      }
    })();
  }, [onAuthenticated]);

  // return (
  //   <div style={{ fontFamily: "sans-serif", padding: "2rem" }}>
  //     <p>{status}</p>
  //   </div>
  // );
}

export default function App() {
  const [session, setSession] = useState(() => getStoredSession());
  const isCallback = window.location.pathname.startsWith("/callback");

  useEffect(() => {
    const current = getStoredSession();
    setSession(current);
  }, []);

  const handleLogin = async () => {
    const { verifier, challenge } = await generatePkcePair();
    const state = generateRandomString(16);

    sessionStorage.setItem(`${PKCE_VERIFIER_PREFIX}${state}`, verifier);
    sessionStorage.setItem(`${PKCE_STATE_PREFIX}${state}`, "1");

    const authUrl = buildAuthUrl({ codeChallenge: challenge, state });
    window.location.href = authUrl;
  };

  const handleLogout = () => {
    const { baseUrl, realm, clientId } = keycloakConfig;
    const idToken = localStorage.getItem(STORAGE_KEY_ID_TOKEN);
    const postLogoutRedirect = window.location.origin;

    clearSession();
    setSession(null);

    // monta URL de logout do Keycloak
    const params = new URLSearchParams();
    params.set("client_id", clientId);
    params.set("post_logout_redirect_uri", postLogoutRedirect);
    if (idToken) {
      params.set("id_token_hint", idToken);
    }

    const logoutUrl = `${baseUrl}/realms/${realm}/protocol/openid-connect/logout?${params.toString()}`;
    window.location.href = logoutUrl;
  };

  if (isCallback) {
    return (
      <CallbackPage
        onAuthenticated={() => {
          const current = getStoredSession();
          setSession(current);
        }}
      />
    );
  }

  if (session) {
    return <Dashboard onLogout={handleLogout} />;
  }

  return (
    <div
      style={{
        fontFamily: "sans-serif",
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#f5f5f5",
      }}
    >
      <div
        style={{
          background: "#fff",
          padding: "2rem",
          borderRadius: "12px",
          boxShadow: "0 4px 10px rgba(0,0,0,0.1)",
          minWidth: "320px",
          textAlign: "center",
        }}
      >
        <h2 style={{ marginBottom: "1.5rem" }}>Application A</h2>
        <p style={{ marginBottom: "1.5rem" }}>
          Clique em &quot;Login&quot; para autenticar no RHBK.
        </p>
        <button
          onClick={handleLogin}
          style={{
            width: "100%",
            padding: "0.75rem",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          Login
        </button>
      </div>
    </div>
  );
}
