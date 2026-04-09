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
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="sticky" color="primary" elevation={0}>
        <Toolbar
          sx={{
            flexWrap: 'wrap',
            gap: 2,
            alignItems: 'center',
            py: 1,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexGrow: 1, minWidth: 200 }}>
            <Box
              component="img"
              src="/logo.png"
              alt="Diocese logo"
              sx={{ height: 36, width: 36, objectFit: 'contain', flexShrink: 0 }}
            />
            <Box>
              <Typography variant="subtitle1" fontWeight={700} color="primary.contrastText"
                sx={{ lineHeight: 1.2 }}>
                {t('common.appName')}
              </Typography>
              <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.65)', lineHeight: 1 }}>
                {t('shell.signedInAs', { username: user?.username ?? '' })}
              </Typography>
            </Box>
          </Box>
          <LanguageSwitcher placement="static" />
          <Button
            component={RouterLink}
            to="/"
            variant="outlined"
            size="small"
            startIcon={<HomeOutlinedIcon />}
            sx={{
              color: 'primary.contrastText',
              borderColor: 'rgba(255,255,255,0.4)',
              '&:hover': { borderColor: 'primary.contrastText', bgcolor: 'rgba(255,255,255,0.08)' },
            }}
          >
            {t('navigation.home')}
          </Button>
          <Button
            onClick={() => void handleSignOut()}
            variant="contained"
            color="secondary"
            size="small"
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

