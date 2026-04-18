import { useState } from "react";
import GameLayoutShell from "./GameLayoutShell";
import ResultsPanel from "./ResultsPanel";

function MinimumCostGame() {
  const [matrix, setMatrix] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [blockedRows, setBlockedRows] = useState([]);
  const [blockedCols, setBlockedCols] = useState([]);
  const [logs, setLogs] = useState([]);
  const [totalCost, setTotalCost] = useState(0);
  const [greedyTotalCost, setGreedyTotalCost] = useState(0);
  const [hungarianTotalCost, setHungarianTotalCost] = useState(0);
  const [greedyTimeMs, setGreedyTimeMs] = useState(0);
  const [hungarianTimeMs, setHungarianTimeMs] = useState(0);

  const handleStartGame = () => {
    fetch("http://localhost:8080/api/mincost/start", {
      method: "POST",
    })
      .then((res) => res.json())
      .then((data) => {
        const nextMatrix = Array.isArray(data.matrix) ? data.matrix : [];
        const greedyAssignments = Array.isArray(data.greedyAssignments)
          ? data.greedyAssignments
          : [];
        const greedyLogs = Array.isArray(data.greedyLogs)
          ? data.greedyLogs
          : ["Matrix generated"];

        setMatrix(nextMatrix);
        setAssignments(greedyAssignments);
        setBlockedRows(greedyAssignments.map((item) => item.row));
        setBlockedCols(greedyAssignments.map((item) => item.col));
        setLogs(greedyLogs);

        setGreedyTotalCost(data.greedyTotalCost ?? 0);
        setHungarianTotalCost(data.hungarianTotalCost ?? 0);
        setGreedyTimeMs(data.greedyTimeMs ?? 0);
        setHungarianTimeMs(data.hungarianTimeMs ?? 0);
        setTotalCost(data.greedyTotalCost ?? 0);
      })
      .catch(() => {
        setLogs((prevLogs) => [
          ...prevLogs,
          "Failed to fetch matrix/results from backend",
        ]);
      });
  };

  const handleNextStep = () => {
    setLogs((prevLogs) => [
      ...prevLogs,
      "Use Generate & Run to refresh full results.",
    ]);
  };

  return (
    <>
      <GameLayoutShell
        matrix={matrix}
        assignments={assignments}
        blockedRows={blockedRows}
        blockedCols={blockedCols}
        logs={logs}
        totalCost={totalCost}
        greedyTotalCost={greedyTotalCost}
        hungarianTotalCost={hungarianTotalCost}
        greedyTimeMs={greedyTimeMs}
        hungarianTimeMs={hungarianTimeMs}
        onStartGame={handleStartGame}
        onNextStep={handleNextStep}
      />

      {/* Results comparison panel */}
      <ResultsPanel
        greedyTotalCost={greedyTotalCost}
        hungarianTotalCost={hungarianTotalCost}
        greedyTimeMs={greedyTimeMs}
        hungarianTimeMs={hungarianTimeMs}
      />
    </>
  );
}

export default MinimumCostGame;