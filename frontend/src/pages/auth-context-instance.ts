import { createContext } from "react";

// NOTE: the auth boundary/login/signup UI lives in CLJS
// (knoxx.frontend.auth.*) and provides on this instance, which it receives
// through the app bridge. TS consumers keep reading it via useAuth.

export interface AuthContext {
  actor: {
    id: string;
  } | null;
  user: {
    id: string;
    email: string;
    username?: string;
    displayName: string;
    status: string;
  } | null;
  org: {
    id: string;
    slug: string;
    name: string;
    isPrimary: boolean;
  } | null;
  membership: {
    id: string;
    actorId?: string;
    status: string;
    isDefault: boolean;
  } | null;
  roleSlugs: string[];
  permissions: string[];
  isSystemAdmin: boolean;
  authProvider: string;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContextInstance = createContext<AuthContext | null>(null);

export { AuthContextInstance };
