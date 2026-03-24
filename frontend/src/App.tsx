import type { ReactNode } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { useAuth } from './auth/auth-context';
import AppShell from './components/AppShell';
import LoginPage from './pages/LoginPage';
import CelebrantManagementPage from './pages/CelebrantManagementPage';
import FeaturePlaceholderPage from './pages/FeaturePlaceholderPage';
import HomePage from './pages/HomePage';
import TemplateSelectionPage from './pages/TemplateSelectionPage';

function FullPageStatus({
  children,
}: Readonly<{ children: ReactNode }>) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'grey.100',
        p: 2,
      }}
    >
      <Card sx={{ maxWidth: 520, width: '100%' }} elevation={3}>
        <CardContent sx={{ p: 4 }}>{children}</CardContent>
      </Card>
    </Box>
  );
}

function ProtectedRoute() {
  const { t } = useTranslation();
  const { status, refreshUser, authErrorKey } = useAuth();

  if (status === 'loading') {
    return (
      <FullPageStatus>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={40} />
          <Typography variant="h6">{t('auth.loading')}</Typography>
        </Box>
      </FullPageStatus>
    );
  }

  if (status === 'error') {
    return (
      <FullPageStatus>
        <Alert severity="error" sx={{ mb: 2 }}>
          {t(authErrorKey ?? 'auth.sessionLoadFailed')}
        </Alert>
        <Button variant="contained" size="large" onClick={() => void refreshUser()}>
          {t('auth.tryAgain')}
        </Button>
      </FullPageStatus>
    );
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

function AdminRoute() {
  const { user } = useAuth();

  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

function LoginRoute() {
  const { status } = useAuth();

  if (status === 'authenticated') {
    return <Navigate to="/" replace />;
  }

  return <LoginPage />;
}

function AuthenticatedLayout() {
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AuthenticatedLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/reports/new" element={<TemplateSelectionPage />} />
          <Route
            path="/submit/service-templates/:templateId"
            element={
              <FeaturePlaceholderPage
                titleKey="areas.submitService.title"
                descriptionKey="areas.submitService.description"
              />
            }
          />
          <Route element={<AdminRoute />}>
            <Route
              path="/service-templates/manage"
              element={
                <FeaturePlaceholderPage
                  titleKey="areas.serviceTemplates.title"
                  descriptionKey="areas.serviceTemplates.description"
                />
              }
            />
            <Route
              path="/users/manage"
              element={
                <FeaturePlaceholderPage
                  titleKey="areas.users.title"
                  descriptionKey="areas.users.description"
                />
              }
            />
            <Route
              path="/celebrants/manage"
              element={<CelebrantManagementPage />}
            />
            <Route
              path="/churches/manage"
              element={
                <FeaturePlaceholderPage
                  titleKey="areas.churches.title"
                  descriptionKey="areas.churches.description"
                />
              }
            />
            <Route
              path="/reporter-links/manage"
              element={
                <FeaturePlaceholderPage
                  titleKey="areas.reporterLinks.title"
                  descriptionKey="areas.reporterLinks.description"
                />
              }
            />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
