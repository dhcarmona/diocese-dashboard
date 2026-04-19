import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getStatisticsTemplates } from '../api/statistics';
import type { ServiceTemplateSummary } from '../api/serviceTemplates';
import ActionTile from '../components/ActionTile';
import PageHeader from '../components/PageHeader';
import TileGrid from '../components/TileGrid';
import { tileArtwork } from '../components/tileArtwork';

export default function StatisticsTemplateSelectionPage() {
  const { t } = useTranslation();
  const [templates, setTemplates] = useState<ServiceTemplateSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadTemplates() {
      setLoading(true);
      setHasError(false);
      try {
        const loaded = await getStatisticsTemplates();
        if (active) setTemplates(loaded);
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadTemplates();
    return () => { active = false; };
  }, []);

  return (
    <>
      <PageHeader
        title={t('statistics.templateSelection.title')}
        subtitle={t('statistics.templateSelection.subtitle')}
      />

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('statistics.templateSelection.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('statistics.templateSelection.loadError')}</Alert>
      )}

      {!loading && !hasError && templates.length === 0 && (
        <Alert severity="info">{t('statistics.templateSelection.empty')}</Alert>
      )}

      {!loading && !hasError && templates.length > 0 && (
        <TileGrid>
          {templates.map((template) => (
            <ActionTile
              key={template.id}
              title={template.serviceTemplateName}
              to={`/statistics/${template.id}`}
              imageUrl={tileArtwork.viewReports}
            />
          ))}
        </TileGrid>
      )}
    </>
  );
}
