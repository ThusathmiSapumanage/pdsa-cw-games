"use client";

import React, { useState, useEffect } from 'react';
import Link from 'next/link';

export default function Header() {
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setScrolled(window.scrollY > 50);
        };
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    return (
        <header className={`fixed top-0 left-0 w-full z-50 transition-all duration-500 ${scrolled ? 'bg-black/60 backdrop-blur-md py-4' : 'bg-transparent py-8'
            }`}>
            {/* Top Border Line - Fades out on scroll */}
            <div className={`w-full h-[1px] bg-white/10 transition-opacity duration-500 ${scrolled ? 'opacity-0' : 'opacity-100'
                }`} />

            <div className="max-w-7xl mx-auto px-12 flex items-center justify-between">
                {/* Left Side: Logo & Dots */}
                <div className="flex items-center gap-10">
                    <Link href="/" className="text-xl font-bold tracking-[0.3em] hover:text-blue-500 transition-colors uppercase text-white">
                        ALGO<span className="text-blue-600">CORE</span>
                    </Link>
                    <div className="hidden sm:flex gap-2 opacity-20">
                        {[1, 2, 3].map((i) => (
                            <div key={i} className="w-1 h-1 rounded-full bg-white" />
                        ))}
                    </div>
                </div>

                {/* Center: Navigation */}
                <nav className="hidden lg:flex items-center gap-10 text-[11px] font-bold tracking-[0.2em] uppercase">
                    <Link href="#game-hub" className="text-white/40 hover:text-white transition-colors">Algorithms</Link>
                    <span className="text-white/10">|</span>
                    <Link href="#documentation" className="text-white/40 hover:text-white transition-colors">Documentation</Link>
                    <span className="text-white/10">|</span>
                    <Link href="#contact" className="text-white/40 hover:text-white transition-colors">Contact</Link>
                </nav>

                {/* Right Side: Account */}
                <div className="flex items-center gap-3 group cursor-pointer text-white">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-white/60 group-hover:text-blue-500 transition-colors">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                    <span className="text-[11px] font-black tracking-widest uppercase group-hover:text-blue-500 transition-colors">
                        Profile
                    </span>
                </div>
            </div>
        </header>
    );
}
