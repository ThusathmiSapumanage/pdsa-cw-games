"use client";

import React from 'react';
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import GameHub from "@/components/GameHub";
import AlgoScrollHero from "@/components/AlgoScrollHero";

export default function Home() {
    return (
        <div className="bg-[#050505] min-h-screen selection:bg-blue-600/30 selection:text-white relative">

            {/* Nav */}
            <Header />

            <main>
                {/* Dynamic Scroll Hero Section */}
                <AlgoScrollHero />

                {/* Main Content Area */}
                <div className="relative z-10 pt-40">
                    <GameHub />
                </div>

                {/* Sub-Footer Documentation Link */}
                <section className="w-full bg-[#050505] py-40 border-t border-white/5 flex items-center justify-center">
                    <div className="max-w-4xl text-center px-6">
                        <h2 className="text-4xl md:text-6xl font-bold tracking-tighter uppercase mb-8">Ready to analyze?</h2>
                        <p className="text-white/40 text-lg md:text-xl font-medium max-w-2xl mx-auto mb-12">
                            Select an algorithm above to begin simulation. Each module is optimized for peak performance and follows strict standards for complexity analysis.
                        </p>
                        <button className="px-12 py-5 bg-white text-black font-black uppercase text-xs tracking-widest hover:bg-blue-600 hover:text-white transition-all duration-300 rounded-full">
                            Technical Repo
                        </button>
                    </div>
                </section>
            </main>

            {/* Footer */}
            <Footer />
        </div>
    );
}
