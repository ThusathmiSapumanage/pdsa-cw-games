"use client";

import { useState } from "react";
import GameLayoutShell from "../../../components/layouts/GameLayoutShell";
import AlgoDropdown from "../../../components/forms/AlgoDropdown";

export default function SixteenQueensPage() {
    const [threading, setThreading] = useState("sequential");

    return (
        <GameLayoutShell
            gameId="05"
            title="Sixteen Queens Puzzle"
            description="Parallel vs Sequential execution comparison for solving non-threatening queen placements on large boards."
            controls={
                <div className="flex flex-col gap-4">
                    <AlgoDropdown 
                        label="Execution Threading" 
                        value={threading} 
                        onChange={setThreading} 
                        options={[
                            { value: "sequential", label: "Sequential (Single Thread)" },
                            { value: "concurrent", label: "Concurrent (Multi-Threading)" }
                        ]} 
                    />
                    <button className="bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs">
                        Deploy Queens
                    </button>
                </div>
            }
            visualization={
                <div className="text-center text-white/50 border-2 border-dashed border-blue-500/50 bg-blue-500/5 p-12 rounded-xl w-full flex flex-col items-center justify-center h-[500px]">
                    <span className="font-mono text-xl md:text-3xl uppercase tracking-widest text-white mb-6 font-black">Live Matrix Panel</span>
                    <p className="text-sm md:text-base max-w-xl mb-8 leading-relaxed">
                        <strong className="text-blue-400 block mb-2 text-lg">ATTENTION TEAM MEMBERS:</strong> 
                        Work on the UI from this point forward. Remove this placeholder box and integrate the live graphical visualization of the <span className="text-white font-bold">Sixteen Queens (Concurrency)</span> algorithm executing in real-time here.
                    </p>
                    <div className="px-8 py-3 bg-black/50 rounded-full border border-white/20 text-sm font-mono flex items-center gap-3 shadow-xl">
                        <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
                        // TODO: Integrate Java/Python backend here
                    </div>
                </div>
            }
            results={
                <div className="flex flex-col gap-2 font-mono text-[10px] text-white/50">
                    <p className="text-white">Status: <span className="text-yellow-500">Awaiting Thread Selection</span></p>
                    <p>Valid Solutions: 0</p>
                    <p>Execution Compute Time: -- ms</p>
                </div>
            }
        />
    );
}
