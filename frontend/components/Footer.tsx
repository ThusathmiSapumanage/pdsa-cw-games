"use client";

import React from 'react';

export default function Footer() {
    return (
        <footer className="w-full bg-[#050505] pt-20 pb-10 border-t border-white/10 text-white relative overflow-hidden">
            {/* Background Gradient for subtle depth */}
            <div className="absolute inset-0 bg-radial-gradient from-blue-900/10 via-transparent to-transparent pointer-events-none" />

            <div className="max-w-[1800px] mx-auto px-6 md:px-12 relative z-10">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-32">

                    {/* Brand Column */}
                    <div className="col-span-1 md:col-span-2 flex flex-col justify-between">
                        <div>
                            <h2 className="text-2xl font-bold tracking-[0.2em] mb-8 uppercase">ALGO<span className="text-blue-600">CORE</span></h2>
                            <p className="text-white/40 max-w-sm leading-relaxed text-sm uppercase font-medium tracking-widest text-[10px]">
                                Advanced Algorithmic Solutions. <br />
                                Engineered for the next generation of computing excellence.
                            </p>
                        </div>
                    </div>

                    {/* Links Column 1 */}
                    <div>
                        <h3 className="font-bold mb-8 text-xs tracking-[0.2em] text-blue-500 uppercase">Navigation</h3>
                        <ul className="space-y-4 text-white/40 text-sm font-medium">
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                Dashboard
                            </li>
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                Algos
                            </li>
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                Reports
                            </li>
                        </ul>
                    </div>

                    {/* Links Column 2 */}
                    <div>
                        <h3 className="font-bold mb-8 text-xs tracking-[0.2em] text-blue-500 uppercase">Resources</h3>
                        <ul className="space-y-4 text-white/40 text-sm font-medium">
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                Documentation
                            </li>
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                Logic Docs
                            </li>
                            <li className="hover:text-white transition-colors cursor-pointer flex items-center gap-2 group uppercase text-[10px] tracking-widest">
                                <span className="w-0 group-hover:w-2 h-[1px] bg-blue-500 transition-all duration-300"></span>
                                API
                            </li>
                        </ul>
                    </div>
                </div>

                {/* Massive Footer Logo */}
                <div className="border-t border-white/10 pt-10 flex flex-col md:flex-row justify-between items-end">
                    <h1 className="text-[12vw] leading-[0.8] font-black tracking-tighter text-white/5 select-none pointer-events-none uppercase">
                        CORE
                    </h1>

                    <div className="flex flex-col md:flex-row gap-8 pb-4 text-[10px] text-white/30 font-mono uppercase tracking-widest">
                        <span>BSc (Hons) Computing - 25.2</span>
                        <span>PDSA Coursework</span>
                        <span className="text-white/50">&copy; 2026 All Rights Reserved.</span>
                    </div>
                </div>
            </div>
        </footer>
    );
}
