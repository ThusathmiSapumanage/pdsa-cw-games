import {
  ApiError,
  StartRequest,
  StartResponse,
  StatusResponse,
  SubmitRequest,
  SubmitResponse,
} from "../../components/queens/types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

function authHeaders(): HeadersInit {
  const headers: HeadersInit = { "Content-Type": "application/json" };
  if (typeof window !== "undefined") {
    const token = localStorage.getItem("accessToken");
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }
  return headers;
}

async function handleResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    const errorData = (data ?? {}) as ApiError;
    throw new Error(
      errorData.message || errorData.error || `Request failed (${response.status})`,
    );
  }

  return data as T;
}

export async function startQueensRound(
  payload: StartRequest,
): Promise<StartResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/start`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return handleResponse<StartResponse>(response);
}

export async function submitQueensAnswer(
  payload: SubmitRequest,
): Promise<SubmitResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/submit`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });

  return handleResponse<SubmitResponse>(response);
}

export async function getQueensStatus(
  sessionId: number,
  playerId: number,
): Promise<StatusResponse> {
  const response = await fetch(
    `${API_BASE_URL}/sixteen-queens/${sessionId}/status?playerId=${playerId}`,
    {
      method: "GET",
      headers: authHeaders(),
    },
  );

  return handleResponse<StatusResponse>(response);
}
