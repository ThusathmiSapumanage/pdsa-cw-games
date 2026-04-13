"use client";

import React, { useEffect, useMemo, useState } from "react";
import AuthGate from "../../../components/auth/AuthGate";
import { useAuth } from "../../../components/auth/AuthProvider";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const GAME_NAME = "Traffic Simulation";

const NODE_POSITIONS: Record<string, { x: number; y: number; kind?: string }> = {
  A: { x: 70, y: 220, kind: "source" },
  B: { x: 210, y: 90 },
  C: { x: 210, y: 220 },
  D: { x: 210, y: 350 },
  E: { x: 390, y: 135 },
  F: { x: 390, y: 305 },
  G: { x: 560, y: 135 },
  H: { x: 560, y: 305 },
  T: { x: 735, y: 220, kind: "sink" },
};

const EDGE_ORDER = [
  ["A", "B"], ["A", "C"], ["A", "D"],
  ["B", "E"], ["B", "F"],
  ["C", "E"], ["C", "F"],
  ["D", "F"],
  ["E", "G"], ["E", "H"],
  ["F", "H"],
  ["G", "T"], ["H", "T"],
];

const keyFor = (a: string, b: string) => `${a}-${b}`;

function decodeEmail(token: string): string | null {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.sub || payload.playerEmail || null;
  } catch {
    return null;
  }
}

async function readJson(response: Response, fallback: string) {
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};
  if (!response.ok) throw new Error(data?.message || data?.detail || fallback);
  return data;
}

