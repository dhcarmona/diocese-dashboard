import Box from '@mui/material/Box';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import type { MouseEvent } from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/auth-context';
import type { PreferredLanguage } from '../api/auth';

interface LanguageSwitcherProps {
  placement?: 'fixed' | 'static';
}

/** Unobtrusive fixed language toggle shown on every page. */
export default function LanguageSwitcher({
  placement = 'fixed',
}: Readonly<LanguageSwitcherProps>) {
  const { i18n, t } = useTranslation();
  const { user, updatePreferredLanguage } = useAuth();
  const current = i18n.language.startsWith('es') ? 'es' : 'en';
  const onDark = placement === 'static';
  const [saving, setSaving] = useState(false);

  async function handleChange(_e: MouseEvent<HTMLElement>, lang: string | null) {
    if (!lang || lang === current || saving) {
      return;
    }
    const nextLanguage = lang as PreferredLanguage;
    setSaving(true);
    await i18n.changeLanguage(nextLanguage);
    try {
      if (user) {
        try {
          await updatePreferredLanguage(nextLanguage);
        } catch (error) {
          await i18n.changeLanguage(current);
          console.error('Failed to save preferred language.', error);
        }
      }
    } finally {
      setSaving(false);
    }
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
        disabled={saving}
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
