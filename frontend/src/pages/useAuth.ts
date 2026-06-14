import { useContext } from "react";
import { AuthContextInstance, type AuthContext } from "./auth-context-instance";

export function useAuth(): AuthContext {
  const ctx = useContext(AuthContextInstance);
  if (!ctx) throw new Error("useAuth must be used within AuthBoundary");
  return ctx;
}
