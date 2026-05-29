import React, { useState, useEffect } from "react";
import "./App.css";
import {
  interpretDate,
  fetchHistory,
  type InterpretResponse,
  type HistoryItem,
} from "./services/dateInterpreter-api";

const EXAMPLES = [
  "Monday in two weeks",
  "next Tuesday",
  "three weeks from now",
  "last day of this month",
  "two days before Christmas",
];

function App() {
  const [input, setInput] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const [result, setResult] = useState<InterpretResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const data = await fetchHistory();
      setHistory(data);
    } catch {
      // Non-critical — history may just be empty
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleSubmit = async () => {
    const query = input.trim();
    if (!query) return;

    setLoading(true);
    setError(null);
    setSubmittedQuery(query);

    try {
      const res = await interpretDate(query);
      setResult(res);
      // Optimistically prepend to history
      setHistory((prev) => [
        {
          id: Date.now(),
          userInput: query,
          jsonResponse: res,
          createdAt: new Date().toISOString(),
        },
        ...prev,
      ]);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : "An unexpected error occurred"
      );
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && e.ctrlKey) handleSubmit();
  };

  return (
    <div className="app">
      <header className="header">
        <h1>🗓️ Natural Language Date Interpreter</h1>
        <p>
          Type a date expression in plain English and get a structured JSON
          response powered by AI
        </p>
      </header>

      <main className="main">
        {/* ── Left column: Input + Result ── */}
        <section className="left-col">
          <div className="card input-card">
            <h2>Ask a Question</h2>
            <textarea
              className="input-area"
              placeholder={`e.g. "next Tuesday", "three weeks from now"…`}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={loading}
              rows={4}
            />
            <button
              className="btn-primary"
              onClick={handleSubmit}
              disabled={loading || !input.trim()}
            >
              {loading ? "⏳ Interpreting…" : "🔍 Interpret"}
            </button>
            <p className="hint">Ctrl + Enter to submit</p>

            <div className="examples">
              <span className="examples-label">Try an example:</span>
              <div className="chips">
                {EXAMPLES.map((ex) => (
                  <button
                    key={ex}
                    className="chip"
                    onClick={() => setInput(ex)}
                    disabled={loading}
                  >
                    {ex}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {error && <div className="alert alert-error">❌ {error}</div>}

          {result && (
            <div className="card result-card">
              <h3>✅ Result</h3>
              <div className="result-query">
                <strong>Query:</strong> <span className="query-text">{submittedQuery}</span>
              </div>
              <div className="json-block">
                <pre>{JSON.stringify(result, null, 2)}</pre>
              </div>
            </div>
          )}
        </section>

        {/* ── Right column: History ── */}
        <section className="right-col">
          <div className="history-header">
            <h2>📋 History</h2>
            {history.length > 0 && (
              <span className="badge">{history.length}</span>
            )}
          </div>

          {historyLoading ? (
            <p className="subtle">Loading history…</p>
          ) : history.length === 0 ? (
            <div className="card empty-state">
              <p>No queries yet — submit something above to get started!</p>
            </div>
          ) : (
            <div className="history-list">
              {history.map((item) => (
                <div key={item.id} className="card history-item">
                  <div className="history-item-query">{item.userInput}</div>
                  <div className="json-block small">
                    <pre>{JSON.stringify(item.jsonResponse, null, 2)}</pre>
                  </div>
                  <div className="history-item-time">
                    {new Date(item.createdAt).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

export default App;
