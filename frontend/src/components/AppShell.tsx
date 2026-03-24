import type { ReactNode } from 'react';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/auth-context';
import LanguageSwitcher from './LanguageSwitcher';

export default function AppShell({ children }: Readonly<{ children: ReactNode }>) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { signOut, user } = useAuth();

  async function handleSignOut() {
    await signOut();
    navigate('/login', { replace: true });
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.100' }}>
      <AppBar position="sticky" color="inherit" elevation={1}>
        <Toolbar
          sx={{
            flexWrap: 'wrap',
            gap: 2,
            alignItems: 'center',
            py: 1.5,
          }}
        >
          <Box sx={{ flexGrow: 1, minWidth: 240 }}>
            <Typography variant="h5" fontWeight={700} color="text.primary">
              {t('common.appName')}
            </Typography>
            <Typography variant="body1" color="text.secondary">
              {t('shell.signedInAs', { username: user?.username ?? '' })}
            </Typography>
          </Box>
          <LanguageSwitcher placement="static" />
          <Button
            component={RouterLink}
            to="/"
            variant="outlined"
            size="large"
            startIcon={<HomeOutlinedIcon />}
          >
            {t('navigation.home')}
          </Button>
          <Button
            onClick={() => void handleSignOut()}
            variant="contained"
            size="large"
            startIcon={<LogoutOutlinedIcon />}
          >
            {t('common.signOut')}
          </Button>
        </Toolbar>
      </AppBar>
      <Box component="main" sx={{ maxWidth: 1200, mx: 'auto', px: { xs: 2, md: 4 }, py: 4 }}>
        {children}
      </Box>
    </Box>
  );
}
