import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { formatDateTimeAtFixedOffset } from '../utils/dateFormatting';

const CURRENT_YEAR = new Date().getFullYear();

const COSTA_RICA_OFFSET_HOURS = -6;

interface AppFooterProps {
  showBuildInfo?: boolean;
}

export default function AppFooter({ showBuildInfo = false }: Readonly<AppFooterProps>) {
  const { i18n } = useTranslation();
  const buildLabel =
    `${formatDateTimeAtFixedOffset(__BUILD_TIME__, COSTA_RICA_OFFSET_HOURS, i18n.resolvedLanguage)} `
    + `GMT-6 (${__COMMIT_HASH__})`;

  return (
    <Box
      component="footer"
      sx={{
        display: 'flex',
        alignItems: 'center',
        px: 3,
        py: 1.5,
        bgcolor: 'primary.dark',
      }}
    >
      <Box sx={{ flex: 1 }} />
      <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.7)' }}>
        © {CURRENT_YEAR} Misión Santa Clara de Asís – IECR
      </Typography>
      <Box sx={{ flex: 1, display: 'flex', justifyContent: 'flex-end' }}>
        {showBuildInfo && (
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.4)' }}>
            Build {buildLabel}
          </Typography>
        )}
      </Box>
    </Box>
  );
}
