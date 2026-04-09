import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1C3A6E',
      light: '#2D5AA0',
      dark: '#0F2144',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#B91C1C',
      light: '#DC2626',
      dark: '#7F1D1D',
      contrastText: '#ffffff',
    },
    background: {
      default: '#F0F4FA',
      paper: '#ffffff',
    },
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
  },
});

export default theme;
