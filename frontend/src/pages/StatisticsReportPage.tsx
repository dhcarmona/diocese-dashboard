import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Link from '@mui/material/Link';
import Divider from '@mui/material/Divider';
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
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { getStatistics, type AggregatedItem, type StatisticsReport } from '../api/statistics';
import PageHeader from '../components/PageHeader';
import { formatDate } from '../utils/dateFormatting';

const CHART_COLORS = [
  '#1C3A6E', '#2E6DB4', '#4A9FD4', '#7BBFDB', '#AED6E8',
  '#F4A621', '#E07B39', '#C7522A', '#8E3B23', '#5A2314',
];

function formatValue(value: number, type: string): string {
  if (type === 'DOLLARS') return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  if (type === 'COLONES') return `₡${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  return value.toLocaleString('en-US');
}

interface ItemSectionProps {
  item: AggregatedItem;
  totalLabel: string;
  trendLabel: string;
}

function ItemSection({ item, totalLabel, trendLabel }: Readonly<ItemSectionProps>) {
  const { t, i18n } = useTranslation();
  const unit = t(`statistics.report.itemTypes.${item.itemType}`, { defaultValue: item.itemType });

  const barData = item.timeSeriesData.map((pt) => ({
    date: formatDate(pt.date, i18n.resolvedLanguage),
    value: pt.value,
  }));

  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h6" fontWeight={700} gutterBottom>
        {item.itemTitle}
        <Chip
          label={unit}
          size="small"
          sx={{ ml: 1, fontWeight: 600 }}
          color="primary"
          variant="outlined"
        />
      </Typography>
      <Typography variant="body1" sx={{ mb: 2 }}>
        {totalLabel}: <strong>{formatValue(item.total, item.itemType)}</strong>
      </Typography>
      {item.timeSeriesData.length > 0 && (
        <Box>
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            {trendLabel}
          </Typography>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={barData} margin={{ top: 4, right: 16, left: 16, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip
                formatter={(val) => [formatValue(Number(val ?? 0), item.itemType), item.itemTitle]}
              />
              <Bar dataKey="value" fill="#1C3A6E" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      )}
    </Box>
  );
}

export default function StatisticsReportPage() {
  const { t, i18n } = useTranslation();
  const { templateId } = useParams<{ templateId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const churchName = searchParams.get('churchName') ?? undefined;
  const startDate = searchParams.get('startDate') ?? '';
  const endDate = searchParams.get('endDate') ?? '';

  const [report, setReport] = useState<StatisticsReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    if (!templateId || !startDate || !endDate) return;
    let active = true;

    async function loadReport() {
      setLoading(true);
      setHasError(false);
      try {
        const data = await getStatistics({
          templateId: Number(templateId),
          churchName,
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
  }, [templateId, churchName, startDate, endDate]);

  const churchDisplay = report?.global
    ? t('statistics.report.global')
    : (report?.churchName ?? churchName ?? '');

  return (
    <>
      <PageHeader
        title={t('statistics.report.title')}
        subtitle={report?.templateName ?? ''}
      />

      <Button
        variant="outlined"
        size="small"
        sx={{ mb: 3 }}
        onClick={() => void navigate(`/statistics/${templateId ?? ''}`)}
      >
        {t('statistics.report.backToFilter')}
      </Button>

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('statistics.report.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('statistics.report.loadError')}</Alert>
      )}

      {!loading && !hasError && report && (
        <Box>
          {/* Report meta */}
          <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
              <Box>
                <Typography variant="overline" color="text.secondary">
                  {t('statistics.filter.churchLabel')}
                </Typography>
                <Typography variant="h6" fontWeight={700}>{churchDisplay}</Typography>
              </Box>
              <Box>
                <Typography variant="overline" color="text.secondary">
                  {t('statistics.report.dateRangeLabel')}
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {formatDate(startDate, i18n.resolvedLanguage)} –{' '}
                  {formatDate(endDate, i18n.resolvedLanguage)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="overline" color="text.secondary">
                  {t('statistics.report.totalServices')}
                </Typography>
                <Typography variant="h6" fontWeight={700}>{report.totalServiceCount}</Typography>
              </Box>
            </Box>
          </Paper>

          {/* Celebrant pie chart */}
          <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              {t('statistics.report.celebrantsTitle')}
            </Typography>
            {report.celebrantStats.length === 0 ? (
              <Typography color="text.secondary">{t('statistics.report.noCelebrants')}</Typography>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 2 }}>
                <ResponsiveContainer width={280} height={280}>
                  <PieChart>
                    <Pie
                      data={report.celebrantStats}
                      dataKey="serviceCount"
                      nameKey="celebrantName"
                      cx="50%"
                      cy="50%"
                      outerRadius={130}
                      labelLine={false}
                    >
                      {report.celebrantStats.map((entry, index) => (
                        <Cell
                          key={entry.celebrantId}
                          fill={CHART_COLORS[index % CHART_COLORS.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(val, name) => [Number(val ?? 0), String(name ?? '')]}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <Box>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell aria-hidden="true" role="presentation" />
                        <TableCell><strong>{t('statistics.report.celebrant')}</strong></TableCell>
                        <TableCell align="right"><strong>{t('statistics.report.services')}</strong></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {report.celebrantStats.map((stat, index) => (
                        <TableRow key={stat.celebrantId}>
                          <TableCell sx={{ pr: 0 }}>
                            <Box
                              aria-hidden="true"
                              sx={{
                                width: 14,
                                height: 14,
                                borderRadius: '3px',
                                bgcolor: CHART_COLORS[index % CHART_COLORS.length],
                              }}
                            />
                          </TableCell>
                          <TableCell>{stat.celebrantName}</TableCell>
                          <TableCell align="right">{stat.serviceCount}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              </Box>
            )}
          </Paper>

          {/* Numerical items */}
          {report.numericalItems.length > 0 && (
            <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
              <Typography variant="h5" fontWeight={700} gutterBottom>
                {t('statistics.report.numericalTitle')}
              </Typography>
              <Divider sx={{ mb: 3 }} />
              {report.numericalItems.map((item) => (
                <ItemSection
                  key={item.itemId}
                  item={item}
                  totalLabel={t('statistics.report.total')}
                  trendLabel={t('statistics.report.timeSeriesTitle')}
                />
              ))}
            </Paper>
          )}

          {/* Money items */}
          {report.moneyItems.length > 0 && (
            <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
              <Typography variant="h5" fontWeight={700} gutterBottom>
                {t('statistics.report.moneyTitle')}
              </Typography>
              <Divider sx={{ mb: 3 }} />
              {report.moneyItems.map((item) => (
                <ItemSection
                  key={item.itemId}
                  item={item}
                  totalLabel={t('statistics.report.total')}
                  trendLabel={t('statistics.report.timeSeriesTitle')}
                />
              ))}
            </Paper>
          )}

          {report.numericalItems.length === 0 && report.moneyItems.length === 0 && (
            <Alert severity="info" sx={{ mb: 4 }}>{t('statistics.report.noItems')}</Alert>
          )}

          {/* Pending reporter links */}
          <Paper elevation={2} sx={{ p: 3, mb: 4 }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              {t('statistics.report.pendingLinksTitle')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('statistics.report.pendingLinksSubtitle')}
            </Typography>
            {report.pendingLinks.length === 0 ? (
              <Typography color="text.secondary">
                {t('statistics.report.noPendingLinks')}
              </Typography>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>{t('statistics.report.pendingLink.reporter')}</strong></TableCell>
                    {report.global && (
                      <TableCell><strong>{t('statistics.report.pendingLink.church')}</strong></TableCell>
                    )}
                    <TableCell><strong>{t('statistics.report.pendingLink.activeDate')}</strong></TableCell>
                    <TableCell><strong>{t('statistics.report.pendingLink.link')}</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {report.pendingLinks.map((link) => (
                    <TableRow key={link.token}>
                      <TableCell>
                        {link.reporterFullName
                          ? `${link.reporterFullName} (${link.reporterUsername})`
                          : link.reporterUsername}
                      </TableCell>
                      {report.global && (
                        <TableCell>{link.churchName}</TableCell>
                      )}
                      <TableCell>{formatDate(link.activeDate, i18n.resolvedLanguage)}</TableCell>
                      <TableCell>
                        <Link href={`/r/${link.token}`} target="_blank" rel="noreferrer">
                          /r/{link.token}
                        </Link>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Paper>
        </Box>
      )}
    </>
  );
}
