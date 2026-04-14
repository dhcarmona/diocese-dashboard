import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { getAppDateLocale } from '../utils/dateFormatting';

const CURRENT_YEAR = new Date().getFullYear();

const GMT_MINUS_6_OFFSET_MS = -6 * 60 * 60 * 1000;

function formatBuildTime(iso: string, language?: string | null): string {
  const d = new Date(new Date(iso).getTime() + GMT_MINUS_6_OFFSET_MS);
  const monthAbbreviations = getAppDateLocale(language) === 'en'
    ? ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
    : ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
  const yyyy = d.getUTCFullYear();
  const dd = String(d.getUTCDate()).padStart(2, '0');
  const mmm = monthAbbreviations[d.getUTCMonth()];
  const hh = String(d.getUTCHours()).padStart(2, '0');
  const min = String(d.getUTCMinutes()).padStart(2, '0');
  return `${dd} ${mmm} ${yyyy} ${hh}:${min} GMT-6`;
}

interface AppFooterProps {
  showBuildInfo?: boolean;
}

export default function AppFooter({ showBuildInfo = false }: Readonly<AppFooterProps>) {
  const { i18n } = useTranslation();
  const buildLabel = `${formatBuildTime(__BUILD_TIME__, i18n.resolvedLanguage)} (${__COMMIT_HASH__})`;

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
