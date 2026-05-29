export type InterpretResponse = Record<string, unknown>;

export interface HistoryItem {
  id: number;
  userInput: string;
  jsonResponse: Record<string, unknown>;
  createdAt: string;
}

const API_BASE = "/api/date";

interface ApiErrorResponse {
  code?: string;
  message?: string;
  detail?: string;
}

async function readError(res: Response): Promise<string> {
  const contentType = res.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    const body = (await res.json()) as ApiErrorResponse;
    if (body.message && body.detail) return `${body.message}: ${body.detail}`;
    if (body.message) return body.message;
  }

  const body = await res.text();
  return body || `Request failed: ${res.status} ${res.statusText}`;
}

export async function interpretDate(
  text: string,
  timezone?: string
): Promise<InterpretResponse> {
  const res = await fetch(`${API_BASE}/interpret`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text, timezone }),
  });

  if (!res.ok) {
    throw new Error(await readError(res));
  }

  return res.json();
}

export async function fetchHistory(): Promise<HistoryItem[]> {
  const res = await fetch(`${API_BASE}/history`);
  if (!res.ok) throw new Error(await readError(res));
  return res.json();
}
