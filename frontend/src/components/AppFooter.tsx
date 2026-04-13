import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

const CURRENT_YEAR = new Date().getFullYear();

const GMT_MINUS_6_OFFSET_MS = -6 * 60 * 60 * 1000;

function formatBuildTime(iso: string): string {
  const d = new Date(new Date(iso).getTime() + GMT_MINUS_6_OFFSET_MS);
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(d.getUTCDate()).padStart(2, '0');
  const hh = String(d.getUTCHours()).padStart(2, '0');
  const min = String(d.getUTCMinutes()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd} ${hh}:${min} GMT-6`;
}

const BUILD_LABEL = `${formatBuildTime(__BUILD_TIME__)} (${__COMMIT_HASH__})`;

interface AppFooterProps {
  showBuildInfo?: boolean;
}

export default function AppFooter({ showBuildInfo = false }: Readonly<AppFooterProps>) {
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
            Build {BUILD_LABEL}
          </Typography>
        )}
      </Box>
    </Box>
  );
}
