import { useContext } from "react";
import { AuthContextInstance } from "./auth-context-instance";
import type { AuthContext } from "./AuthContext";

export function useAuth(): AuthContext {
  const ctx = useContext(AuthContextInstance);
  if (!ctx) throw new Error("useAuth must be used within AuthBoundary");
  return ctx;
}
