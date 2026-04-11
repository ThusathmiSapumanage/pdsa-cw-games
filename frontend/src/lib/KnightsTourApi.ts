import {
  StartRequest,
  StartResponse,
  SubmitRequest,
  SubmitResponse,
  HintRequest,
  HintResponse,
  CorrectAnswerResponse,
  ApiError,
} from "../../components/knightstour/types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

async function handleResponse<T>(response: Response): Promise<T> {
  const data = await response.json();

  if (!response.ok) {
    const errorData = data as ApiError;
    throw new Error(errorData.message || "Request failed");
  }

  return data as T;
}

export async function startKnightTourRound(
  payload: StartRequest
): Promise<StartResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/start`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse<StartResponse>(response);
}

export async function submitKnightTourAnswer(
  payload: SubmitRequest
): Promise<SubmitResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/submit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse<SubmitResponse>(response);
}

export async function getKnightTourHint(
  payload: HintRequest
): Promise<HintResponse> {
  const response = await fetch(`${API_BASE_URL}/knights-tour/hint`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  return handleResponse<HintResponse>(response);
}

export async function getKnightTourCorrectAnswer(
  sessionId: number,
  playerId: number
): Promise<CorrectAnswerResponse> {
  const response = await fetch(
    `${API_BASE_URL}/knights-tour/${sessionId}/correct-answer?playerId=${playerId}`,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    }
  );

  return handleResponse<CorrectAnswerResponse>(response);
}