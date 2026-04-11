"use client";

import React, { useEffect, useMemo, useState } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AuthGate from "../../../components/auth/AuthGate";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";

export default function TrafficSimulationPage() {
  const [topology, setTopology] = useState("default");

  return (
    <AuthGate>
      <GameLayoutShell
        gameId="03"
        title="Traffic Simulation"
        description="Maximum vehicle throughput analysis across a directed network. Solves for peak capacity flow."
        controls={
          <div className="flex flex-col gap-4">
            <AlgoDropdown
              label="Network Topology"
              value={topology}
              onChange={setTopology}
              options={[
                { value: "default", label: "Default City Grid" },
                { value: "highway", label: "Highway Intersections" },
              ]}
            />
            <button className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs">
              Compute Max Flow
            </button>
          </div>
        }
        visualization={
          <div className="text-center text-white/50 border-2 border-dashed border-blue-500/50 bg-blue-500/5 p-12 rounded-xl w-full flex flex-col items-center justify-center h-[500px]">
            <span className="font-mono text-xl md:text-3xl uppercase tracking-widest text-white mb-6 font-black">
              Live Matrix Panel
            </span>
            <p className="text-sm md:text-base max-w-xl mb-8 leading-relaxed">
              <strong className="text-blue-400 block mb-2 text-lg">
                ATTENTION TEAM MEMBERS:
              </strong>
              Work on the UI from this point forward. Remove this placeholder
              box and integrate the live graphical visualization of the{" "}
              <span className="text-white font-bold">
                Traffic Simulation (Max Flow)
              </span>{" "}
              algorithm executing in real-time here.
            </p>
            <div className="px-8 py-3 bg-black/50 rounded-full border border-white/20 text-sm font-mono flex items-center gap-3 shadow-xl">
              <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
              // TODO: Integrate Java/Python backend here
            </div>
          </div>
        }
        results={
          <div className="flex flex-col gap-2 font-mono text-[10px] text-white/50">
            <p className="text-white">
              Status: <span className="text-yellow-500">Idle</span>
            </p>
            <p>Peak Capacity: -- vpm</p>
            <p>Bottleneck Nodes: None detected</p>
          </div>
        }
      />
    </AuthGate>
  );
}
