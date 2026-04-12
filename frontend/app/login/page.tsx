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
          : "Unable to reach server. Please try again."
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
          `Registration succeeded, but automatic login failed: ${loginMessage}`
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
          : "Unable to reach server. Please try again."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white flex items-center justify-center px-6 py-24 relative">

      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(37,99,235,0.18),transparent_55%)]" />

      <div className="relative z-10 w-full max-w-5xl grid grid-cols-1 lg:grid-cols-[1.12fr_1fr] lg:min-h-[620px] gap-10 bg-[#070707] border border-white/5 rounded-2xl overflow-hidden">

        {/* LEFT PANEL */}
        <div className="p-10 lg:p-12 h-full flex flex-col border-b lg:border-b-0 lg:border-r border-white/5">

          <p className="text-[10px] font-mono uppercase tracking-[0.4em] text-blue-500 mb-6">
            User Access
          </p>

          <h1 className="text-[clamp(1.7rem,3.6vw,3.1rem)] leading-[0.95] font-black uppercase mb-4">
            ALGOCORE
          </h1>

          <p className="text-[11px] font-mono uppercase tracking-[0.35em] text-white/40 mb-6">
            Account Access
          </p>

          <p className="text-sm text-white/50">
            Secure access to algorithm simulations. Login or register to continue.
          </p>

          <div className="mt-10 flex gap-4">

            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => handleTabChange(tab.id)}
                className={`px-5 py-2 rounded-full text-[10px] font-black uppercase tracking-widest border ${
                  activeTab === tab.id
                    ? "bg-blue-600 text-white border-blue-500"
                    : "text-white/60 border-white/10 hover:text-white"
                }`}
              >
                {tab.label}
              </button>
            ))}

          </div>

          {error && (
            <div className="mt-6 border border-red-500/30 bg-red-500/10 text-red-300 text-xs px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          <div className="mt-auto pt-10 text-[10px] text-white/30">
            Back to{" "}
            <Link href="/" className="text-blue-500 hover:text-white">
              dashboard
            </Link>
          </div>
        </div>

        {/* RIGHT PANEL */}
        <div className="p-10 lg:p-12 flex flex-col">

          {activeTab === "login" ? (

            <form onSubmit={handleLogin} className="flex flex-col gap-6 flex-1">

              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <input
                type={showPassword ? "text" : "password"}
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <button
                type="submit"
                disabled={isSubmitting || !isLoginReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold"
              >
                {isSubmitting ? "Logging In..." : "Login"}
              </button>

            </form>

          ) : (

            <form onSubmit={handleRegister} className="flex flex-col gap-6 flex-1">

              <input
                type="text"
                placeholder="Player Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <input
                type="password"
                placeholder="Confirm Password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="border-b border-white/10 py-3 bg-transparent"
              />

              <button
                type="submit"
                disabled={isSubmitting || !isRegisterReady}
                className="mt-auto bg-white text-black py-3 rounded font-bold"
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