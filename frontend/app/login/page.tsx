"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "../../components/auth/AuthProvider";
import {
  loginPlayer,
  registerPlayer,
} from "../../services/api";

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
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isLoginReady = email.trim().length > 0 && password.trim().length > 0;
  const isRegisterReady =
    fullName.trim().length > 0 &&
    email.trim().length > 0 &&
    password.trim().length > 0 &&
    confirmPassword.trim().length > 0;

  const nextPath = useMemo(() => {
    const next = searchParams?.get("next");
    return next && next.startsWith("/") ? next : "/";
  }, [searchParams]);

  useEffect(() => {
    if (isReady && isAuthenticated) {
      router.replace(nextPath);
    }
  }, [isReady, isAuthenticated, router, nextPath]);

  const handleTabChange = (tabId: TabId) => {
    setActiveTab(tabId);
    setEmail("");
    setPassword("");
    setFullName("");
    setConfirmPassword("");
    setShowPassword(false);
    setError(null);
  };

  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!email || !password) {
      setError("Please provide your email and password.");
      return;
    }
    setError(null);
    setIsSubmitting(true);

    try {
      const data = await loginPlayer(email, password);

      if (!data.accessToken) {
        setError("Sign-in failed. Invalid server response.");
        return;
      }

      login(email, data.accessToken);
      router.replace(nextPath);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to reach server. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRegister = async (event: React.FormEvent) => {
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
    setIsSubmitting(true);

    try {
      await registerPlayer(fullName, email, password);

      let loginData;
      try {
        loginData = await loginPlayer(email, password);
      } catch (loginErr) {
        const loginMessage =
          loginErr instanceof Error ? loginErr.message : "Unknown error";
        setError(
          `Registration succeeded, but automatic login failed: ${loginMessage}`,
        );
        return;
      }

      if (!loginData.accessToken) {
        setError("Registration succeeded, but login token was missing.");
        return;
      }

      login(email, loginData.accessToken);
      router.replace(nextPath);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to reach server. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white flex items-center justify-center px-6 py-24 relative">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(37,99,235,0.18),transparent_55%)]" />

      <div className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-[1.12fr_1fr] lg:min-h-[620px] gap-10 bg-[#070707] border border-white/5 rounded-2xl overflow-hidden">
        <div className="p-10 lg:p-12 h-full flex flex-col border-b lg:border-b-0 lg:border-r border-white/5">
          <p className="text-[10px] font-mono uppercase tracking-[0.4em] text-blue-500 mb-6">
            User Access
          </p>
          <h1 className="text-[clamp(1.7rem,3.6vw,3.1rem)] leading-[0.95] font-black tracking-normal uppercase mb-4 max-w-full overflow-hidden">
            ALGOCORE
          </h1>
          <p className="text-[11px] font-mono uppercase tracking-[0.35em] text-white/40 mb-6">
            Account Access
          </p>
          <p className="text-sm text-white/50 leading-relaxed">
            Secure access to algorithm simulations. Login or register to continue.
          </p>

          <div className="mt-10 flex items-center gap-4">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => handleTabChange(tab.id)}
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

          <div className="mt-auto pt-10 text-[10px] font-mono uppercase tracking-[0.3em] text-white/30">
            Back to{" "}
            <Link
              href="/"
              className="text-blue-500 hover:text-white transition-colors"
            >
              dashboard
            </Link>
          </div>
        </div>

        <div className="p-10 lg:p-12 h-full flex flex-col">
          {activeTab === "login" ? (
            <form noValidate onSubmit={handleLogin} className="flex flex-col gap-6 flex-1 min-h-[420px]">
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
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Password
                </label>
                <div className="relative mt-2">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-transparent border-b border-white/10 py-3 pr-12 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                    disabled={isSubmitting}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    className="absolute right-0 top-1/2 -translate-y-1/2 text-[10px] font-bold uppercase tracking-widest text-white/40 hover:text-white transition-colors disabled:opacity-40"
                  >
                    {showPassword ? "Hide" : "View"}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting || !isLoginReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {isSubmitting ? "Logging In..." : "Login"}
              </button>
            </form>
          ) : (
            <form noValidate onSubmit={handleRegister} className="flex flex-col gap-6 flex-1 min-h-[420px]">
              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Player Name
                </label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  placeholder="Algorithm Explorer"
                  className="mt-2 w-full bg-transparent border-b border-white/10 py-3 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                  disabled={isSubmitting}
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
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Password
                </label>
                <div className="relative mt-2">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Create a password"
                    className="w-full bg-transparent border-b border-white/10 py-3 pr-12 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                    disabled={isSubmitting}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    className="absolute right-0 top-1/2 -translate-y-1/2 text-[10px] font-bold uppercase tracking-widest text-white/40 hover:text-white transition-colors disabled:opacity-40"
                  >
                    {showPassword ? "Hide" : "View"}
                  </button>
                </div>
                <p className="mt-2 text-[10px] font-mono uppercase tracking-[0.2em] text-white/30 leading-relaxed">
                  Password must be at least 6 characters.
                </p>
              </div>

              <div>
                <label className="text-[10px] font-mono uppercase tracking-[0.3em] text-white/40">
                  Confirm Password
                </label>
                <div className="relative mt-2">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    placeholder="Repeat password"
                    className="w-full bg-transparent border-b border-white/10 py-3 pr-12 text-white placeholder:text-white/20 focus:outline-none focus:border-blue-500"
                    disabled={isSubmitting}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((prev) => !prev)}
                    disabled={isSubmitting}
                    className="absolute right-0 top-1/2 -translate-y-1/2 text-[10px] font-bold uppercase tracking-widest text-white/40 hover:text-white transition-colors disabled:opacity-40"
                  >
                    {showPassword ? "Hide" : "View"}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting || !isRegisterReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold uppercase tracking-widest hover:bg-blue-600 hover:text-white transition-colors text-xs disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {isSubmitting ? "Registering..." : "Register"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
