import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import Alert from '@mui/material/Alert';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import FormControl from '@mui/material/FormControl';
import InputAdornment from '@mui/material/InputAdornment';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useParams } from 'react-router-dom';
import type { ReportSubmissionResponse } from '../api/reportSubmissions';
import { type Celebrant, getCelebrants } from '../api/celebrants';
import { type Church, getChurches } from '../api/churches';
import {
  type ServiceInfoItemSummary,
  type ServiceTemplate,
  getServiceTemplateById,
} from '../api/serviceTemplates';
import { submitServiceInstance } from '../api/serviceInstances';
import { useAuth } from '../auth/auth-context';
import PageHeader from '../components/PageHeader';
import ReporterLinkFollowUpCard from '../components/ReporterLinkFollowUpCard';

function getInputAdornment(type: ServiceInfoItemSummary['serviceInfoItemType']): string | null {
  if (type === 'DOLLARS') return '$';
  if (type === 'COLONES') return '₡';
  return null;
}

export default function ServiceSubmitPage() {
  const { templateId } = useParams<{ templateId: string }>();
  const { t } = useTranslation();
  const { user } = useAuth();

  const [template, setTemplate] = useState<ServiceTemplate | null>(null);
  const [allCelebrants, setAllCelebrants] = useState<Celebrant[]>([]);
  const [allChurches, setAllChurches] = useState<Church[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const [serviceDate, setServiceDate] = useState<Dayjs | null>(dayjs());
  const [selectedChurch, setSelectedChurch] = useState<Church | null>(null);
  const [selectedCelebrants, setSelectedCelebrants] = useState<Celebrant[]>([]);
  const [responses, setResponses] = useState<Record<number, string>>({});

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submissionResult, setSubmissionResult] = useState<ReportSubmissionResponse | null>(null);

  const availableChurches = useMemo(() => {
    if (!user || user.role === 'ADMIN') {
      return allChurches;
    }
    const assignedSet = new Set(user.assignedChurchNames);
    return allChurches.filter((c) => assignedSet.has(c.name));
  }, [allChurches, user]);

  useEffect(() => {
    setLoading(true);
    setLoadError(false);

    const parsedTemplateId = Number(templateId);
    if (!templateId || !Number.isFinite(parsedTemplateId)) {
      setLoadError(true);
      setLoading(false);
      return;
    }

    let active = true;

    Promise.all([
      getServiceTemplateById(parsedTemplateId),
      getCelebrants(),
      getChurches(),
    ])
      .then(([loadedTemplate, loadedCelebrants, loadedChurches]) => {
        if (!active) return;
        setTemplate(loadedTemplate);
        setAllCelebrants(loadedCelebrants);
        setAllChurches(loadedChurches);
      })
      .catch(() => {
        if (active) setLoadError(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [templateId]);

  useEffect(() => {
    if (availableChurches.length === 1) {
      setSelectedChurch(availableChurches[0]);
    }
  }, [availableChurches]);

  function handleResponseChange(itemId: number, value: string) {
    setResponses((prev) => ({ ...prev, [itemId]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!template || !selectedChurch || !serviceDate || !serviceDate.isValid()) return;

    setSubmitError(false);

    const serviceInfoItems = template.serviceInfoItems ?? [];
    const hasMissingRequired = serviceInfoItems.some((item) => {
      const value = responses[item.id]?.trim() ?? '';
      return item.required && value === '';
    });

    if (hasMissingRequired) {
      setSubmitError(true);
      return;
    }

    setSubmitting(true);

    const responseEntries = serviceInfoItems
      .map((item) => ({
        serviceInfoItemId: item.id,
        responseValue: responses[item.id]?.trim() ?? '',
      }))
      .filter((entry) => entry.responseValue !== '');

    try {
      const result = await submitServiceInstance(template.id, {
        churchName: selectedChurch.name,
        celebrantIds: selectedCelebrants.map((c) => c.id),
        serviceDate: serviceDate.format('YYYY-MM-DD'),
        responses: responseEntries,
      });
      setSubmissionResult(result);
      setSubmitSuccess(true);
    } catch {
      setSubmitError(true);
    } finally {
      setSubmitting(false);
    }
  }

  function handleSubmitAnother() {
    setSubmitSuccess(false);
    setSubmitError(false);
    setServiceDate(dayjs());
    setSubmissionResult(null);
    if (availableChurches.length !== 1) setSelectedChurch(null);
    setSelectedCelebrants([]);
    setResponses({});
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 4 }}>
        <CircularProgress size={32} />
        <Typography variant="h6">{t('submitService.loading')}</Typography>
      </Box>
    );
  }

  if (loadError || !template) {
    return (
      <>
        <PageHeader
        title={t('submitService.title')}
        subtitle={t('submitService.loadError')}
      />
        <Alert severity="error">{t('submitService.loadError')}</Alert>
        <Button component={RouterLink} to="/reports/new" sx={{ mt: 2 }}>
          {t('submitService.backToTemplates')}
        </Button>
      </>
    );
  }

  if (submitSuccess) {
    return (
      <>
        <PageHeader title={template.serviceTemplateName}
        subtitle={t('submitService.subtitle')} />
        <Stack alignItems="center" spacing={2} sx={{ mt: 4 }}>
          <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64 }} />
          <Typography variant="h5" fontWeight={700} color="success.main">
            {t('submitService.successTitle')}
          </Typography>
          <Typography color="text.secondary">{t('submitService.successMessage')}</Typography>
          {user?.role === 'REPORTER' && submissionResult && (
            <ReporterLinkFollowUpCard
              nextReporterLinkToken={submissionResult.nextReporterLinkToken}
              nextReporterLinkActiveDate={submissionResult.nextReporterLinkActiveDate}
            />
          )}
          <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
            <Button variant="outlined" onClick={handleSubmitAnother}>
              {t('submitService.submitAnother')}
            </Button>
            <Button variant="contained" component={RouterLink} to="/">
              {t('submitService.backHome')}
            </Button>
          </Stack>
        </Stack>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={template.serviceTemplateName}
        subtitle={t('submitService.subtitle')}
      />

      <Box
        component="form"
        noValidate
        onSubmit={(e) => void handleSubmit(e)}
        sx={{ maxWidth: 640 }}
      >
        <Stack spacing={3}>
          {/* Date */}
          <DatePicker
            label={t('submitService.fields.date')}
            value={serviceDate}
            onChange={(val) => setServiceDate(val)}
            maxDate={dayjs()}
            format="DD/MM/YYYY"
            slotProps={{
              textField: { required: true, fullWidth: true },
            }}
          />

          {/* Church */}
          {user?.role === 'REPORTER' && availableChurches.length === 0 ? (
            <Alert severity="warning">{t('submitService.noChurchesAssigned')}</Alert>
          ) : (
            <FormControl fullWidth required>
            <InputLabel id="church-label">{t('submitService.fields.church')}</InputLabel>
            <Select
              labelId="church-label"
              label={t('submitService.fields.church')}
              value={selectedChurch?.name ?? ''}
              onChange={(e) => {
                const church = availableChurches.find((c) => c.name === e.target.value) ?? null;
                setSelectedChurch(church);
              }}
            >
              {availableChurches.map((church) => (
                <MenuItem key={church.name} value={church.name}>
                  {church.name}
                </MenuItem>
              ))}
            </Select>
            </FormControl>
          )}

          {/* Celebrants — pills pattern */}
          <Autocomplete
            multiple
            options={allCelebrants}
            getOptionLabel={(option) => option.name}
            value={selectedCelebrants}
            onChange={(_e, newValue) => setSelectedCelebrants(newValue)}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => {
                const { key, ...tagProps } = getTagProps({ index });
                return <Chip key={key} label={option.name} {...tagProps} />;
              })
            }
            renderInput={(params) => (
              <TextField
                {...params}
                label={t('submitService.fields.celebrants')}
                placeholder={
                  selectedCelebrants.length === 0
                    ? t('submitService.fields.celebrantsPlaceholder')
                    : ''
                }
              />
            )}
          />

          {/* Dynamic info items */}
          {(template.serviceInfoItems ?? []).length > 0 && (
            <>
              <Divider />
              <Typography variant="subtitle1" fontWeight={600}>
                {t('submitService.infoItemsTitle')}
              </Typography>
            </>
          )}

          {(template.serviceInfoItems ?? []).map((item) => {
            const isNumeric =
              item.serviceInfoItemType === 'NUMERICAL' ||
              item.serviceInfoItemType === 'DOLLARS' ||
              item.serviceInfoItemType === 'COLONES';
            const adornment = getInputAdornment(item.serviceInfoItemType);
            return (
              <TextField
                key={item.id}
                label={item.title}
                helperText={item.description ?? undefined}
                required={item.required}
                value={responses[item.id] ?? ''}
                onChange={(e) => handleResponseChange(item.id, e.target.value)}
                type={isNumeric ? 'number' : 'text'}
                inputProps={isNumeric ? { min: 0, step: 'any' } : undefined}
                InputProps={
                  adornment
                    ? {
                        startAdornment: (
                          <InputAdornment position="start">{adornment}</InputAdornment>
                        ),
                      }
                    : undefined
                }
                fullWidth
              />
            );
          })}

          {submitError && (
            <Alert severity="error">{t('submitService.submitError')}</Alert>
          )}

          <Stack direction="row" spacing={2}>
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={
                submitting
                || !selectedChurch
                || !serviceDate
                || !serviceDate.isValid()
                || (user?.role === 'REPORTER' && availableChurches.length === 0)
              }
            >
              {submitting ? (
                <CircularProgress size={22} color="inherit" />
              ) : (
                t('submitService.submit')
              )}
            </Button>
            <Button
              component={RouterLink}
              to="/reports/new"
              variant="outlined"
              size="large"
              disabled={submitting}
            >
              {t('submitService.cancel')}
            </Button>
          </Stack>
        </Stack>
      </Box>
    </>
  );
}
