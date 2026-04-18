"use client";

import React from "react";
import Footer from "@/components/Footer";
import GameHub from "@/components/GameHub";

export default function Home() {
  return (
    <div className="bg-[#050505] min-h-screen selection:bg-blue-600/30 selection:text-white relative overflow-hidden">
      <main>
        <section className="relative border-b border-white/10 px-6 pb-20 pt-20 md:px-12 md:pt-24">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(37,99,235,0.14),transparent_30%),radial-gradient(circle_at_bottom_left,rgba(255,255,255,0.05),transparent_18%)]" />

          <div className="relative mx-auto max-w-[1700px]">
            <div className="grid gap-10 lg:grid-cols-[1.3fr_0.7fr] lg:items-end">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.35em] text-blue-500">
                  Home
                </p>
                <h1 className="mt-5 text-5xl font-black uppercase tracking-tight md:text-7xl">
                  Ready to Analyze?
                </h1>
                <p className="mt-6 max-w-3xl text-base leading-8 text-white/45 md:text-lg">
                  Explore the modules below to run simulations, compare
                  algorithm behavior, and prepare clean outputs for your report
                  and presentation.
                </p>

                <div className="mt-8 flex flex-wrap gap-4">
                  <div className="rounded-2xl border border-white/10 bg-white/[0.03] px-5 py-4">
                    <p className="text-[10px] uppercase tracking-[0.25em] text-white/30">
                      Modules
                    </p>
                    <p className="mt-2 text-sm font-bold text-white/80">5 Available</p>
                  </div>

                  <div className="rounded-2xl border border-white/10 bg-white/[0.03] px-5 py-4">
                    <p className="text-[10px] uppercase tracking-[0.25em] text-white/30">
                      Focus
                    </p>
                    <p className="mt-2 text-sm font-bold text-white/80">Algorithm Analysis</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

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
