import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import FormControl from '@mui/material/FormControl';
import IconButton from '@mui/material/IconButton';
import InputLabel from '@mui/material/InputLabel';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs, { type Dayjs } from 'dayjs';
import { type FormEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type Church, getChurches } from '../api/churches';
import {
  type LinkSchedule,
  type LinkSchedulePayload,
  createLinkSchedule,
  deleteLinkSchedule,
  getLinkSchedules,
  updateLinkSchedule,
} from '../api/linkSchedules';
import {
  createReporterLinksBulk,
  getReporterLinks,
  revokeReporterLink,
  type ReporterLink,
} from '../api/reporterLinks';
import { type ServiceTemplate, getServiceTemplates } from '../api/serviceTemplates';
import PageHeader from '../components/PageHeader';
import ScheduleDialog, { type ScheduleFormValues } from '../components/ScheduleDialog';
import { APP_DATE_FORMAT, formatDate } from '../utils/dateFormatting';

type FeedbackSeverity = 'success' | 'error' | 'warning';

interface FeedbackState {
  severity: FeedbackSeverity;
  message: string;
}

export default function ReporterLinkManagementPage() {
  const { t, i18n } = useTranslation();

  const [templates, setTemplates] = useState<ServiceTemplate[]>([]);
  const [allChurches, setAllChurches] = useState<Church[]>([]);
  const [activeLinks, setActiveLinks] = useState<ReporterLink[]>([]);
  const [schedules, setSchedules] = useState<LinkSchedule[]>([]);
  const [loadingData, setLoadingData] = useState(true);
  const [loadError, setLoadError] = useState(false);

  const [selectedTemplateId, setSelectedTemplateId] = useState<number | ''>('');
  const [selectedDate, setSelectedDate] = useState<Dayjs | null>(dayjs());
  const [selectedChurches, setSelectedChurches] = useState<string[]>([]);

  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  // Schedule dialog state
  const [scheduleDialogOpen, setScheduleDialogOpen] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<LinkSchedule | null>(null);
  const [createDialogKey, setCreateDialogKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadAll() {
      setLoadingData(true);
      setLoadError(false);
      try {
        const [loadedTemplates, loadedChurches, loadedLinks, loadedSchedules] = await Promise.all([
          getServiceTemplates(),
          getChurches(),
          getReporterLinks(),
          getLinkSchedules(),
        ]);
        if (!active) return;
        setTemplates(loadedTemplates);
        setAllChurches(loadedChurches);
        setActiveLinks(loadedLinks);
        setSchedules(loadedSchedules);
      } catch {
        if (active) setLoadError(true);
      } finally {
        if (active) setLoadingData(false);
      }
    }

    void loadAll();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!feedback) return undefined;
    const id = window.setTimeout(() => setFeedback(null), 6000);
    return () => window.clearTimeout(id);
  }, [feedback]);

  function toggleChurch(churchName: string) {
    setSelectedChurches((prev) =>
      prev.includes(churchName)
        ? prev.filter((n) => n !== churchName)
        : [...prev, churchName],
    );
  }

  function selectAllChurches() {
    setSelectedChurches(allChurches.map((c) => c.name));
  }

  function clearChurches() {
    setSelectedChurches([]);
  }

  function resetForm() {
    setSelectedTemplateId('');
    setSelectedDate(null);
    setSelectedChurches([]);
  }

  async function handleCreateLinks(e: FormEvent) {
    e.preventDefault();
    if (!selectedTemplateId || !selectedDate || !selectedDate.isValid()) return;
    if (selectedChurches.length === 0) {
      setFeedback({ severity: 'error', message: t('reporterLinks.validation.churchesRequired') });
      return;
    }

    setSubmitting(true);
    setFeedback(null);

    try {
      const result = await createReporterLinksBulk({
        serviceTemplateId: selectedTemplateId as number,
        activeDate: selectedDate.format('YYYY-MM-DD'),
        churchNames: selectedChurches,
      });

      const reloaded = await getReporterLinks();
      setActiveLinks(reloaded);
      resetForm();

      if (result.skippedChurches.length > 0) {
        setFeedback({
          severity: 'warning',
          message: t('reporterLinks.feedback.partialSuccess', {
            count: result.created.length,
            skipped: result.skippedChurches.join(', '),
          }),
        });
      } else {
        setFeedback({
          severity: 'success',
          message: t('reporterLinks.feedback.created', { count: result.created.length }),
        });
      }
    } catch {
      setFeedback({ severity: 'error', message: t('reporterLinks.feedback.createError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRevoke(token: string) {
    try {
      await revokeReporterLink(token);
      setActiveLinks((prev) => prev.filter((l) => l.token !== token));
      setFeedback({ severity: 'success', message: t('reporterLinks.feedback.revoked') });
    } catch {
      setFeedback({ severity: 'error', message: t('reporterLinks.feedback.revokeError') });
    }
  }

  function openCreateScheduleDialog() {
    setEditingSchedule(null);
    setCreateDialogKey((kk) => kk + 1);
    setScheduleDialogOpen(true);
  }

  function openEditScheduleDialog(schedule: LinkSchedule) {
    setEditingSchedule(schedule);
    setScheduleDialogOpen(true);
  }

  async function handleScheduleConfirm(values: ScheduleFormValues) {
    if (!editingSchedule && (!selectedTemplateId || selectedChurches.length === 0)) return;
    setScheduleDialogOpen(false);

    const payload: LinkSchedulePayload = {
      serviceTemplateId: editingSchedule
        ? editingSchedule.serviceTemplateId
        : (selectedTemplateId as number),
      churchNames: values.churchNames ?? selectedChurches,
      daysOfWeek: values.daysOfWeek,
      sendHour: values.sendHour,
    };

    try {
      if (editingSchedule) {
        const updated = await updateLinkSchedule(editingSchedule.id, payload);
        setSchedules((prev) => prev.map((ss) => (ss.id === updated.id ? updated : ss)));
        setFeedback({ severity: 'success', message: t('reporterLinks.feedback.scheduleUpdated') });
      } else {
        const created = await createLinkSchedule(payload);
        setSchedules((prev) => [...prev, created]);
        resetForm();
        setFeedback({ severity: 'success', message: t('reporterLinks.feedback.scheduleCreated') });
      }
    } catch {
      setFeedback({ severity: 'error', message: t('reporterLinks.feedback.scheduleError') });
    }
  }

  async function handleDeleteSchedule(id: number) {
    try {
      await deleteLinkSchedule(id);
      setSchedules((prev) => prev.filter((ss) => ss.id !== id));
      setFeedback({ severity: 'success', message: t('reporterLinks.feedback.scheduleDeleted') });
    } catch {
      setFeedback({
        severity: 'error',
        message: t('reporterLinks.feedback.scheduleDeleteError'),
      });
    }
  }

  if (loadingData) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 4 }}>
        <CircularProgress size={32} />
        <Typography variant="h6">{t('reporterLinks.loading')}</Typography>
      </Box>
    );
  }

  if (loadError) {
    return (
      <>
        <PageHeader
          title={t('areas.reporterLinks.title')}
          subtitle={t('areas.reporterLinks.description')}
        />
        <Alert severity="error">{t('reporterLinks.loadError')}</Alert>
      </>
    );
  }

  const canSubmit =
    selectedTemplateId !== '' &&
    selectedDate !== null &&
    selectedDate.isValid() &&
    selectedChurches.length > 0 &&
    !submitting;

  const canSchedule =
    selectedTemplateId !== '' && selectedChurches.length > 0 && !submitting;

  return (
    <>
      <PageHeader
        title={t('areas.reporterLinks.title')}
        subtitle={t('reporterLinks.subtitle')}
      />

      {feedback && (
        <Alert severity={feedback.severity} onClose={() => setFeedback(null)} sx={{ mb: 3 }}>
          {feedback.message}
        </Alert>
      )}

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Creation form */}
        <Card
          elevation={2}
          sx={{ borderRadius: 4 }}
          component="form"
          onSubmit={(e: FormEvent<HTMLFormElement>) => void handleCreateLinks(e)}
        >
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 2 }}>
              {t('reporterLinks.form.title')}
            </Typography>

            <Stack spacing={3}>
              {/* Step 1: Template */}
              <FormControl fullWidth required>
                <InputLabel id="template-label">
                  {t('reporterLinks.form.templateLabel')}
                </InputLabel>
                <Select
                  labelId="template-label"
                  label={t('reporterLinks.form.templateLabel')}
                  value={selectedTemplateId}
                  onChange={(e) => setSelectedTemplateId(e.target.value as number)}
                  disabled={submitting}
                >
                  {templates.map((tmpl) => (
                    <MenuItem key={tmpl.id} value={tmpl.id}>
                      {tmpl.serviceTemplateName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              {/* Step 2: Date */}
              <DatePicker
                label={t('reporterLinks.form.dateLabel')}
                value={selectedDate}
                onChange={(val) => setSelectedDate(val)}
                format={APP_DATE_FORMAT}
                disabled={submitting}
                slotProps={{
                  textField: { required: true, fullWidth: true },
                }}
              />

              {/* Step 3: Churches */}
              <Box>
                <Stack
                  direction="row"
                  alignItems="center"
                  justifyContent="space-between"
                  sx={{ mb: 1 }}
                >
                  <Typography variant="subtitle1" fontWeight={600}>
                    {t('reporterLinks.form.churchesLabel')}
                  </Typography>
                  <Stack direction="row" spacing={1}>
                    <Button
                      type="button"
                      size="small"
                      variant="outlined"
                      onClick={selectAllChurches}
                      disabled={submitting || allChurches.length === 0}
                    >
                      {t('reporterLinks.form.selectAll')}
                    </Button>
                    {selectedChurches.length > 0 && (
                      <Button
                        type="button"
                        size="small"
                        variant="text"
                        onClick={clearChurches}
                        disabled={submitting}
                      >
                        {t('reporterLinks.form.clearAll')}
                      </Button>
                    )}
                  </Stack>
                </Stack>

                {allChurches.length === 0 ? (
                  <Alert severity="info">{t('reporterLinks.form.noChurches')}</Alert>
                ) : (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {allChurches.map((church) => {
                      const selected = selectedChurches.includes(church.name);
                      return (
                        <Chip
                          key={church.name}
                          label={church.name}
                          onClick={() => !submitting && toggleChurch(church.name)}
                          color={selected ? 'primary' : 'default'}
                          variant={selected ? 'filled' : 'outlined'}
                          clickable={!submitting}
                        />
                      );
                    })}
                  </Box>
                )}

                {selectedChurches.length > 0 && (
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    {t('reporterLinks.form.selectedCount', { count: selectedChurches.length })}
                  </Typography>
                )}
              </Box>

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={!canSubmit}
                >
                  {submitting ? (
                    <CircularProgress size={22} color="inherit" />
                  ) : (
                    t('reporterLinks.form.submit')
                  )}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={!canSchedule}
                  onClick={openCreateScheduleDialog}
                >
                  {t('reporterLinks.form.scheduleButton')}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={submitting}
                  onClick={resetForm}
                >
                  {t('reporterLinks.form.reset')}
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>

        {/* Scheduled links */}
        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700}>
                {t('reporterLinks.schedules.title')}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t('reporterLinks.schedules.subtitle', { count: schedules.length })}
              </Typography>
            </Box>
            <Divider />

            {schedules.length === 0 ? (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">{t('reporterLinks.schedules.empty')}</Alert>
              </Box>
            ) : (
              <List disablePadding>
                {schedules.map((schedule, index) => (
                  <Box component="li" key={schedule.id} sx={{ listStyle: 'none' }}>
                    {index > 0 && <Divider component="div" />}
                    <ListItem
                      sx={{ px: 3, py: 2 }}
                      secondaryAction={
                        <Stack direction="row" spacing={0.5}>
                          <IconButton
                            edge="end"
                            aria-label={t('reporterLinks.schedules.edit')}
                            onClick={() => openEditScheduleDialog(schedule)}
                          >
                            <EditOutlinedIcon />
                          </IconButton>
                          <IconButton
                            edge="end"
                            aria-label={t('reporterLinks.schedules.delete')}
                            color="error"
                            onClick={() => void handleDeleteSchedule(schedule.id)}
                          >
                            <DeleteOutlineIcon />
                          </IconButton>
                        </Stack>
                      }
                    >
                      <ListItemText
                        primary={
                          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                            <Typography variant="body1" fontWeight={700}>
                              {schedule.serviceTemplateName}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              &mdash; {schedule.churchNames.join(', ')}
                            </Typography>
                          </Stack>
                        }
                        secondary={
                          <Stack component="span" spacing={0.25}>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {t('reporterLinks.schedules.days', {
                                days: schedule.daysOfWeek
                                  .map((dd) => t(`scheduleDialog.days.${dd}`))
                                  .join(', '),
                              })}
                            </Typography>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {t('reporterLinks.schedules.hour', { hour: schedule.sendHour })}
                            </Typography>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {schedule.lastTriggeredDate
                                ? t('reporterLinks.schedules.lastTriggered', {
                                    date: formatDate(
                                      schedule.lastTriggeredDate,
                                      i18n.resolvedLanguage,
                                    ),
                                  })
                                : t('reporterLinks.schedules.neverTriggered')}
                            </Typography>
                          </Stack>
                        }
                      />
                    </ListItem>
                  </Box>
                ))}
              </List>
            )}
          </CardContent>
        </Card>

        {/* Active links list */}
        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700}>
                {t('reporterLinks.list.title')}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {t('reporterLinks.list.subtitle', { count: activeLinks.length })}
              </Typography>
            </Box>
            <Divider />

            {activeLinks.length === 0 ? (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">{t('reporterLinks.list.empty')}</Alert>
              </Box>
            ) : (
              <List disablePadding>
                {activeLinks.map((link, index) => (
                  <Box component="li" key={link.token} sx={{ listStyle: 'none' }}>
                    {index > 0 && <Divider component="div" />}
                    <ListItem
                      sx={{ px: 3, py: 2 }}
                      secondaryAction={
                        <IconButton
                          edge="end"
                          aria-label={t('reporterLinks.actions.revoke')}
                          color="error"
                          onClick={() => void handleRevoke(link.token)}
                        >
                          <DeleteOutlineIcon />
                        </IconButton>
                      }
                    >
                      <ListItemText
                        primary={
                          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                            <Typography variant="body1" fontWeight={700}>
                              {link.serviceTemplateName}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              &mdash; {link.churchName}
                            </Typography>
                          </Stack>
                        }
                        secondary={
                          <Stack component="span" spacing={0.25}>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {t('reporterLinks.list.reporter', {
                                name: link.reporterFullName ?? link.reporterUsername,
                              })}
                            </Typography>
                            <Typography component="span" variant="body2" color="text.secondary">
                              {t('reporterLinks.list.activeDate', {
                                date: formatDate(link.activeDate, i18n.resolvedLanguage),
                              })}
                            </Typography>
                          </Stack>
                        }
                      />
                    </ListItem>
                  </Box>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      </Box>

      <ScheduleDialog
        key={editingSchedule ? `edit-${editingSchedule.id}` : `create-${createDialogKey}`}
        open={scheduleDialogOpen}
        initialValues={
          editingSchedule
            ? {
                daysOfWeek: editingSchedule.daysOfWeek,
                sendHour: editingSchedule.sendHour,
                churchNames: editingSchedule.churchNames,
              }
            : undefined
        }
        allChurches={editingSchedule ? allChurches : undefined}
        onConfirm={(values) => void handleScheduleConfirm(values)}
        onCancel={() => setScheduleDialogOpen(false)}
      />
    </>
  );
}
