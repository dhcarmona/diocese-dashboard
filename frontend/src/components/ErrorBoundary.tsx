import { Component, type ErrorInfo, type ReactNode } from 'react';
import { reportClientError } from '../utils/errorReporter';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

/**
 * Top-level React error boundary. Catches rendering crashes that would otherwise
 * unmount the React tree and leave the user with a blank page. Renders a minimal
 * fallback UI and reports the error to the backend log.
 *
 * <p>Fallback text is intentionally hard-coded in Spanish because this app targets
 * a single Spanish-speaking diocese and class components cannot use React hooks
 * (including {@code useTranslation}).
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    void reportClientError({
      message: error.message,
      stack: `${error.stack ?? ''}\n\nComponent stack:\n${info.componentStack ?? ''}`,
    });
  }

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <div
          style={{
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            fontFamily: 'sans-serif',
            padding: '2rem',
            textAlign: 'center',
            gap: '1rem',
          }}
        >
          <h2 style={{ margin: 0 }}>Ocurrió un error inesperado</h2>
          <p style={{ margin: 0, color: '#666' }}>Por favor recargue la página e intente de nuevo.</p>
          <button
            onClick={() => window.location.reload()}
            style={{
              marginTop: '0.5rem',
              padding: '0.5rem 1.5rem',
              fontSize: '1rem',
              cursor: 'pointer',
            }}
          >
            Recargar
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
