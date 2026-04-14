import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import Select from '@mui/material/Select';
import Typography from '@mui/material/Typography';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { getChurches, type Church } from '../api/churches';
import { useAuth } from '../auth/auth-context';
import PageHeader from '../components/PageHeader';
import { APP_DATE_FORMAT } from '../utils/dateFormatting';

const GLOBAL_VALUE = '__global__';

export default function StatisticsFilterPage() {
  const { t } = useTranslation();
  const { templateId } = useParams<{ templateId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [churches, setChurches] = useState<Church[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  const [churchName, setChurchName] = useState<string>('');
  const [startDate, setStartDate] = useState<Dayjs | null>(
    dayjs().subtract(1, 'year').startOf('month'),
  );
  const [endDate, setEndDate] = useState<Dayjs | null>(dayjs());
  const [dateError, setDateError] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadChurches() {
      setLoading(true);
      setHasError(false);
      try {
        const loaded = await getChurches();
        if (active) {
          setChurches(loaded);
          if (isAdmin) {
            setChurchName(GLOBAL_VALUE);
          } else if (loaded.length > 0) {
            setChurchName(loaded[0].name);
          }
        }
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadChurches();
    return () => { active = false; };
  }, [isAdmin]);

  function handleGenerate() {
    if (!startDate || !endDate) return;
    if (endDate.isBefore(startDate)) {
      setDateError(true);
      return;
    }
    setDateError(false);

    const params = new URLSearchParams({
      startDate: startDate.format('YYYY-MM-DD'),
      endDate: endDate.format('YYYY-MM-DD'),
    });
    if (churchName && churchName !== GLOBAL_VALUE) {
      params.set('churchName', churchName);
    }
    void navigate(`/statistics/${templateId ?? ''}/report?${params.toString()}`);
  }

  const churchOptions = isAdmin
    ? [{ name: GLOBAL_VALUE, label: t('statistics.filter.globalOption') }, ...churches.map((c) => ({
        name: c.name,
        label: c.name,
      }))]
    : churches.map((c) => ({ name: c.name, label: c.name }));

  return (
    <>
      <PageHeader
        title={t('statistics.filter.title')}
        subtitle={t('statistics.filter.subtitle')}
      />

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
          <CircularProgress size={24} />
          <Typography>{t('statistics.filter.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error" sx={{ mb: 3 }}>{t('statistics.filter.loadError')}</Alert>
      )}

      {!loading && !hasError && (
        <Paper elevation={2} sx={{ p: 4, maxWidth: 520 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <FormControl fullWidth>
              <InputLabel id="church-select-label">
                {t('statistics.filter.churchLabel')}
              </InputLabel>
              <Select
                labelId="church-select-label"
                value={churchName}
                label={t('statistics.filter.churchLabel')}
                onChange={(e) => setChurchName(e.target.value)}
              >
                {churchOptions.map((opt) => (
                  <MenuItem key={opt.name} value={opt.name}>
                    {opt.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <DatePicker
              label={t('statistics.filter.startDateLabel')}
              value={startDate}
              format={APP_DATE_FORMAT}
              onChange={(val) => {
                setStartDate(val);
                setDateError(false);
              }}
            />

            <DatePicker
              label={t('statistics.filter.endDateLabel')}
              value={endDate}
              format={APP_DATE_FORMAT}
              onChange={(val) => {
                setEndDate(val);
                setDateError(false);
              }}
            />

            {dateError && (
              <Alert severity="error">{t('statistics.filter.validationDateRange')}</Alert>
            )}

            <Button
              variant="contained"
              size="large"
              onClick={handleGenerate}
              disabled={!startDate || !endDate}
            >
              {t('statistics.filter.generate')}
            </Button>
          </Box>
        </Paper>
      )}
    </>
  );
}
