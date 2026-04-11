"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "../../components/auth/AuthProvider";

const tabs = [
  { id: "login", label: "Login" },
  { id: "register", label: "Register" },
] as const;

type TabId = (typeof tabs)[number]["id"];

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isAuthenticated, login, isReady } = useAuth();

  const [activeTab, setActiveTab] = useState<TabId>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const nextPath = useMemo(() => {
    const next = searchParams?.get("next");
    return next && next.startsWith("/") ? next : "/";
  }, [searchParams]);

  useEffect(() => {
    if (isReady && isAuthenticated) {
      router.replace(nextPath);
    }
  }, [isReady, isAuthenticated, router, nextPath]);

  const handleLogin = (event: React.FormEvent) => {
    event.preventDefault();
    if (!email || !password) {
      setError("Please provide your email and password.");
      return;
    }
    setError(null);
    login(email);
    router.replace(nextPath);
  };

  const handleRegister = (event: React.FormEvent) => {
    event.preventDefault();
    if (!fullName || !email || !password || !confirmPassword) {
      setError("Please complete all registration fields.");
      return;
    }
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    setError(null);
    login(email);
    router.replace(nextPath);
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white flex items-center justify-center px-6 py-24 relative">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(37,99,235,0.18),transparent_55%)]" />

      <div className="relative z-10 w-full max-w-4xl grid grid-cols-1 lg:grid-cols-2 gap-10 bg-[#070707] border border-white/5 rounded-2xl overflow-hidden">
        <div className="p-10 lg:p-12 border-b lg:border-b-0 lg:border-r border-white/5">
          <p className="text-[10px] font-mono uppercase tracking-[0.4em] text-blue-500 mb-6">
            User Access
          </p>
          <h1 className="text-4xl md:text-5xl font-black tracking-tighter uppercase mb-6">
            ALGOCORE Login
          </h1>
          <p className="text-sm text-white/50 leading-relaxed">
            Secure access to algorithm simulations. Login or create a quick
            profile to proceed.
          </p>

          <div className="mt-10 flex items-center gap-4">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setActiveTab(tab.id);
                  setError(null);
                }}
                className={`px-5 py-2 rounded-full text-[10px] font-black uppercase tracking-widest transition-colors border ${
                  activeTab === tab.id
                    ? "bg-blue-600 text-white border-blue-500"
                    : "text-white/60 border-white/10 hover:text-white hover:border-white/30"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {error && (
            <div className="mt-6 border border-red-500/30 bg-red-500/10 text-red-300 text-xs uppercase tracking-widest font-bold px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          <div className="mt-10 text-[10px] font-mono uppercase tracking-[0.3em] text-white/30">
            Back to{" "}
            <Link
              href="/"
              className="text-blue-500 hover:text-white transition-colors"
            >
              dashboard
            </Link>
          </div>
        </div>

        <div className="p-10 lg:p-12">
          {activeTab === "login" ? (
            <form onSubmit={handleLogin} className="flex flex-col gap-6">
              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Email
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@algocore.com"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Password
                </label>
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <button
                type="submit"
                className="mt-4 bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
              >
                Access System
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="flex flex-col gap-6">
              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Full Name
                </label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  placeholder="Algorithm Explorer"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Email
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@algocore.com"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Password
                </label>
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Create a password"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Confirm Password
                </label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="Repeat password"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                />
              </div>

              <button
                type="submit"
                className="mt-2 bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs"
              >
                Create Profile
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
