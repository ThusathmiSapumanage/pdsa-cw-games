"use client";

import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

type AuthContextValue = {
  isAuthenticated: boolean;
  isReady: boolean;
  userEmail: string | null;
  accessToken: string | null;
  login: (_e: string, _t: string) => void;
  logout: () => void;
};

const STORAGE_KEY = "algocore_auth";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as { email?: string; token?: string };
        const email = parsed.email ?? null;
        const token = parsed.token ?? null;

        if (email && token) {
          setUserEmail(email);
          setAccessToken(token);
          setIsAuthenticated(true);
        } else {
          localStorage.removeItem(STORAGE_KEY);
        }
      }
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    } finally {
      setIsReady(true);
    }
  }, []);

  const login = (email: string, token: string) => {
    setIsAuthenticated(true);
    setUserEmail(email);
    setAccessToken(token);
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ email, token }));
  };

  const logout = () => {
    setIsAuthenticated(false);
    setUserEmail(null);
    setAccessToken(null);
    localStorage.removeItem(STORAGE_KEY);
  };

  const value = useMemo(
    () => ({ isAuthenticated, isReady, userEmail, accessToken, login, logout }),
    [isAuthenticated, isReady, userEmail, accessToken],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
