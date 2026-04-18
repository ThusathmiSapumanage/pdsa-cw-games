"use client";

import React from "react";
import Footer from "@/components/Footer";
import GameHub from "@/components/GameHub";
import HeroShowcase from "@/components/HeroShowcase";

export default function Home() {
  return (
    <div className="bg-[#050505] min-h-screen selection:bg-blue-600/30 selection:text-white relative overflow-hidden">
      <main>
        <HeroShowcase />

        <GameHub />

        <section className="w-full bg-[#050505] py-28 border-t border-white/5 flex items-center justify-center">
          <div className="max-w-4xl text-center px-6">
            <h2 className="text-4xl md:text-6xl font-bold tracking-tighter uppercase mb-8">
              Technical Excellence
            </h2>
            <p className="text-white/40 text-lg md:text-xl font-medium max-w-2xl mx-auto mb-12">
              Each module is designed for clear demonstrations, cleaner
              comparisons, and smooth academic presentation.
            </p>
            <button className="px-12 py-5 bg-white text-black font-black uppercase text-xs tracking-widest hover:bg-blue-600 hover:text-white transition-all duration-300 rounded-full">
              Technical Repo
            </button>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
