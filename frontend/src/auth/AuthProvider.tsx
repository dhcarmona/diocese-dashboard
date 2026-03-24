import type { ReactNode } from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  fetchAuthenticatedUser,
  isBackendUnavailableError,
  type AuthenticatedUser,
  login,
  logout,
} from '../api/auth';
import { AuthContext, type AuthContextValue, type AuthStatus } from './auth-context';

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [authErrorKey, setAuthErrorKey] = useState<AuthContextValue['authErrorKey']>(null);

  const refreshUser = useCallback(async () => {
    setStatus('loading');
    setAuthErrorKey(null);
    try {
      const currentUser = await fetchAuthenticatedUser();
      setUser(currentUser);
      setStatus(currentUser ? 'authenticated' : 'unauthenticated');
      return currentUser;
    } catch (error) {
      setUser(null);
      setStatus('error');
      setAuthErrorKey(
        isBackendUnavailableError(error) ? 'auth.backendUnavailable' : 'auth.sessionLoadFailed',
      );
      return null;
    }
  }, []);

  const signIn = useCallback(
    async (username: string, password: string) => {
      setAuthErrorKey(null);
      await login(username, password);
      const currentUser = await fetchAuthenticatedUser();
      if (!currentUser) {
        setUser(null);
        setStatus('unauthenticated');
        throw new Error('Authenticated user could not be loaded after login.');
      }
      setUser(currentUser);
      setStatus('authenticated');
    },
    [],
  );

  const signOut = useCallback(async () => {
    await logout();
    setUser(null);
    setStatus('unauthenticated');
    setAuthErrorKey(null);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadInitialUser() {
      try {
        const currentUser = await fetchAuthenticatedUser();
        if (!active) {
          return;
        }
        setUser(currentUser);
        setStatus(currentUser ? 'authenticated' : 'unauthenticated');
        setAuthErrorKey(null);
      } catch (error) {
        if (!active) {
          return;
        }
        setUser(null);
        setStatus('error');
        setAuthErrorKey(
          isBackendUnavailableError(error) ? 'auth.backendUnavailable' : 'auth.sessionLoadFailed',
        );
      }
    }

    void loadInitialUser();

    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      authErrorKey,
      signIn,
      signOut,
      refreshUser,
    }),
    [authErrorKey, refreshUser, signIn, signOut, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
