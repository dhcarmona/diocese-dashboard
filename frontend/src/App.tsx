import type { ReactNode } from 'react';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { enUS, esES } from '@mui/x-date-pickers/locales';
import { useTranslation } from 'react-i18next';
import { BrowserRouter, Navigate, Outlet, Route, Routes, useSearchParams } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { useAuth } from './auth/auth-context';
import AppShell from './components/AppShell';
import ChurchManagementPage from './pages/ChurchManagementPage';
import LoginPage from './pages/LoginPage';
import CelebrantManagementPage from './pages/CelebrantManagementPage';
import HomePage from './pages/HomePage';
import ReportTemplateSelectionPage from './pages/ReportTemplateSelectionPage';
import ReportInstanceDetailPage from './pages/ReportInstanceDetailPage';
import ReportInstancesListPage from './pages/ReportInstancesListPage';
import ReporterLinkManagementPage from './pages/ReporterLinkManagementPage';
import ReporterLinkPage from './pages/ReporterLinkPage';
import ReporterUserManagementPage from './pages/ReporterUserManagementPage';
import ReportsHubPage from './pages/ReportsHubPage';
import ServiceSubmitPage from './pages/ServiceSubmitPage';
import ServiceTemplateManagementPage from './pages/ServiceTemplateManagementPage';
import TemplateSelectionPage from './pages/TemplateSelectionPage';
import StatisticsFilterPage from './pages/StatisticsFilterPage';
import StatisticsReportPage from './pages/StatisticsReportPage';
import StatisticsTemplateSelectionPage from './pages/StatisticsTemplateSelectionPage';
import WhatsAppMessageLogPage from './pages/WhatsAppMessageLogPage';
import { getAppDateLocale } from './utils/dateFormatting';

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
  const [searchParams] = useSearchParams();
  const redirectTo = searchParams.get('redirect');

  if (status === 'authenticated') {
    return <Navigate to={redirectTo ?? '/'} replace />;
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
      <Route path="/r/:token" element={<ReporterLinkPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AuthenticatedLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/reports/new" element={<TemplateSelectionPage />} />
          <Route
            path="/submit/service-templates/:templateId"
            element={<ServiceSubmitPage />}
          />
          <Route path="/statistics" element={<StatisticsTemplateSelectionPage />} />
          <Route path="/statistics/:templateId" element={<StatisticsFilterPage />} />
          <Route path="/statistics/:templateId/report" element={<StatisticsReportPage />} />
          <Route path="/reports/view" element={<ReportsHubPage />} />
          <Route
            path="/reports/view/individual"
            element={<ReportTemplateSelectionPage />}
          />
          <Route
            path="/reports/view/individual/:templateId"
            element={<ReportInstancesListPage />}
          />
          <Route
            path="/reports/view/individual/:templateId/:instanceId"
            element={<ReportInstanceDetailPage />}
          />
          <Route element={<AdminRoute />}>
            <Route
              path="/service-templates/manage"
              element={<ServiceTemplateManagementPage />}
            />
            <Route
              path="/users/manage"
              element={<ReporterUserManagementPage />}
            />
            <Route
              path="/celebrants/manage"
              element={<CelebrantManagementPage />}
            />
            <Route
              path="/churches/manage"
              element={<ChurchManagementPage />}
            />
            <Route
              path="/reporter-links/manage"
              element={<ReporterLinkManagementPage />}
            />
            <Route
              path="/whatsapp-logs"
              element={<WhatsAppMessageLogPage />}
            />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

function App() {
  const { i18n } = useTranslation();
  const locale = getAppDateLocale(i18n.resolvedLanguage);
  const localeText = locale === 'en'
    ? enUS.components.MuiLocalizationProvider.defaultProps.localeText
    : esES.components.MuiLocalizationProvider.defaultProps.localeText;

  return (
    <LocalizationProvider
      dateAdapter={AdapterDayjs}
      adapterLocale={locale}
      localeText={localeText}
    >
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </LocalizationProvider>
  );
}

export default App;
