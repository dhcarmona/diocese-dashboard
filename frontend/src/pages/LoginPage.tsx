import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import Link from '@mui/material/Link';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  getRetryAfterMinutes,
  isBackendUnavailableError,
  isTooManyRequestsError,
  isUnauthorizedError,
  requestReporterOtp,
} from '../api/auth';
import { useAuth } from '../auth/auth-context';
import LanguageSwitcher from '../components/LanguageSwitcher';
import AppFooter from '../components/AppFooter';

type LoginMode = 'admin' | 'reporterRequest' | 'reporterVerify';

type LoginErrorKey =
  | 'login.invalidCredentials'
  | 'login.genericError'
  | 'login.invalidCode'
  | 'login.lockedOut'
  | 'login.otpGenericError'
  | 'auth.backendUnavailable';

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTo = searchParams.get('redirect');
  const { t } = useTranslation();
  const { signIn, reporterSignIn, status, authErrorKey } = useAuth();
  const [loginMode, setLoginMode] = useState<LoginMode>('reporterRequest');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorKey, setErrorKey] = useState<LoginErrorKey | null>(null);
  const [retryAfterMinutes, setRetryAfterMinutes] = useState<number | null>(null);

  function switchToReporter() {
    setLoginMode('reporterRequest');
    setPassword('');
    setOtpCode('');
    setErrorKey(null);
    setRetryAfterMinutes(null);
  }

  function switchToAdmin() {
    setLoginMode('admin');
    setOtpCode('');
    setErrorKey(null);
    setRetryAfterMinutes(null);
  }

  async function handleAdminSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorKey(null);
    setRetryAfterMinutes(null);
    setLoading(true);
    try {
      await signIn(username, password);
      navigate(redirectTo ?? '/');
    } catch (error) {
      if (isTooManyRequestsError(error)) {
        setErrorKey('login.lockedOut');
        setRetryAfterMinutes(getRetryAfterMinutes(error) ?? 1);
      } else if (isUnauthorizedError(error)) {
        setErrorKey('login.invalidCredentials');
      } else if (isBackendUnavailableError(error)) {
        setErrorKey('auth.backendUnavailable');
      } else {
        setErrorKey('login.genericError');
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleRequestOtp(e: FormEvent) {
    e.preventDefault();
    setErrorKey(null);
    setRetryAfterMinutes(null);
    setLoading(true);
    try {
      await requestReporterOtp(username);
    } catch (error) {
      if (isBackendUnavailableError(error)) {
        setErrorKey('auth.backendUnavailable');
        return;
      } else if (!isUnauthorizedError(error)) {
        setErrorKey('login.otpGenericError');
        return;
      }
    } finally {
      setLoading(false);
    }
    setLoginMode('reporterVerify');
  }

  async function handleVerifyOtp(e: FormEvent) {
    e.preventDefault();
    setErrorKey(null);
    setRetryAfterMinutes(null);
    setLoading(true);
    try {
      await reporterSignIn(username, otpCode);
      navigate(redirectTo ?? '/');
    } catch (error) {
      if (isTooManyRequestsError(error)) {
        setErrorKey('login.lockedOut');
        setRetryAfterMinutes(getRetryAfterMinutes(error) ?? 1);
      } else if (isUnauthorizedError(error)) {
        setErrorKey('login.invalidCode');
      } else if (isBackendUnavailableError(error)) {
        setErrorKey('auth.backendUnavailable');
      } else {
        setErrorKey('login.otpGenericError');
      }
    } finally {
      setLoading(false);
    }
  }

  const modeIcon = loginMode === 'admin' ? <LockOutlinedIcon /> : <SendOutlinedIcon />;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Box sx={{ display: 'flex', flex: 1 }}>

      {/* ── Left branding panel ── */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          width: '52%',
          flexShrink: 0,
          background: 'linear-gradient(155deg, #1C3A6E 0%, #0F2144 100%)',
          p: 8,
          gap: 4,
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Subtle red arc accent */}
        <Box sx={{
          position: 'absolute',
          width: 420,
          height: 420,
          borderRadius: '50%',
          border: '2px solid rgba(185,28,28,0.25)',
          top: -80,
          right: -120,
          pointerEvents: 'none',
        }} />
        <Box sx={{
          position: 'absolute',
          width: 280,
          height: 280,
          borderRadius: '50%',
          border: '1px solid rgba(185,28,28,0.15)',
          bottom: -40,
          left: -60,
          pointerEvents: 'none',
        }} />

        <Box
          component="img"
          src="/logo.png"
          alt={t('common.appName')}
          sx={{ width: 160, height: 160, objectFit: 'contain', filter: 'drop-shadow(0 4px 24px rgba(0,0,0,0.4))' }}
        />
        <Box sx={{ textAlign: 'center', maxWidth: 340 }}>
          <Typography variant="h4" fontWeight={700} color="white" sx={{ mb: 1.5, lineHeight: 1.25 }}>
            {t('common.appName')}
          </Typography>
          <Typography variant="body1" sx={{ color: 'rgba(255,255,255,0.65)', lineHeight: 1.7 }}>
            {t('login.subtitle')}
          </Typography>
        </Box>
      </Box>

      {/* ── Right form panel ── */}
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: 'background.default',
          px: { xs: 2, sm: 4 },
          py: 6,
          position: 'relative',
        }}
      >
        <Box sx={{ position: 'absolute', top: 16, right: 16 }}>
          <LanguageSwitcher />
        </Box>

        {/* Mobile-only logo strip */}
        <Box sx={{ display: { xs: 'flex', md: 'none' }, alignItems: 'center', gap: 1.5, mb: 4 }}>
          <Box
            component="img"
            src="/logo.png"
            alt={t('common.appName')}
            sx={{ height: 48, objectFit: 'contain' }}
          />
        </Box>

        <Card sx={{ width: '100%', maxWidth: 400, p: 1 }} elevation={3}>
          <CardContent>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3 }}>
              <Box sx={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                width: 40, height: 40, borderRadius: '50%',
                bgcolor: 'primary.main', color: 'white', flexShrink: 0,
              }}>
                {modeIcon}
              </Box>
              <Box>
                <Typography variant="subtitle1" fontWeight={700} color="text.primary"
                  sx={{ lineHeight: 1.2 }}>
                  {loginMode === 'admin' ? t('login.subtitle') : t('login.reporterSubtitle')}
                </Typography>
              </Box>
            </Box>

            {status === 'error' && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {t(authErrorKey ?? 'auth.sessionLoadFailed')}
              </Alert>
            )}

            {errorKey && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {errorKey === 'login.lockedOut'
                  ? t(errorKey, { count: retryAfterMinutes ?? 1, minutes: retryAfterMinutes ?? 1 })
                  : t(errorKey)}
              </Alert>
            )}

            {loginMode === 'reporterVerify' && !errorKey && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {t('login.codeSent', { username })}
              </Alert>
            )}

            {loginMode === 'admin' && (
              <Box component="form" onSubmit={(e) => void handleAdminSubmit(e)} noValidate>
                <TextField
                  label={t('login.username')}
                  fullWidth
                  required
                  margin="normal"
                  autoComplete="username"
                  autoFocus
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={loading}
                />
                <TextField
                  label={t('login.password')}
                  type="password"
                  fullWidth
                  required
                  margin="normal"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={loading}
                />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  sx={{ mt: 3 }}
                  disabled={loading || !username || !password}
                >
                  {loading ? <CircularProgress size={24} color="inherit" /> : t('login.signIn')}
                </Button>
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <Link component="button" type="button" variant="body2" onClick={switchToReporter}>
                    {t('login.switchToReporter')}
                  </Link>
                </Box>
              </Box>
            )}

            {loginMode === 'reporterRequest' && (
              <Box component="form" onSubmit={(e) => void handleRequestOtp(e)} noValidate>
                <TextField
                  label={t('login.username')}
                  fullWidth
                  required
                  margin="normal"
                  autoComplete="username"
                  autoFocus
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={loading}
                />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  sx={{ mt: 3 }}
                  disabled={loading || !username}
                >
                  {loading
                    ? <CircularProgress size={24} color="inherit" />
                    : t('login.sendCode')}
                </Button>
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <Link component="button" type="button" variant="body2" onClick={switchToAdmin}>
                    {t('login.switchToAdmin')}
                  </Link>
                </Box>
              </Box>
            )}

            {loginMode === 'reporterVerify' && (
              <Box component="form" onSubmit={(e) => void handleVerifyOtp(e)} noValidate>
                <TextField
                  label={t('login.otpLabel')}
                  placeholder={t('login.otpPlaceholder')}
                  fullWidth
                  required
                  margin="normal"
                  autoComplete="one-time-code"
                  autoFocus
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value)}
                  disabled={loading}
                />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  size="large"
                  sx={{ mt: 3 }}
                  disabled={loading || !otpCode}
                >
                  {loading
                    ? <CircularProgress size={24} color="inherit" />
                    : t('login.verifyCode')}
                </Button>
                <Box sx={{ mt: 2, textAlign: 'center' }}>
                  <Link
                    component="button"
                    type="button"
                    variant="body2"
                    onClick={() => {
                      setLoginMode('reporterRequest');
                      setOtpCode('');
                      setErrorKey(null);
                      setRetryAfterMinutes(null);
                    }}
                  >
                    {t('login.tryDifferentUsername')}
                  </Link>
                </Box>
              </Box>
            )}
          </CardContent>
        </Card>
      </Box>
      </Box>
      <AppFooter showBuildInfo={false} />
    </Box>
  );
}

