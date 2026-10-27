import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getConventionReport, type ConventionReport } from '../api/conventionReport';
import PageHeader from '../components/PageHeader';
import { downloadConventionReportExcel } from '../utils/conventionReportExcel';
import { formatDate } from '../utils/dateFormatting';
import { formatStatisticsValue } from '../utils/statisticsFormatting';

export default function ConventionReportPage() {
  const { t, i18n } = useTranslation();
  const { templateId } = useParams<{ templateId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const startDate = searchParams.get('startDate') ?? '';
  const endDate = searchParams.get('endDate') ?? '';

  const [report, setReport] = useState<ConventionReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [excelGenerating, setExcelGenerating] = useState(false);

  useEffect(() => {
    if (!templateId || !startDate || !endDate) return;
    let active = true;

    async function loadReport() {
      setLoading(true);
      setHasError(false);
      try {
        const data = await getConventionReport({
          templateId: Number(templateId),
          startDate,
          endDate,
        });
        if (active) setReport(data);
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadReport();
    return () => { active = false; };
  }, [templateId, startDate, endDate]);

  function handleDownloadExcel() {
    if (!report) return;
    setExcelGenerating(true);
    try {
      downloadConventionReportExcel(report, {
        churchHeader: t('conventionReport.report.churchHeader'),
        reportCountHeader: t('conventionReport.report.reportCountHeader'),
        totalsLabel: t('conventionReport.report.excel.totalsLabel'),
      });
    } finally {
      setExcelGenerating(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('conventionReport.report.title')}
        subtitle={report?.templateName ?? ''}
      />

      <Button
        variant="outlined"
        size="small"
        sx={{ mb: 3 }}
        onClick={() => void navigate(`/convention-report/${templateId ?? ''}`)}
      >
        {t('conventionReport.report.backToFilter')}
      </Button>

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('conventionReport.report.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('conventionReport.report.loadError')}</Alert>
      )}

      {!loading && !hasError && report && (
        <Box>
          <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, justifyContent: 'center' }}>
              <Box sx={{ textAlign: 'center' }}>
                <Typography variant="overline" color="text.secondary">
                  {t('conventionReport.report.dateRangeLabel')}
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {formatDate(startDate, i18n.resolvedLanguage)} –{' '}
                  {formatDate(endDate, i18n.resolvedLanguage)}
                </Typography>
              </Box>
            </Box>
          </Paper>

          {report.duplicateNotes.length > 0 && (
            <Alert severity="warning" sx={{ mb: 4 }}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                {t('conventionReport.report.duplicateNotesTitle')}
              </Typography>
              {report.duplicateNotes.map((note) => (
                <Typography key={`${note.churchName}-${note.serviceDate}`} variant="body2">
                  {t('conventionReport.report.duplicateNoteLine', {
                    churchName: note.churchName,
                    date: formatDate(note.serviceDate, i18n.resolvedLanguage),
                    count: note.reportCount,
                  })}
                </Typography>
              ))}
            </Alert>
          )}

          <Paper elevation={2} sx={{ p: 3, overflowX: 'auto' }}>
            {report.columns.length === 0 ? (
              <Alert severity="info">{t('conventionReport.report.noItems')}</Alert>
            ) : (
              <>
                <Button
                  variant="outlined"
                  size="small"
                  sx={{ mb: 3 }}
                  disabled={excelGenerating}
                  onClick={() => handleDownloadExcel()}
                >
                  {excelGenerating ? <CircularProgress size={18} sx={{ mr: 1 }} /> : null}
                  {t('conventionReport.report.downloadExcel')}
                </Button>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>{t('conventionReport.report.churchHeader')}</strong></TableCell>
                      <TableCell align="right">
                        <strong>{t('conventionReport.report.reportCountHeader')}</strong>
                      </TableCell>
                      {report.columns.map((column) => (
                        <TableCell key={column.itemId} align="right">
                          <strong>{column.itemTitle}</strong>
                        </TableCell>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {report.rows.map((row) => (
                      <TableRow key={row.churchName}>
                        <TableCell>{row.churchName}</TableCell>
                        <TableCell align="right">{row.reportCount}</TableCell>
                        {row.values.map((value, index) => (
                          <TableCell key={report.columns[index].itemId} align="right">
                            {formatStatisticsValue(value, report.columns[index].itemType)}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </>
            )}
          </Paper>
        </Box>
      )}
    </>
  );
}
