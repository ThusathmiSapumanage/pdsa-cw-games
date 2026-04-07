import React from 'react';
import Link from 'next/link';

const games = [
    {
        id: "01",
        title: "Minimum Cost Problem",
        description: "Optimal task-to-employee assignment using advanced mapping algorithms. Minimizes total operational expenditure.",
        tag: "ASSIGNMENT",
        link: "/games/minimum-cost",
        metric: "50-100",
        unit: "tasks"
    },
    {
        id: "02",
        title: "Snake and Ladder",
        description: "Calculation of minimum dice throws required to navigate a dynamic board. Analyzes pathfinding efficiency.",
        tag: "STOCHASTIC",
        link: "/games/snake-ladder",
        metric: "N²",
        unit: "cells"
    },
    {
        id: "03",
        title: "Traffic Simulation",
        description: "Maximum vehicle throughput analysis across a directed network. Solves for peak capacity flow.",
        tag: "MAX-FLOW",
        link: "/games/traffic-simulation",
        metric: "15",
        unit: "vpm"
    },
    {
        id: "04",
        title: "Knight's Tour",
        description: "Complete chessboard traversal logic. Ensures the knight visits every square exactly once without overlap.",
        tag: "BACKTRACKING",
        link: "/games/knights-tour",
        metric: "256",
        unit: "sq"
    },
    {
        id: "05",
        title: "Sixteen Queens Puzzle",
        description: "Parallel vs Sequential execution comparison for solving non-threatening queen placements on large boards.",
        tag: "CONCURRENCY",
        link: "/games/sixteen-queens",
        metric: "16x16",
        unit: "grid"
    }
];

export default function GameHub() {
    return (
        <section id="game-hub" className="w-full bg-[#050505] py-40 px-12 relative z-10 text-white">
            <div className="max-w-[1800px] mx-auto">

                {/* Section Header */}
                <div className="flex flex-col md:flex-row justify-between items-end mb-24 border-b border-white/10 pb-10">
                    <div>
                        <span className="text-blue-500 font-bold text-xs tracking-[0.3em] mb-4 block">CORE ALGORITHMS</span>
                        <h2 className="text-5xl md:text-7xl font-black tracking-tighter uppercase">ALGOCORE <span className="text-blue-600">v1.1</span></h2>
                    </div>
                    <p className="text-white/40 max-w-md text-right md:text-left mt-8 md:mt-0 font-mono text-[10px] tracking-widest leading-relaxed uppercase">
                        PDSA COURSEWORK SOLUTION.<br />
                        BSc (HONS) COMPUTING - 25.2
                    </p>
                </div>

                {/* Games Grid */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-x-20 gap-y-20">
                    {games.map((game) => (
                        <Link href={game.link} key={game.id} className="group border-t border-white/10 pt-10 hover:border-blue-500 transition-all duration-500 block">
                            <div className="flex justify-between items-start mb-6">
                                <span className="text-white/30 font-mono text-sm">{game.id}</span>
                                <div className="flex items-center gap-4">
                                    <span className="text-blue-500 font-bold text-[10px] tracking-[0.2em] opacity-0 group-hover:opacity-100 transition-opacity uppercase">{game.tag}</span>
                                    <div className="h-2 w-2 bg-blue-500 rounded-full opacity-0 group-hover:opacity-100 transition-opacity" />
                                </div>
                            </div>
                            <h3 className="text-4xl md:text-5xl font-bold mb-6 group-hover:text-blue-500 transition-colors uppercase tracking-tighter">{game.title}</h3>
                            <p className="text-white/50 leading-relaxed mb-10 max-w-md text-sm md:text-base">
                                {game.description}
                            </p>
                            <div className="text-8xl font-black tracking-tighter text-white/5 group-hover:text-blue-600/10 transition-all duration-700">
                                {game.metric}<span className="text-3xl ml-2 opacity-50">{game.unit}</span>
                            </div>
                        </Link>
                    ))}
                </div>

                {/* Footer CTA */}
                <div className="mt-40 pt-10 border-t border-white/10 text-center">
                    <p className="text-[10px] font-mono text-white/20 tracking-[0.5em] uppercase">Algorithm Suite calibrated for NIBM Academic standards</p>
                </div>
            </div>
        </section>
    );
}
