import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import CssBaseline from '@mui/material/CssBaseline'
import GlobalStyles from '@mui/material/GlobalStyles'
import { ThemeProvider } from '@mui/material/styles'
import './i18n.ts'
import App from './App.tsx'
import { ErrorBoundary } from './components/ErrorBoundary.tsx'
import theme from './theme.ts'
import { reportClientError } from './utils/errorReporter.ts'

window.onerror = (_message, source, lineno, colno, error) => {
  void reportClientError({
    message: error?.message ?? String(_message),
    stack: error?.stack ?? `at ${source ?? '?'}:${lineno ?? 0}:${colno ?? 0}`,
  });
  return false;
};

window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
  const reason: unknown = event.reason;
  void reportClientError({
    message: reason instanceof Error ? reason.message : String(reason),
    stack: reason instanceof Error ? reason.stack : undefined,
  });
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <GlobalStyles styles={{ 'html, body': { overscrollBehaviorY: 'none' } }} />
        <App />
      </ThemeProvider>
    </ErrorBoundary>
  </StrictMode>,
)
