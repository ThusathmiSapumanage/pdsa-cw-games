import React from 'react';
import Link from 'next/link';
import Image from 'next/image';

const games = [
    {
        id: "01",
        title: "Minimum Cost",
        tag: "ASSIGNMENT",
        link: "/games/minimum-cost",
        poster: "/posters/minimum-cost.png",
    },
    {
        id: "02",
        title: "Snake & Ladder",
        tag: "SHORTEST PATH",
        link: "/games/snake-ladder",
        poster: "/posters/snake-ladder.png",
    },
    {
        id: "03",
        title: "Traffic Simulation",
        tag: "MAX FLOW",
        link: "/games/traffic-simulation",
        poster: "/posters/traffic-simulation.png",
    },
    {
        id: "04",
        title: "Knight's Tour",
        tag: "BACKTRACKING",
        link: "/games/knights-tour",
        poster: "/posters/knights-tour.png",
    },
    {
        id: "05",
        title: "16 Queens Puzzle",
        tag: "CONCURRENCY",
        link: "/games/sixteen-queens",
        poster: "/posters/sixteen-queens.png",
    }
];

export default function GameHub() {
    return (
        <section id="game-hub" className="w-full bg-[#050505] py-40 px-6 md:px-12 relative z-10 text-white">
            <div className="max-w-[1800px] mx-auto">

                {/* Section Header */}
                <div className="flex flex-col md:flex-row justify-between items-end mb-16 border-b border-white/10 pb-10">
                    <div>
                        <span className="text-blue-500 font-bold text-xs tracking-[0.3em] mb-4 block">CORE ALGORITHMS</span>
                        <h2 className="text-5xl md:text-7xl font-black tracking-tighter uppercase">ALGOCORE <span className="text-blue-600">v1.1</span></h2>
                    </div>
                    <p className="text-white/40 max-w-md text-right md:text-left mt-8 md:mt-0 font-mono text-[10px] tracking-widest leading-relaxed uppercase">
                        PDSA COURSEWORK SOLUTION.<br />
                        BSc (HONS) COMPUTING - 25.2
                    </p>
                </div>

                {/* Games Grid — 5 cards in a row */}
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-6">
                    {games.map((game) => (
                        <Link href={game.link} key={game.id} className="group block">
                            {/* Poster */}
                            <div className="relative aspect-[3/4] rounded-lg overflow-hidden bg-[#0a0a0a] border border-white/5 group-hover:border-blue-500/40 transition-all duration-500">
                                <Image
                                    src={game.poster}
                                    alt={game.title}
                                    fill
                                    className="object-cover group-hover:scale-105 transition-transform duration-700"
                                    sizes="(max-width: 768px) 50vw, (max-width: 1024px) 33vw, 20vw"
                                />
                                {/* Hover overlay */}
                                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                                {/* Tag badge on hover */}
                                <span className="absolute bottom-3 left-3 text-[9px] font-bold tracking-[0.15em] uppercase text-blue-400 bg-blue-500/10 backdrop-blur-sm px-2 py-1 rounded opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                                    {game.tag}
                                </span>
                            </div>

                            {/* Title */}
                            <div className="mt-3">
                                <p className="text-[10px] font-mono text-white/30 tracking-widest uppercase mb-1">Game {game.id}</p>
                                <h3 className="text-sm font-bold text-white group-hover:text-blue-500 transition-colors duration-300 uppercase tracking-tight">
                                    {game.title}
                                </h3>
                            </div>
                        </Link>
                    ))}
                </div>

                {/* Footer CTA */}
                <div className="mt-24 pt-10 border-t border-white/10 text-center">
                    <p className="text-[10px] font-mono text-white/20 tracking-[0.5em] uppercase">Algorithm Suite calibrated for NIBM Academic standards</p>
                </div>
            </div>
        </section>
    );
}
