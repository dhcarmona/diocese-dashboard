import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useParams } from 'react-router-dom';
import {
  type ServiceInstanceSummary,
  getInstancesByTemplate,
} from '../api/serviceInstances';
import PageHeader from '../components/PageHeader';
import { formatDate, formatDateTime } from '../utils/dateFormatting';

export default function ReportInstancesListPage() {
  const { templateId } = useParams<{ templateId: string }>();
  const { t, i18n } = useTranslation();

  const [instances, setInstances] = useState<ServiceInstanceSummary[]>([]);
  const [templateName, setTemplateName] = useState('');
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const parsedId = Number(templateId);
    if (!templateId || !Number.isFinite(parsedId)) {
      setHasError(true);
      setLoading(false);
      return;
    }

    let active = true;

    async function load() {
      setLoading(true);
      setHasError(false);
      try {
        const data = await getInstancesByTemplate(parsedId);
        if (active) {
          setInstances(data);
          setTemplateName(data.length > 0 ? (data[0].templateName ?? '') : '');
        }
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [templateId]);

  const subtitle = templateName
    ? t('reportsList.subtitle', { templateName })
    : t('reportsList.title');

  return (
    <>
      <PageHeader title={t('reportsList.title')} subtitle={subtitle} />

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('reportsList.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('reportsList.loadError')}</Alert>
      )}

      {!loading && !hasError && instances.length === 0 && (
        <Alert severity="info">{t('reportsList.empty')}</Alert>
      )}

      {!loading && !hasError && instances.length > 0 && (
        <TableContainer component={Paper} sx={{ mt: 2 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('reportsList.columns.date')}</TableCell>
                <TableCell>{t('reportsList.columns.church')}</TableCell>
                <TableCell>{t('reportsList.columns.reporter')}</TableCell>
                {instances.some((i) => i.submittedAt != null) && (
                  <TableCell>{t('reportsList.columns.submittedAt')}</TableCell>
                )}
                <TableCell>{t('reportsList.columns.actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {instances.map((instance) => (
                <TableRow key={instance.id} hover>
                  <TableCell>{formatDate(instance.serviceDate, i18n.resolvedLanguage)}</TableCell>
                  <TableCell>{instance.churchName}</TableCell>
                  <TableCell>
                    {instance.submittedByFullName
                      ?? instance.submittedByUsername
                      ?? t('reportsList.unknownReporter')}
                  </TableCell>
                  {instances.some((i) => i.submittedAt != null) && (
                    <TableCell>
                      {instance.submittedAt != null
                        ? formatDateTime(instance.submittedAt, i18n.resolvedLanguage)
                        : '—'}
                    </TableCell>
                  )}
                  <TableCell>
                    <Button
                      component={RouterLink}
                      to={`/reports/view/individual/${templateId ?? ''}/${instance.id}`}
                      variant="outlined"
                      size="small"
                    >
                      {t('reportsList.viewDetails')}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
