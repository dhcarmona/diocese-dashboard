import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableSortLabel from '@mui/material/TableSortLabel';
import Typography from '@mui/material/Typography';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { type ChartDrillDown, type DrillDownRow, getDrillDown } from '../api/statistics';
import PageHeader from '../components/PageHeader';
import { formatDate } from '../utils/dateFormatting';

type SortKey = 'church' | 'filledBy' | 'value';
type SortDir = 'asc' | 'desc';

function formatValue(value: number, type: string): string {
  if (type === 'DOLLARS')
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  if (type === 'COLONES')
    return `₡${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  return value.toLocaleString('en-US');
}

function sortRows(rows: DrillDownRow[], key: SortKey, dir: SortDir): DrillDownRow[] {
  return [...rows].sort((a, b) => {
    let cmp = 0;
    if (key === 'church') {
      cmp = a.churchName.localeCompare(b.churchName);
    } else if (key === 'filledBy') {
      const aName = a.filledByFullName ?? a.filledByUsername ?? '';
      const bName = b.filledByFullName ?? b.filledByUsername ?? '';
      cmp = aName.localeCompare(bName);
    } else {
      cmp = a.value - b.value;
    }
    return dir === 'asc' ? cmp : -cmp;
  });
}

export default function StatisticsChartDrillDownPage() {
  const { templateId } = useParams<{ templateId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();

  const itemId = searchParams.get('itemId') ?? '';
  const date = searchParams.get('date') ?? '';
  const churchName = searchParams.get('churchName') ?? undefined;
  const startDate = searchParams.get('startDate') ?? '';
  const endDate = searchParams.get('endDate') ?? '';

  const [data, setData] = useState<ChartDrillDown | null>(null);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [sortKey, setSortKey] = useState<SortKey>('value');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  useEffect(() => {
    if (!templateId || !itemId || !date) return;
    let active = true;

    async function load() {
      setLoading(true);
      setHasError(false);
      try {
        const result = await getDrillDown({
          templateId: Number(templateId),
          itemId: Number(itemId),
          date,
          churchName,
        });
        if (active) setData(result);
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => { active = false; };
  }, [templateId, itemId, date, churchName]);

  const sortedRows = useMemo(
    () => (data ? sortRows(data.rows, sortKey, sortDir) : []),
    [data, sortKey, sortDir],
  );

  function handleSort(key: SortKey) {
    if (key === sortKey) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortKey(key);
      setSortDir(key === 'value' ? 'desc' : 'asc');
    }
  }

  function buildReportUrl(instanceId: number): string {
    return `/reports/view/individual/${templateId ?? ''}/${instanceId}`;
  }

  function buildBackUrl(): string {
    const params = new URLSearchParams();
    if (churchName) params.set('churchName', churchName);
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    return `/statistics/${templateId ?? ''}/report?${params.toString()}`;
  }

  const unit = data
    ? t(`statistics.report.itemTypes.${data.itemType}`, { defaultValue: data.itemType })
    : '';

  return (
    <>
      <PageHeader
        title={t('statistics.drillDown.title')}
        subtitle={data?.itemTitle ?? ''}
      />

      <Button
        variant="outlined"
        size="small"
        sx={{ mb: 3 }}
        onClick={() => void navigate(buildBackUrl())}
      >
        {t('statistics.drillDown.backToReport')}
      </Button>

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('statistics.drillDown.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('statistics.drillDown.loadError')}</Alert>
      )}

      {!loading && !hasError && data && (
        <Paper elevation={2} sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 3, alignItems: 'center' }}>
            <Typography variant="h6" fontWeight={700}>
              {data.itemTitle}
              <Chip
                label={unit}
                size="small"
                sx={{ ml: 1, fontWeight: 600 }}
                color="primary"
                variant="outlined"
              />
            </Typography>
            <Typography variant="body1">
              {t('statistics.drillDown.dateLabel')}:{' '}
              <strong>{formatDate(data.date, i18n.resolvedLanguage)}</strong>
            </Typography>
          </Box>

          {sortedRows.length === 0 ? (
            <Typography color="text.secondary">
              {t('statistics.drillDown.noRows')}
            </Typography>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>
                    <strong>{t('statistics.drillDown.table.report')}</strong>
                  </TableCell>
                  <TableCell>
                    <TableSortLabel
                      active={sortKey === 'church'}
                      direction={sortKey === 'church' ? sortDir : 'asc'}
                      onClick={() => handleSort('church')}
                    >
                      <strong>{t('statistics.drillDown.table.church')}</strong>
                    </TableSortLabel>
                  </TableCell>
                  <TableCell>
                    <TableSortLabel
                      active={sortKey === 'filledBy'}
                      direction={sortKey === 'filledBy' ? sortDir : 'asc'}
                      onClick={() => handleSort('filledBy')}
                    >
                      <strong>{t('statistics.drillDown.table.filledBy')}</strong>
                    </TableSortLabel>
                  </TableCell>
                  <TableCell align="right">
                    <TableSortLabel
                      active={sortKey === 'value'}
                      direction={sortKey === 'value' ? sortDir : 'desc'}
                      onClick={() => handleSort('value')}
                    >
                      <strong>{t('statistics.drillDown.table.value')}</strong>
                    </TableSortLabel>
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sortedRows.map((row) => (
                  <TableRow key={row.serviceInstanceId}>
                    <TableCell>
                      <Link component={RouterLink} to={buildReportUrl(row.serviceInstanceId)}>
                        #{row.serviceInstanceId}
                      </Link>
                    </TableCell>
                    <TableCell>{row.churchName}</TableCell>
                    <TableCell>
                      {row.filledByFullName
                        ? `${row.filledByFullName} (${row.filledByUsername ?? ''})`
                        : (row.filledByUsername ?? '—')}
                    </TableCell>
                    <TableCell align="right">
                      {formatValue(row.value, data.itemType)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}
    </>
  );
}