export default function TrafficSimulationPage() {
  const { accessToken, isReady } = useAuth();
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [gameId, setGameId] = useState<number | null>(null);
  const [playerId, setPlayerId] = useState<number | null>(null);
  const [roads, setRoads] = useState<any[]>([]);
  const [answer, setAnswer] = useState("");
  const [dinicRun, setDinicRun] = useState<any>(null);
  const [fordRun, setFordRun] = useState<any>(null);
  const [submission, setSubmission] = useState<any>(null);
  const [status, setStatus] = useState("Preparing traffic network...");
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [isLocked, setIsLocked] = useState(false);

  useEffect(() => {
    if (!isReady || !accessToken || sessionId) return;
    initializeSession();
  }, [isReady, accessToken, sessionId]);

  async function initializeSession() {
    try {
      const token = accessToken;
      if (!token) throw new Error("Please login first.");
      const email = decodeEmail(token);
      if (!email) throw new Error("Invalid login token.");

      const resolvedGameId = await resolveTrafficGameId(token);
      setGameId(resolvedGameId);

      const playersRes = await fetch(`${API_BASE_URL}/api/players`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const players = await readJson(playersRes, "Unable to load player profile");
      const currentPlayer = players.find((p: any) => p.playerEmail === email);

      if (!currentPlayer) throw new Error("Player account not found.");

      setPlayerId(currentPlayer.playerId);

      const newSessionId = await createNewSession(resolvedGameId, currentPlayer.playerId, token);
      setSessionId(newSessionId);

      generateRound(newSessionId);
    } catch (e: any) {
      setError(e.message);
      setStatus("Session initialization failed.");
    }
  }

  async function createNewSession(gameId: number, playerId: number, token: string) {
    const sessionRes = await fetch(`${API_BASE_URL}/api/game-sessions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ gameId, playerId }),
    });

    const session = await readJson(sessionRes, "Unable to create game session");
    return session.sessionId as number;
  }

  async function resolveTrafficGameId(token: string) {
    const gamesRes = await fetch(`${API_BASE_URL}/api/games`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    const games = await readJson(gamesRes, "Unable to load games");

    const existingGame = Array.isArray(games)
      ? games.find((game: any) => game?.gameName === GAME_NAME)
      : null;

    if (existingGame?.gameId) return existingGame.gameId as number;

    const createRes = await fetch(`${API_BASE_URL}/api/games`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ gameName: GAME_NAME }),
    });

    const createdGame = await readJson(createRes, "Unable to register traffic game");

    if (!createdGame?.gameId) throw new Error("Traffic game setup failed.");

    return createdGame.gameId as number;
  }

  const roadMap = useMemo(() => roads.reduce((acc: any, road: any) => {
    acc[keyFor(road.id.startNode, road.id.endNode)] = road.capacity;
    return acc;
  }, {}), [roads]);

  const bottleneckThreshold = useMemo(() => {
    const vals = Object.values(roadMap) as number[];
    return vals.length ? Math.min(...vals) + 1 : 0;
  }, [roadMap]);

  async function generateRound(activeSessionId?: number) {
    const token = accessToken;
    const sid = activeSessionId ?? sessionId;

    if (!sid || !token) return;

    setLoading(true);
    setSubmission(null);
    setAnswer("");
    setError("");
    setIsLocked(false);

    try {
      const generated = await fetch(`${API_BASE_URL}/api/traffic-sim/generate/${sid}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });

      setRoads(await readJson(generated, "Unable to generate network"));

      const [dRes, fRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/traffic-sim/dinic/${sid}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`${API_BASE_URL}/api/traffic-sim/ford/${sid}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      setDinicRun(await readJson(dRes, "Dinic failed"));
      setFordRun(await readJson(fRes, "Ford failed"));
      setStatus("System active. Trace the animated routes and compute max flow.");
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerateAndRun() {
    try {
      const token = accessToken;
      if (!token || !gameId || !playerId) return;

      const newSessionId = await createNewSession(gameId, playerId, token);
      setSessionId(newSessionId);

      await generateRound(newSessionId);
    } catch (e: any) {
      setError(e.message);
    }
  }

  async function submitAnswer() {
    const token = accessToken;
    if (!sessionId || !token) return;
    if (!answer.trim()) {
      setError("Enter a max flow value first.");
      return;
    }

    setSubmitting(true);

    try {
      const res = await fetch(
        `${API_BASE_URL}/api/traffic-sim/submit/${sessionId}?answer=${encodeURIComponent(answer)}`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      const data = await readJson(res, "Submit failed");
      setSubmission(data);

      if (data.correct) setIsLocked(true);

      setStatus(
        data.correct
          ? "Correct maximum flow identified."
          : `Incorrect answer. Correct max flow is ${data.correctAnswer}.`
      );
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  const controls = (
    <div className="flex flex-col gap-6">
      <div>
        <label className="block text-[10px] uppercase tracking-[0.35em] text-white/60 mb-3">
          Player Max Flow
        </label>
        <input
          type="number"
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          placeholder="Enter max flow"
          className="w-full bg-transparent border-b border-white/10 py-4 text-3xl font-bold outline-none"
        />
      </div>

      <button
        onClick={handleGenerateAndRun}
        disabled={loading}
        className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
      >
        {loading ? "GENERATING..." : "GENERATE & RUN"}
      </button>

      <button
        onClick={submitAnswer}
        disabled={submitting || loading || roads.length === 0}
        className="border border-white/20 py-3 rounded font-bold uppercase tracking-widest hover:border-green-400 hover:text-green-400 transition-colors text-xs"
      >
        {submitting ? "SUBMITTING..." : "SUBMIT ANSWER"}
      </button>

      {error ? <p className="text-red-400 text-xs">{error}</p> : null}
    </div>
  );

  const visualization = (
    <div className="w-full h-[500px] rounded-xl border border-blue-500/20 bg-blue-500/[0.03] p-4">
      <svg viewBox="0 0 810 430" className="w-full h-full">
        <defs>
          <marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto">
            <path d="M0,0 L0,6 L9,3 z" fill="rgba(96,165,250,0.9)" />
          </marker>
        </defs>

        {EDGE_ORDER.map(([from, to]) => {
          const s = NODE_POSITIONS[from];
          const e = NODE_POSITIONS[to];
          const lx = (s.x + e.x) / 2;
          const ly = (s.y + e.y) / 2 - 10;
          const cap = roadMap[keyFor(from, to)];
          const isBottleneck = cap !== undefined && cap <= bottleneckThreshold;

          return (
            <g key={keyFor(from, to)}>
              <line
                x1={s.x}
                y1={s.y}
                x2={e.x}
                y2={e.y}
                stroke={isBottleneck ? "rgba(248,113,113,0.9)" : "rgba(96,165,250,0.7)"}
                strokeWidth={isBottleneck ? 3 : 2}
                strokeDasharray="8 6"
                markerEnd="url(#arrow)"
              >
                <animate attributeName="stroke-dashoffset" from="14" to="0" dur="0.8s" repeatCount="indefinite" />
              </line>
              <text x={lx} y={ly} fill="white" fontSize="12" textAnchor="middle">
                {cap ?? "--"}
              </text>
            </g>
          );
        })}

        {Object.entries(NODE_POSITIONS).map(([node, p]) => (
          <g key={node}>
            <circle cx={p.x} cy={p.y} r="28" fill="rgba(15,23,42,0.95)" stroke="rgba(96,165,250,0.8)">
              <animate attributeName="r" values="28;29;28" dur="2s" repeatCount="indefinite" />
            </circle>
            <text x={p.x} y={p.y + 4} fill="white" textAnchor="middle" fontSize="16">
              {node}
            </text>
          </g>
        ))}
      </svg>
    </div>
  );

  const results = (
    <div className="flex flex-col gap-2 font-mono text-[10px] text-white/70">
      <p>Status: <span className="text-yellow-400">{status}</span></p>
      <p>Dinic: {dinicRun ? `${dinicRun.maxFlow} (${Number(dinicRun.timeMs).toFixed(3)} ms)` : "--"}</p>
      <p>Ford: {fordRun ? `${fordRun.maxFlow} (${Number(fordRun.timeMs).toFixed(3)} ms)` : "--"}</p>
      <p>Bottleneck threshold: {bottleneckThreshold || "--"}</p>
      <p>
        Result: {submission ? (
          submission.correct ? <span className="text-green-400">Correct</span> : <span className="text-red-400">Incorrect</span>
        ) : "Pending"}
      </p>
    </div>
  );

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="03"
        title="TRAFFIC SIMULATION"
        description="Maximum vehicle throughput analysis across a directed network with animated flow routes and bottleneck detection."
        controls={controls}
        visualization={visualization}
        results={results}
      />
    </AuthGate>
  );
}
