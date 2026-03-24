import { createContext, useContext } from 'react';
import type { AuthenticatedUser } from '../api/auth';

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error';
export type AuthErrorKey = 'auth.sessionLoadFailed' | 'auth.backendUnavailable';

export interface AuthContextValue {
  user: AuthenticatedUser | null;
  status: AuthStatus;
  authErrorKey: AuthErrorKey | null;
  signIn: (username: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  refreshUser: () => Promise<AuthenticatedUser | null>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider.');
  }
  return context;
}
