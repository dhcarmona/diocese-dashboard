import Box from '@mui/material/Box';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';

/** Unobtrusive fixed language toggle shown on every page. */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const current = i18n.language.startsWith('es') ? 'es' : 'en';

  function handleChange(_e: MouseEvent<HTMLElement>, lang: string | null) {
    if (lang) void i18n.changeLanguage(lang);
  }

  return (
    <Box sx={{ position: 'fixed', top: 16, right: 16, zIndex: 1300 }}>
      <ToggleButtonGroup value={current} exclusive onChange={handleChange} size="small">
        <ToggleButton value="en" aria-label="English">
          EN
        </ToggleButton>
        <ToggleButton value="es" aria-label="Spanish">
          ES
        </ToggleButton>
      </ToggleButtonGroup>
    </Box>
  );
}
