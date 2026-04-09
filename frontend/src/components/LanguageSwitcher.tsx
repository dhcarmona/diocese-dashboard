import Box from '@mui/material/Box';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';

interface LanguageSwitcherProps {
  placement?: 'fixed' | 'static';
}

/** Unobtrusive fixed language toggle shown on every page. */
export default function LanguageSwitcher({
  placement = 'fixed',
}: Readonly<LanguageSwitcherProps>) {
  const { i18n, t } = useTranslation();
  const current = i18n.language.startsWith('es') ? 'es' : 'en';
  const onDark = placement === 'static';

  function handleChange(_e: MouseEvent<HTMLElement>, lang: string | null) {
    if (lang) void i18n.changeLanguage(lang);
  }

  return (
    <Box
      sx={
        placement === 'fixed'
          ? { position: 'fixed', top: 16, right: 16, zIndex: 1300 }
          : undefined
      }
    >
      <ToggleButtonGroup
        value={current}
        exclusive
        onChange={handleChange}
        size="small"
        sx={onDark ? {
          '& .MuiToggleButton-root': {
            color: 'rgba(255,255,255,0.7)',
            borderColor: 'rgba(255,255,255,0.3)',
            '&:hover': { bgcolor: 'rgba(255,255,255,0.08)', borderColor: 'rgba(255,255,255,0.5)' },
            '&.Mui-selected': {
              color: 'primary.main',
              bgcolor: 'rgba(255,255,255,0.92)',
              borderColor: 'rgba(255,255,255,0.6)',
              '&:hover': { bgcolor: 'rgba(255,255,255,0.82)' },
            },
          },
        } : undefined}
      >
        <ToggleButton value="en" aria-label={t('language.english')}>
          EN
        </ToggleButton>
        <ToggleButton value="es" aria-label={t('language.spanish')}>
          ES
        </ToggleButton>
      </ToggleButtonGroup>
    </Box>
  );
}

