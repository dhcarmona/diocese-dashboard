import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import Alert from '@mui/material/Alert';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import InputAdornment from '@mui/material/InputAdornment';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import dayjs from 'dayjs';
import { type FormEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Link as RouterLink,
  Navigate,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom';
import type { ReportSubmissionResponse } from '../api/reportSubmissions';
import { type Celebrant, getCelebrants } from '../api/celebrants';
import {
  getNextPublicReporterLink,
  getReporterLinkByToken,
  getReporterLinkPublic,
  submitViaReporterLink,
  submitViaReporterLinkPublic,
  type ReporterLink,
} from '../api/reporterLinks';
import {
  type SectionHeaderSummary,
  type ServiceInfoItemSummary,
  getServiceTemplateById,
} from '../api/serviceTemplates';
import { useAuth } from '../auth/auth-context';
import AppShell from '../components/AppShell';
import PageHeader from '../components/PageHeader';
import ReporterLinkFollowUpCard from '../components/ReporterLinkFollowUpCard';
import ServiceTemplateBanner from '../components/ServiceTemplateBanner';
import { formatDate } from '../utils/dateFormatting';
import { formatMoneyDisplay, parseMoneyInput } from '../utils/moneyFormatting';

function getInputAdornment(type: ServiceInfoItemSummary['serviceInfoItemType']): string | null {
  if (type === 'DOLLARS') return '$';
  if (type === 'COLONES') return '₡';
  return null;
}

interface InfoTemplateItem extends ServiceInfoItemSummary {
  kind: 'INFO_ITEM';
}

interface SectionHeaderTemplateItem extends SectionHeaderSummary {
  kind: 'SECTION_HEADER';
}

type TemplateItem = InfoTemplateItem | SectionHeaderTemplateItem;

function buildTemplateItems(
  infoItems: ServiceInfoItemSummary[],
  sectionHeaders: SectionHeaderSummary[],
): TemplateItem[] {
  const all: TemplateItem[] = [
    ...infoItems.map((item) => ({ ...item, kind: 'INFO_ITEM' as const })),
    ...sectionHeaders.map((h) => ({ ...h, kind: 'SECTION_HEADER' as const })),
  ];
  return all.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
}

export default function ReporterLinkPage() {
  const { token } = useParams<{ token: string }>();
  const { t, i18n } = useTranslation();
  const { user, status } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [link, setLink] = useState<ReporterLink | null>(null);
  const [bannerUrl, setBannerUrl] = useState<string | undefined>(undefined);
  const [templateItems, setTemplateItems] = useState<TemplateItem[]>([]);
  const [allCelebrants, setAllCelebrants] = useState<Celebrant[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<'notFound' | 'wrongUser' | 'generic' | null>(null);

  const [selectedCelebrants, setSelectedCelebrants] = useState<Celebrant[]>([]);
  const [responses, setResponses] = useState<Record<number, string>>({});

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(false);
  const [submissionResult, setSubmissionResult] = useState<ReportSubmissionResponse | null>(null);
  const [focusedItemId, setFocusedItemId] = useState<number | null>(null);
  const [openingNextPendingLink, setOpeningNextPendingLink] = useState(false);
  const [openNextPendingLinkError, setOpenNextPendingLinkError] = useState(false);
  const showHomeButton = status === 'authenticated';

  useEffect(() => {
    if (status === 'loading' || status === 'error' || !token) return;

    let active = true;
    setSubmissionResult(null);
    setSubmitError(false);
    setOpenNextPendingLinkError(false);
    setResponses({});
    setSelectedCelebrants([]);
    setLoading(true);
    setLoadError(null);

    async function loadData() {
      try {
        if (status === 'authenticated') {
          const loadedLink = await getReporterLinkByToken(token!);
          if (!active) return;

          if (loadedLink.reporterId !== user!.id) {
            setLoadError('wrongUser');
            setLoading(false);
            return;
          }

          const [loadedTemplate, loadedCelebrants] = await Promise.all([
            getServiceTemplateById(loadedLink.serviceTemplateId),
            getCelebrants(),
          ]);
          if (!active) return;

          setLink(loadedLink);
          setBannerUrl(loadedTemplate.bannerUrl);
          setTemplateItems(buildTemplateItems(
            loadedTemplate.serviceInfoItems ?? [],
            loadedTemplate.sectionHeaders ?? [],
          ));
          setAllCelebrants(loadedCelebrants);
        } else {
          // Unauthenticated: use the public endpoint; the token is the credential.
          const publicData = await getReporterLinkPublic(token!);
          if (!active) return;

          setLink({
            id: publicData.id,
            token: publicData.token,
            reporterId: 0,
            reporterUsername: '',
            reporterFullName: null,
            churchName: publicData.churchName,
            serviceTemplateId: publicData.serviceTemplateId,
            serviceTemplateName: publicData.serviceTemplateName,
            activeDate: publicData.activeDate,
          });
          setBannerUrl(publicData.bannerUrl);
          setTemplateItems(buildTemplateItems(publicData.serviceInfoItems, publicData.sectionHeaders ?? []));
          setAllCelebrants(publicData.celebrants);
        }
      } catch (err: unknown) {
        if (!active) return;
        const isAxiosError =
          typeof err === 'object' &&
          err !== null &&
          'response' in err &&
          typeof (err as { response?: { status?: number } }).response?.status === 'number';
        const status =
          isAxiosError ? (err as { response: { status: number } }).response.status : 0;
        setLoadError(status === 404 ? 'notFound' : 'generic');
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadData();
    return () => {
      active = false;
    };
  }, [status, token, user]);

  if (status === 'loading') {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (status === 'error') {
    return (
      <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname)}`} replace />
    );
  }

  if (loading) {
    return (
      <AppShell>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 4 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('reporterLink.loading')}</Typography>
        </Box>
      </AppShell>
    );
  }

  if (loadError === 'notFound') {
    return (
      <AppShell>
        <Alert severity="warning" sx={{ maxWidth: 640 }}>
          {t('reporterLink.notFound')}
        </Alert>
        {showHomeButton && (
          <Button component={RouterLink} to="/" sx={{ mt: 2 }}>
            {t('reporterLink.backHome')}
          </Button>
        )}
      </AppShell>
    );
  }

  if (loadError === 'wrongUser') {
    return (
      <AppShell>
        <Alert severity="error" sx={{ maxWidth: 640 }}>
          {t('reporterLink.wrongUser')}
        </Alert>
        {showHomeButton && (
          <Button component={RouterLink} to="/" sx={{ mt: 2 }}>
            {t('reporterLink.backHome')}
          </Button>
        )}
      </AppShell>
    );
  }

  if (loadError === 'generic' || !link) {
    return (
      <AppShell>
        <Alert severity="error" sx={{ maxWidth: 640 }}>
          {t('reporterLink.loadError')}
        </Alert>
        {showHomeButton && (
          <Button component={RouterLink} to="/" sx={{ mt: 2 }}>
            {t('reporterLink.backHome')}
          </Button>
        )}
      </AppShell>
    );
  }

  const today = dayjs().startOf('day');
  const activeDateObj = dayjs(link.activeDate);
  const formattedActiveDate = formatDate(link.activeDate, i18n.resolvedLanguage);
  const isNotYetActive = activeDateObj.isAfter(today);

  const infoItemsOnly = templateItems.filter(
    (item): item is InfoTemplateItem => item.kind === 'INFO_ITEM',
  );
  const hasRequiredStringItem = infoItemsOnly.some(
    (item) => item.serviceInfoItemType === 'STRING' && item.required,
  );
  const showNoCelebrationButton = !hasRequiredStringItem;

  if (isNotYetActive) {
    return (
      <AppShell>
        <PageHeader
          title={link.serviceTemplateName}
          subtitle={t('reporterLink.notYetActiveSubtitle')}
        />
        <Alert severity="info" sx={{ maxWidth: 640 }}>
          {t('reporterLink.notYetActive', {
            date: formattedActiveDate,
          })}
        </Alert>
        {showHomeButton && (
          <Button component={RouterLink} to="/" sx={{ mt: 2 }}>
            {t('reporterLink.backHome')}
          </Button>
        )}
      </AppShell>
    );
  }

  if (submissionResult) {
    return (
      <AppShell>
        <PageHeader title={link.serviceTemplateName} subtitle={link.churchName} />
        <Stack alignItems="center" spacing={2} sx={{ mt: 4 }}>
          <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64 }} />
          <Typography variant="h5" fontWeight={700} color="success.main">
            {t('submitService.successTitle')}
          </Typography>
          <Typography color="text.secondary">{t('submitService.successMessage')}</Typography>
          {openNextPendingLinkError && (
            <Alert severity="error" sx={{ maxWidth: 640 }}>
              {t('submitService.nextPendingError')}
            </Alert>
          )}
          <ReporterLinkFollowUpCard
            nextReporterLinkToken={
              status === 'authenticated' ? submissionResult.nextReporterLinkToken : null
            }
            nextReporterLinkActiveDate={submissionResult.nextReporterLinkActiveDate}
            onOpenNextPendingLink={
              status !== 'authenticated' && submissionResult.nextReporterLinkFollowUpToken
                ? () => void handleOpenNextPendingLink()
                : undefined
            }
            openingNextPendingLink={openingNextPendingLink}
          />
          {showHomeButton && (
            <Button variant="contained" component={RouterLink} to="/">
              {t('submitService.backHome')}
            </Button>
          )}
        </Stack>
      </AppShell>
    );
  }

  async function handleOpenNextPendingLink() {
    if (!submissionResult?.nextReporterLinkFollowUpToken) {
      return;
    }

    setOpeningNextPendingLink(true);
    setOpenNextPendingLinkError(false);
    try {
      const nextLink = await getNextPublicReporterLink(
        submissionResult.nextReporterLinkFollowUpToken,
      );
      navigate(`/r/${nextLink.nextReporterLinkToken}`);
    } catch {
      setOpenNextPendingLinkError(true);
    } finally {
      setOpeningNextPendingLink(false);
    }
  }

  async function handleNoCelebration() {
    if (!link) return;

    const responseEntries = infoItemsOnly
      .filter((item) => item.serviceInfoItemType !== 'STRING')
      .map((item) => ({
        serviceInfoItemId: item.id,
        responseValue: '0',
      }));

    setSubmitting(true);
    setSubmitError(false);

    try {
      if (status === 'authenticated') {
        const result = await submitViaReporterLink(link.token, {
          celebrantIds: [],
          responses: responseEntries,
        });
        setSubmissionResult(result);
      } else {
        const result = await submitViaReporterLinkPublic(link.token, {
          celebrantIds: [],
          responses: responseEntries,
        });
        setSubmissionResult(result);
      }
    } catch {
      setSubmitError(true);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!link) return;

    const infoItems = templateItems.filter(
      (item): item is InfoTemplateItem => item.kind === 'INFO_ITEM',
    );

    const hasMissingRequired = infoItems.some((item) => {
      const value = responses[item.id]?.trim() ?? '';
      return item.required && value === '';
    });

    if (hasMissingRequired) {
      setSubmitError(true);
      return;
    }

    setSubmitting(true);
    setSubmitError(false);

    const responseEntries = infoItems
      .map((item) => ({
        serviceInfoItemId: item.id,
        responseValue: responses[item.id]?.trim() ?? '',
      }))
      .filter((entry) => entry.responseValue !== '');

    try {
      if (status === 'authenticated') {
        const result = await submitViaReporterLink(link.token, {
          celebrantIds: selectedCelebrants.map((c) => c.id),
          responses: responseEntries,
        });
        setSubmissionResult(result);
      } else {
        const result = await submitViaReporterLinkPublic(link.token, {
          celebrantIds: selectedCelebrants.map((c) => c.id),
          responses: responseEntries,
        });
        setSubmissionResult(result);
      }
    } catch {
      setSubmitError(true);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AppShell>
      <PageHeader title={link.serviceTemplateName} subtitle={link.churchName} />

      <ServiceTemplateBanner
        template={{ id: link.serviceTemplateId, serviceTemplateName: link.serviceTemplateName, linkOnly: false, bannerUrl }}
        sx={{ mb: 2 }}
      />

      <Box
        component="form"
        noValidate
        onSubmit={(e) => void handleSubmit(e)}
        sx={{ maxWidth: 640 }}
      >
        <Stack spacing={3}>
          <Alert severity="info">
            {t('reporterLink.fixedDateNotice', { date: formattedActiveDate })}
          </Alert>

          {showNoCelebrationButton && (
            <Button
              type="button"
              variant="contained"
              color="warning"
              onClick={() => void handleNoCelebration()}
              disabled={submitting}
            >
              {t('reporterLink.noCelebrationButton')}
            </Button>
          )}

          <TextField
            label={t('submitService.fields.date')}
            value={formattedActiveDate}
            fullWidth
            InputProps={{ readOnly: true }}
          />

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

          {templateItems.some((item) => item.kind === 'INFO_ITEM') && (
            <>
              <Divider />
              <Typography variant="subtitle1" fontWeight={600}>
                {t('submitService.infoItemsTitle')}
              </Typography>
            </>
          )}

          {templateItems.map((item) => {
            if (item.kind === 'SECTION_HEADER') {
              return (
                <Typography
                  key={`h${item.id}`}
                  variant="overline"
                  fontWeight={700}
                  color="text.secondary"
                  sx={{ display: 'block', mt: 1, letterSpacing: 1.2 }}
                >
                  {item.title}
                </Typography>
              );
            }
            const isMoney =
              item.serviceInfoItemType === 'DOLLARS' ||
              item.serviceInfoItemType === 'COLONES';
            const isNumeric = isMoney || item.serviceInfoItemType === 'NUMERICAL';
            const adornment = getInputAdornment(item.serviceInfoItemType);
            const rawValue = responses[item.id] ?? '';
            return (
              <TextField
                key={`i${item.id}`}
                label={item.title}
                helperText={item.description ?? undefined}
                required={item.required}
                value={isMoney && focusedItemId !== item.id ? formatMoneyDisplay(rawValue) : rawValue}
                onChange={(e) =>
                  setResponses((prev) => ({
                    ...prev,
                    [item.id]: isMoney ? parseMoneyInput(e.target.value) : e.target.value,
                  }))
                }
                onFocus={isMoney ? () => setFocusedItemId(item.id) : undefined}
                onBlur={isMoney ? () => setFocusedItemId(null) : undefined}
                type={isNumeric && !isMoney ? 'number' : 'text'}
                inputProps={
                  isMoney
                    ? { inputMode: 'decimal' }
                    : isNumeric
                      ? { min: 0, step: 'any' }
                      : undefined
                }
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
              disabled={submitting}
            >
              {submitting ? (
                <CircularProgress size={22} color="inherit" />
              ) : (
                t('submitService.submit')
              )}
            </Button>
            {showHomeButton && (
              <Button
                component={RouterLink}
                to="/"
                variant="outlined"
                size="large"
                disabled={submitting}
              >
                {t('reporterLink.backHome')}
              </Button>
            )}
          </Stack>
        </Stack>
      </Box>
    </AppShell>
  );
}
