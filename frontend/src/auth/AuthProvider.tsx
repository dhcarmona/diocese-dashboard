import type { ReactNode } from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import i18n from '../i18n';
import {
  fetchAuthenticatedUser,
  isBackendUnavailableError,
  type AuthenticatedUser,
  login,
  logout,
  type PreferredLanguage,
  updatePreferredLanguage as savePreferredLanguage,
  verifyReporterOtp,
  redeemLoginToken,
} from '../api/auth';
import { AuthContext, type AuthContextValue, type AuthStatus } from './auth-context';

export function AuthProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [authErrorKey, setAuthErrorKey] = useState<AuthContextValue['authErrorKey']>(null);

  const syncUserLanguage = useCallback(async (currentUser: AuthenticatedUser | null) => {
    if (!currentUser || i18n.language === currentUser.preferredLanguage) {
      return;
    }
    await i18n.changeLanguage(currentUser.preferredLanguage);
  }, []);

  const refreshUser = useCallback(async () => {
    setStatus('loading');
    setAuthErrorKey(null);
    try {
      const currentUser = await fetchAuthenticatedUser();
      await syncUserLanguage(currentUser);
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
  }, [syncUserLanguage]);

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
      await syncUserLanguage(currentUser);
      setUser(currentUser);
      setStatus('authenticated');
    },
    [syncUserLanguage],
  );

  const reporterSignIn = useCallback(
    async (username: string, code: string) => {
      setAuthErrorKey(null);
      await verifyReporterOtp(username, code);
      const currentUser = await fetchAuthenticatedUser();
      if (!currentUser) {
        setUser(null);
        setStatus('unauthenticated');
        throw new Error('Authenticated user could not be loaded after OTP verification.');
      }
      await syncUserLanguage(currentUser);
      setUser(currentUser);
      setStatus('authenticated');
    },
    [syncUserLanguage],
  );

  const redeemToken = useCallback(
    async (token: string) => {
      setAuthErrorKey(null);
      await redeemLoginToken(token);
      const currentUser = await fetchAuthenticatedUser();
      if (!currentUser) {
        setUser(null);
        setStatus('unauthenticated');
        throw new Error('Authenticated user could not be loaded after token redemption.');
      }
      await syncUserLanguage(currentUser);
      setUser(currentUser);
      setStatus('authenticated');
    },
    [syncUserLanguage],
  );

  const signOut = useCallback(async () => {
    await logout();
    setUser(null);
    setStatus('unauthenticated');
    setAuthErrorKey(null);
  }, []);

  const updatePreferredLanguage = useCallback(
    async (language: PreferredLanguage) => {
      const updatedUser = await savePreferredLanguage(language);
      setUser(updatedUser);
    },
    [],
  );

  useEffect(() => {
    let active = true;

    async function loadInitialUser() {
      try {
        const currentUser = await fetchAuthenticatedUser();
        await syncUserLanguage(currentUser);
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
  }, [syncUserLanguage]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      authErrorKey,
      signIn,
      reporterSignIn,
      redeemToken,
      signOut,
      refreshUser,
      updatePreferredLanguage,
    }),
    [authErrorKey, redeemToken, refreshUser, reporterSignIn, signIn, signOut, status,
      updatePreferredLanguage, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
