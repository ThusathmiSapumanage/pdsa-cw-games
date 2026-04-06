"use client";

import React, { useRef, useEffect, useState } from 'react';
import { motion, useScroll, useTransform, useMotionValueEvent, MotionValue, AnimatePresence, useSpring } from 'framer-motion';

const FRAME_COUNT = 360;

export default function AlgoScrollHero() {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);
    const [images, setImages] = useState<HTMLImageElement[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [loadingProgress, setLoadingProgress] = useState(0);
    const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
    const prevIndex = useRef<number>(-1);

    const { scrollYProgress } = useScroll({
        target: containerRef,
        offset: ["start start", "end end"]
    });

    const smoothProgress = useSpring(scrollYProgress, {
        stiffness: 100,
        damping: 30,
        restDelta: 0.001
    });

    // --- SCROLL LOCK LOGIC (400vh) ---
    const frameIndex = useTransform(smoothProgress, [0, 1], [0, FRAME_COUNT - 1]);

    // Handle Resize (Client Side Only)
    useEffect(() => {
        if (typeof window === "undefined") return;

        const handleResize = () => {
            setDimensions({
                width: window.innerWidth,
                height: window.innerHeight
            });
        };

        handleResize();
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    // Render Frame Logic
    const renderFrame = React.useCallback((index: number, canvas: HTMLCanvasElement, imgs: HTMLImageElement[]) => {
        const i = Math.min(Math.round(index), imgs.length - 1);
        if (i === prevIndex.current) return; // Prevent redundant drawing for 60+ FPS stability

        const ctx = canvas.getContext('2d');
        if (!ctx || imgs.length === 0) return;

        const img = imgs[i];
        if (!img || !img.complete) return;

        prevIndex.current = i; // Update rendered frame State

        const cw = canvas.width;
        const ch = canvas.height;
        const iw = img.naturalWidth || 1920;
        const ih = img.naturalHeight || 1080;

        const scale = Math.max(cw / iw, ch / ih);
        const dw = iw * scale;
        const dh = ih * scale;
        const dx = (cw - dw) / 2;
        const dy = (ch - dh) / 2;

        ctx.clearRect(0, 0, cw, ch);
        ctx.drawImage(img, dx, dy, dw, dh);
    }, []);

    // Update Canvas on Dimension Change
    useEffect(() => {
        const canvas = canvasRef.current;
        if (canvas && dimensions.width > 0) {
            canvas.width = dimensions.width;
            canvas.height = dimensions.height;
            if (images.length > 0) {
                const currentFrame = frameIndex.get();
                renderFrame(currentFrame, canvas, images);
            }
        }
    }, [dimensions, images, renderFrame]);

    // Preload Images
    useEffect(() => {
        const preload = async () => {
            if (typeof window === "undefined") return;

            const loadWithTimeout = (src: string, timeout = 5000) => {
                return new Promise<HTMLImageElement>((resolve) => {
                    const img = new Image();
                    img.src = src;
                    img.onload = () => resolve(img);
                    img.onerror = () => resolve(img);
                    setTimeout(() => resolve(img), timeout);
                });
            };

            const promises = [];
            let loadedCount = 0;

            for (let i = 0; i < FRAME_COUNT; i++) {
                const src = `/frames/frame-${i + 1}.webp`;
                promises.push(loadWithTimeout(src).then(img => {
                    loadedCount++;
                    setLoadingProgress(Math.round((loadedCount / FRAME_COUNT) * 100));
                    return img;
                }));
            }

            const results = await Promise.all(promises);
            setImages(results);
            setTimeout(() => setIsLoading(false), 500);
        };
        preload();
    }, []);

    // Sync Animation with Scroll
    useMotionValueEvent(frameIndex, "change", (latest) => {
        if (canvasRef.current && images.length > 0) {
            renderFrame(latest, canvasRef.current, images);
        }
    });

    // Initial Draw after Loading
    useEffect(() => {
        if (!isLoading && canvasRef.current && images.length > 0) {
            renderFrame(0, canvasRef.current, images);
        }
    }, [isLoading, images, renderFrame]);

    return (
        <div ref={containerRef} className="h-[400vh] relative z-20 bg-[#050505]">
            <div className="sticky top-0 h-screen w-full flex items-center justify-center overflow-hidden bg-[#050505]">

                <div className="absolute inset-0 bg-radial-gradient from-transparent via-black/40 to-black/90 pointer-events-none z-20" />

                {/* Watermark Cover Box (Premium HUD Element) */}
                <div className="absolute bottom-0 right-0 z-30 pointer-events-none select-none">
                    <div className="bg-[#050505] border-t border-l border-blue-500/20 px-6 py-4 flex flex-col items-end shadow-2xl rounded-tl-xl relative overflow-hidden">
                        {/* Decorative glass shine */}
                        <div className="absolute inset-0 bg-gradient-to-tr from-blue-500/[0.05] to-transparent pointer-events-none" />
                        
                        <div className="flex items-center gap-3 mb-1.5 relative z-10">
                            <div className="w-1.5 h-1.5 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.8)] animate-pulse" />
                            <span className="text-white font-black tracking-[0.4em] text-xs md:text-sm uppercase leading-none" style={{ textShadow: '0 2px 10px rgba(0,0,0,1)' }}>ALGOCORE</span>
                        </div>
                        <span className="text-white/40 font-bold tracking-[0.3em] text-[8px] uppercase relative z-10">
                            SYS: ONLINE // V_1.0
                        </span>
                    </div>
                </div>
                <AnimatePresence>
                    {isLoading && (
                        <motion.div
                            initial={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-[#050505]"
                        >
                            <span className="text-9xl font-bold text-white/5 select-none">{loadingProgress}%</span>
                        </motion.div>
                    )}
                </AnimatePresence>

                <canvas ref={canvasRef} className="block relative z-10" />

                <div className="absolute inset-0 pointer-events-none z-30">
                    {/* 1. Title */}
                    <TextOverlay progress={smoothProgress} range={[0.0, 0.15]} className="flex items-center justify-center">
                        <div className="text-left px-10">
                            <span className="text-white font-bold text-xs tracking-[0.5em] mb-8 block uppercase">
                                Computational Intelligence
                            </span>
                            <h1 className="text-6xl md:text-[10vw] leading-[0.85] font-black text-white mb-4 tracking-tighter mix-blend-difference uppercase">ALGOCORE</h1>
                            <div className="h-[1px] w-20 bg-blue-600 mb-4" />
                            <p className="text-white tracking-[0.8em] text-[10px] uppercase font-bold">PDSA Coursework Solution</p>
                        </div>
                    </TextOverlay>

                    {/* 2. Precision */}
                    <TextOverlay progress={smoothProgress} range={[0.20, 0.40]} className="flex items-center justify-end">
                        <div className="max-w-4xl pr-[5vw] text-left">
                            <span className="text-white font-bold text-sm mb-6 block tracking-[0.4em] uppercase">
                                01 / PERFORMANCE
                            </span>
                            <h2 className="text-7xl md:text-9xl font-bold text-white mb-8 tracking-tight uppercase leading-[0.9] mix-blend-difference">Optimized.</h2>
                            <p className="text-white text-2xl md:text-3xl leading-relaxed font-medium max-w-2xl">
                                Threaded processing. Low-level memory management. Built for maximum efficiency.
                            </p>
                        </div>
                    </TextOverlay>

                    {/* 3. Clarity */}
                    <TextOverlay progress={smoothProgress} range={[0.45, 0.65]} className="flex items-center justify-start">
                        <div className="max-w-4xl pl-[5vw] text-left">
                            <span className="text-white font-bold text-sm mb-6 block tracking-[0.4em] uppercase">
                                02 / ARCHITECTURE
                            </span>
                            <h2 className="text-7xl md:text-9xl font-bold text-white mb-8 tracking-tight uppercase leading-[0.9] mix-blend-difference">Robust.</h2>
                            <p className="text-white text-2xl md:text-3xl leading-relaxed font-medium max-w-2xl">
                                Five algorithms integrated into a single, cohesive, highly-validated pipeline.
                            </p>
                        </div>
                    </TextOverlay>

                    {/* 4. CTA */}
                    <TextOverlay progress={smoothProgress} range={[0.75, 0.90]} className="flex items-center justify-center w-full h-full">
                        <div className="text-center pointer-events-auto">
                            {/* Solid White Button for maximum contrast against cyan background */}
                            <a href="#game-hub" className="relative inline-flex items-center justify-center px-12 py-6 overflow-hidden font-bold text-black transition-all duration-300 bg-white border-2 border-white rounded-full hover:bg-black hover:text-white hover:border-black shadow-[0_0_50px_rgba(255,255,255,0.4)]">
                                <span className="relative text-xl tracking-[0.3em] font-black uppercase">Begin Simulation</span>
                            </a>
                        </div>
                        
                        {/* Animated standalone bouncing arrow - pinned to bottom */}
                        <div className="absolute bottom-[5vh] left-1/2 -translate-x-1/2 flex flex-col items-center gap-3 w-[min-content] min-w-max pointer-events-none">
                            <motion.div
                                animate={{ y: [0, 10, 0], opacity: [0.6, 1, 0.6] }}
                                transition={{ repeat: Infinity, duration: 1.5, ease: "easeInOut" }}
                                className="p-3 bg-white text-black rounded-full shadow-[0_0_20px_rgba(255,255,255,0.6)]"
                            >
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M19 14l-7 7m0 0l-7-7m7 7V3"></path>
                                </svg>
                            </motion.div>
                            <p className="text-white text-[10px] tracking-[0.4em] font-black uppercase bg-black/60 px-4 py-2 rounded-full backdrop-blur-md border border-white/20 whitespace-nowrap">
                                Scroll down to view modules
                            </p>
                        </div>
                    </TextOverlay>
                </div>
            </div>
        </div>
    );
}

function TextOverlay({ progress, range, children, className }: { progress: MotionValue<number>, range: [number, number], children: React.ReactNode, className?: string }) {
    const [start, end] = range;
    const fade = (end - start) * 0.15;
    const opacity = useTransform(progress, [start, start + fade, end - fade, end], [0, 1, 1, 0]);
    const y = useTransform(progress, [start, start + fade, end - fade, end], [50, 0, 0, -50]);

    return (
        <motion.div style={{ opacity, y }} className={`absolute inset-0 w-full h-full ${className}`}>
            {children}
        </motion.div>
    );
}
