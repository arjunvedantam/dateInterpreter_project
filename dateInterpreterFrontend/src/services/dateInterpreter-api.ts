export type InterpretResponse = Record<string, unknown>;

export interface HistoryItem {
  id: number;
  userInput: string;
  jsonResponse: Record<string, unknown>;
  createdAt: string;
}

const API_BASE = "/api/date";

export async function interpretDate(text: string): Promise<InterpretResponse> {
  const res = await fetch(`${API_BASE}/interpret`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text }),
  });

  if (!res.ok) {
    const body = await res.text();
    throw new Error(body || `Request failed: ${res.status} ${res.statusText}`);
  }

  return res.json();
}

export async function fetchHistory(): Promise<HistoryItem[]> {
  const res = await fetch(`${API_BASE}/history`);
  if (!res.ok) throw new Error(`Failed to fetch history: ${res.statusText}`);
  return res.json();
}
