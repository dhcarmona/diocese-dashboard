import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import Alert from '@mui/material/Alert';
import Avatar from '@mui/material/Avatar';
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
import { useNavigate } from 'react-router-dom';
import { isBackendUnavailableError, isUnauthorizedError, requestReporterOtp } from '../api/auth';
import { useAuth } from '../auth/auth-context';
import LanguageSwitcher from '../components/LanguageSwitcher';

type LoginMode = 'admin' | 'reporterRequest' | 'reporterVerify';

type LoginErrorKey =
  | 'login.invalidCredentials'
  | 'login.genericError'
  | 'login.invalidCode'
  | 'login.otpGenericError'
  | 'auth.backendUnavailable';

export default function LoginPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { signIn, reporterSignIn, status, authErrorKey } = useAuth();
  const [loginMode, setLoginMode] = useState<LoginMode>('reporterRequest');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorKey, setErrorKey] = useState<LoginErrorKey | null>(null);

  function switchToReporter() {
    setLoginMode('reporterRequest');
    setPassword('');
    setOtpCode('');
    setErrorKey(null);
  }

  function switchToAdmin() {
    setLoginMode('admin');
    setOtpCode('');
    setErrorKey(null);
  }

  async function handleAdminSubmit(e: FormEvent) {
    e.preventDefault();
    setErrorKey(null);
    setLoading(true);
    try {
      await signIn(username, password);
      navigate('/');
    } catch (error) {
      if (isUnauthorizedError(error)) {
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
    setLoading(true);
    try {
      await reporterSignIn(username, otpCode);
      navigate('/');
    } catch (error) {
      if (isUnauthorizedError(error)) {
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

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.100',
        px: 2,
      }}
    >
      <LanguageSwitcher />
      <Card sx={{ width: 420, p: 2 }} elevation={4}>
        <CardContent>
          <Box
            sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}
          >
            <Avatar sx={{ bgcolor: 'primary.main', mb: 1 }}>
              {loginMode === 'admin' ? <LockOutlinedIcon /> : <SendOutlinedIcon />}
            </Avatar>
            <Typography variant="h5" fontWeight={600}>
              {t('login.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {loginMode === 'admin' ? t('login.subtitle') : t('login.reporterSubtitle')}
            </Typography>
          </Box>

          {status === 'error' && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {t(authErrorKey ?? 'auth.sessionLoadFailed')}
            </Alert>
          )}

          {errorKey && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {t(errorKey)}
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
                <Link
                  component="button"
                  type="button"
                  variant="body2"
                  onClick={switchToReporter}
                >
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
                <Link
                  component="button"
                  type="button"
                  variant="body2"
                  onClick={switchToAdmin}
                >
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
  );
}

