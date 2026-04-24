import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getServiceTemplatesForViewing, type ServiceTemplateSummary } from '../api/serviceTemplates';
import ActionTile from '../components/ActionTile';
import PageHeader from '../components/PageHeader';
import TileGrid from '../components/TileGrid';
import { tileArtwork } from '../components/tileArtwork';

export default function AdminReportTemplateSelectionPage() {
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
        const loadedTemplates = await getServiceTemplatesForViewing();
        if (active) {
          setTemplates(loadedTemplates);
        }
      } catch {
        if (active) {
          setHasError(true);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadTemplates();

    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <PageHeader
        title={t('reportsHub.tiles.individualReports.title')}
        subtitle={t('reports.templateSelection.subtitle')}
      />

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('reports.templateSelection.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('reports.templateSelection.loadError')}</Alert>
      )}

      {!loading && !hasError && templates.length === 0 && (
        <Alert severity="info">{t('reports.templateSelection.empty')}</Alert>
      )}

      {!loading && !hasError && templates.length > 0 && (
        <TileGrid>
          {templates.map((template) => (
            <ActionTile
              key={template.id}
              title={template.serviceTemplateName}
              description={t('reports.templateSelection.tileDescription')}
              to={`/reports/view/individual/${template.id}`}
              imageUrl={tileArtwork.viewReports}
            />
          ))}
        </TileGrid>
      )}
    </>
  );
}
