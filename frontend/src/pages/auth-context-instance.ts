import { createContext } from "react";
import type { AuthContext } from "./AuthContext";

const AuthContextInstance = createContext<AuthContext | null>(null);

export { AuthContextInstance };
