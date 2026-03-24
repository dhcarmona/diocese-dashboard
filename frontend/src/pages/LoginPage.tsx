import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import Alert from '@mui/material/Alert';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { isBackendUnavailableError, isUnauthorizedError } from '../api/auth';
import { useAuth } from '../auth/auth-context';
import LanguageSwitcher from '../components/LanguageSwitcher';

type LoginErrorKey =
  | 'login.invalidCredentials'
  | 'login.genericError'
  | 'auth.backendUnavailable';

export default function LoginPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { signIn, status, authErrorKey } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorKey, setErrorKey] = useState<LoginErrorKey | null>(null);

  async function handleSubmit(e: FormEvent) {
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

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', bgcolor: 'grey.100', px: 2 }}>
      <LanguageSwitcher />
      <Card sx={{ width: 420, p: 2 }} elevation={4}>
        <CardContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mb: 3 }}>
            <Avatar sx={{ bgcolor: 'primary.main', mb: 1 }}>
              <LockOutlinedIcon />
            </Avatar>
            <Typography variant="h5" fontWeight={600}>
              {t('login.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('login.subtitle')}
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

          <Box component="form" onSubmit={handleSubmit} noValidate>
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
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
