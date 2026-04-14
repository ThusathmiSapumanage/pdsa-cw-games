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

async function handleResponse<T>(response: Response): Promise<T> {
  const data = await response.json();

  if (!response.ok) {
    const errorData = data as ApiError;
    throw new Error(errorData.message || errorData.error || "Request failed");
  }

  return data as T;
}

export async function startQueensRound(
  payload: StartRequest,
): Promise<StartResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/start`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse<StartResponse>(response);
}

export async function submitQueensAnswer(
  payload: SubmitRequest,
): Promise<SubmitResponse> {
  const response = await fetch(`${API_BASE_URL}/sixteen-queens/submit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
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
      headers: {
        "Content-Type": "application/json",
      },
    },
  );

  return handleResponse<StatusResponse>(response);
}
