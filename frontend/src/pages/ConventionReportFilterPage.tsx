import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import PageHeader from '../components/PageHeader';
import { APP_DATE_FORMAT } from '../utils/dateFormatting';

export default function ConventionReportFilterPage() {
  const { t } = useTranslation();
  const { templateId } = useParams<{ templateId: string }>();
  const navigate = useNavigate();

  const [startDate, setStartDate] = useState<Dayjs | null>(
    dayjs().subtract(1, 'year').startOf('month'),
  );
  const [endDate, setEndDate] = useState<Dayjs | null>(dayjs());
  const [dateError, setDateError] = useState(false);

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
    void navigate(`/convention-report/${templateId ?? ''}/report?${params.toString()}`);
  }

  return (
    <>
      <PageHeader
        title={t('conventionReport.filter.title')}
        subtitle={t('conventionReport.filter.subtitle')}
      />

      <Paper elevation={2} sx={{ p: 4, maxWidth: 520 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <DatePicker
            label={t('conventionReport.filter.startDateLabel')}
            value={startDate}
            format={APP_DATE_FORMAT}
            onChange={(val) => {
              setStartDate(val);
              setDateError(false);
            }}
          />

          <DatePicker
            label={t('conventionReport.filter.endDateLabel')}
            value={endDate}
            format={APP_DATE_FORMAT}
            onChange={(val) => {
              setEndDate(val);
              setDateError(false);
            }}
          />

          {dateError && (
            <Alert severity="error">{t('conventionReport.filter.validationDateRange')}</Alert>
          )}

          <Button
            variant="contained"
            size="large"
            onClick={handleGenerate}
            disabled={!startDate || !endDate}
          >
            {t('conventionReport.filter.generate')}
          </Button>
        </Box>
      </Paper>
    </>
  );
}
