import React, { useEffect, useState } from "react";
import "./Traffic.css";

const sessionId = 1;

export default function Traffic() {
  const [roads, setRoads] = useState([]);
  const [correctFlow, setCorrectFlow] = useState(null);
  const [timeMs, setTimeMs] = useState(null);
  const [userAnswer, setUserAnswer] = useState("");
  const [result, setResult] = useState("");

  useEffect(() => {
    generateNetwork();
  }, []);

  const generateNetwork = async () => {
    const res = await fetch(
      `http://localhost:8080/api/traffic-sim/generate/${sessionId}`,
      { method: "POST" }
    );
    const data = await res.json();
    setRoads(data);

    const dinicRes = await fetch(
      `http://localhost:8080/api/traffic-sim/dinic/${sessionId}`
    );
    const dinicData = await dinicRes.json();

    setCorrectFlow(dinicData.maxFlow);
    setTimeMs(dinicData.timeMs);
  };

  const handleSubmit = () => {
    if (parseInt(userAnswer) === correctFlow) {
      setResult("Correct! You Win!");
    } else {
      setResult(`Wrong! Correct flow is ${correctFlow}`);
    }
  };

  return (
    <div className="traffic-shell">
      <div className="traffic-shell__header">
        <h1 className="traffic-shell__title">Traffic Simulation Game</h1>
        <div className="traffic-shell__actions">
          <button
            type="button"
            className="traffic-button traffic-button--secondary"
            onClick={generateNetwork}
          >
            Generate Network
          </button>
          <button
            type="button"
            className="traffic-button traffic-button--primary"
            onClick={handleSubmit}
          >
            Submit Answer
          </button>
        </div>
      </div>

      <div className="traffic-shell__grid">
        <section className="traffic-panel">
          <div className="traffic-panel__heading">
            <h2>Road Network</h2>
            <p>Review each directed road and its capacity before submitting the max flow.</p>
          </div>

          <div className="roads-list">
            {roads.length === 0 ? (
              <div className="traffic-empty-state">No network generated yet.</div>
            ) : (
              roads.map((road, index) => (
                <div key={index} className="road-card">
                  <span className="road-card__route">
                    {road.id.startNode} &rarr; {road.id.endNode}
                  </span>
                  <span className="road-card__capacity">Capacity: {road.capacity}</span>
                </div>
              ))
            )}
          </div>
        </section>

        <section className="traffic-panel">
          <div className="traffic-panel__heading">
            <h2>Player Input</h2>
            <p>Enter the maximum flow for the generated network.</p>
          </div>

          <label className="traffic-input-group">
            <span>Your max flow answer</span>
            <input
              type="number"
              placeholder="Enter max flow"
              value={userAnswer}
              onChange={(e) => setUserAnswer(e.target.value)}
            />
          </label>

          <div className="traffic-metrics">
            <div className="traffic-metric-card">
              <span className="traffic-metric-card__label">Dinic Runtime</span>
              <strong className="traffic-metric-card__value">
                {timeMs !== null ? `${timeMs.toFixed(3)} ms` : "Pending"}
              </strong>
            </div>
            <div className="traffic-metric-card">
              <span className="traffic-metric-card__label">Road Count</span>
              <strong className="traffic-metric-card__value">{roads.length}</strong>
            </div>
          </div>
        </section>
      </div>

      <section className="traffic-panel">
        <div className="traffic-panel__heading">
          <h2>Algorithm Log</h2>
          <p>Current evaluation and simulation status.</p>
        </div>

        <div className="traffic-log">
          {result ? (
            <p className="traffic-log__message">{result}</p>
          ) : (
            <p className="traffic-log__placeholder">
              No result yet. Generate a network and submit an answer.
            </p>
          )}
        </div>
      </section>
    </div>
  );
}
