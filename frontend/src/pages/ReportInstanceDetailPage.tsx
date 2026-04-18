import Alert from '@mui/material/Alert';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import InputAdornment from '@mui/material/InputAdornment';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { type FormEvent, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { type Celebrant, getCelebrants } from '../api/celebrants';
import {
  type ResponseDetail,
  type ServiceInstanceDetail,
  deleteInstance,
  getInstanceDetail,
  updateInstance,
} from '../api/serviceInstances';
import PageHeader from '../components/PageHeader';
import { formatDate } from '../utils/dateFormatting';
import { formatMoneyDisplay, parseMoneyInput } from '../utils/moneyFormatting';

function getAdornment(type: ResponseDetail['serviceInfoItemType']): string | null {
  if (type === 'DOLLARS') return '$';
  if (type === 'COLONES') return '₡';
  return null;
}

type NotifyDialogMode = 'edit' | 'delete' | null;

export default function ReportInstanceDetailPage() {
  const { templateId, instanceId } = useParams<{
    templateId: string;
    instanceId: string;
  }>();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<ServiceInstanceDetail | null>(null);
  const [responses, setResponses] = useState<Record<number, string>>({});
  const [allCelebrants, setAllCelebrants] = useState<Celebrant[]>([]);
  const [selectedCelebrants, setSelectedCelebrants] = useState<Celebrant[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(false);

  const [focusedItemId, setFocusedItemId] = useState<number | null>(null);

  // Two-step dialogs
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [notifyDialogMode, setNotifyDialogMode] = useState<NotifyDialogMode>(null);

  // Pending action state while notify dialog is open
  const [pendingNotifyCallback, setPendingNotifyCallback] = useState<
    ((notify: boolean) => void) | null
  >(null);

  useEffect(() => {
    const parsedId = Number(instanceId);
    if (!instanceId || !Number.isFinite(parsedId)) {
      setHasError(true);
      setLoading(false);
      return;
    }

    let active = true;

    async function load() {
      setLoading(true);
      setHasError(false);
      try {
        const [data, celebrants] = await Promise.all([
          getInstanceDetail(parsedId),
          getCelebrants(),
        ]);
        if (active) {
          setDetail(data);
          setAllCelebrants(celebrants);
          setSelectedCelebrants(
            celebrants.filter((c) => data.celebrants.some((dc) => dc.id === c.id)),
          );
          const initial: Record<number, string> = {};
          data.responses.forEach((r) => {
            initial[r.serviceInfoItemId] = r.responseValue ?? '';
          });
          setResponses(initial);
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
  }, [instanceId]);

  function handleResponseChange(itemId: number, value: string) {
    setResponses((prev) => ({ ...prev, [itemId]: value }));
    setSaveSuccess(false);
  }

  function handleSaveClick(e: FormEvent) {
    e.preventDefault();
    setSaveError(false);
    setSaveSuccess(false);
    // Open the notify dialog; actual save happens after the user chooses
    setNotifyDialogMode('edit');
    setPendingNotifyCallback(() => (notify: boolean) => void performSave(notify));
  }

  async function performSave(notify: boolean) {
    if (!detail) return;
    setSaving(true);
    setSaveError(false);
    try {
      const entries = detail.responses.map((r) => ({
        serviceInfoItemId: r.serviceInfoItemId,
        responseValue: responses[r.serviceInfoItemId] ?? '',
      }));
      const updated = await updateInstance(
        detail.id,
        entries,
        notify,
        selectedCelebrants.map((c) => c.id),
      );
      setDetail(updated);
      setSaveSuccess(true);
    } catch {
      setSaveError(true);
    } finally {
      setSaving(false);
    }
  }

  function handleDeleteClick() {
    setDeleteError(false);
    setDeleteConfirmOpen(true);
  }

  function handleDeleteConfirmed() {
    setDeleteConfirmOpen(false);
    setNotifyDialogMode('delete');
    setPendingNotifyCallback(() => (notify: boolean) => void performDelete(notify));
  }

  async function performDelete(notify: boolean) {
    if (!detail) return;
    setDeleting(true);
    setDeleteError(false);
    try {
      await deleteInstance(detail.id, notify);
      navigate(`/reports/view/individual/${templateId ?? ''}`);
    } catch {
      setDeleteError(true);
      setDeleting(false);
    }
  }

  function handleNotifyChoice(notify: boolean) {
    const cb = pendingNotifyCallback;
    setNotifyDialogMode(null);
    setPendingNotifyCallback(null);
    if (cb) cb(notify);
  }

  const listPath = `/reports/view/individual/${templateId ?? ''}`;

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 4 }}>
        <CircularProgress size={32} />
        <Typography variant="h6">{t('reportDetail.loading')}</Typography>
      </Box>
    );
  }

  if (hasError || !detail) {
    return (
      <>
        <PageHeader title={t('reportDetail.title')} subtitle={t('reportDetail.loadError')} />
        <Alert severity="error">{t('reportDetail.loadError')}</Alert>
        <Button component={RouterLink} to={listPath} sx={{ mt: 2 }}>
          {t('reportDetail.backToList')}
        </Button>
      </>
    );
  }

  const reporterLabel = detail.submittedByFullName
    ?? detail.submittedByUsername
    ?? t('reportsList.unknownReporter');

  return (
    <>
      <PageHeader title={detail.templateName} subtitle={t('reportDetail.title')} />

      {/* Metadata */}
      <Stack spacing={0.5} sx={{ mb: 3 }}>
        <Typography variant="body2" color="text.secondary">
          <strong>{t('reportDetail.meta.date')}:</strong>{' '}
          {formatDate(detail.serviceDate, i18n.resolvedLanguage)}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          <strong>{t('reportDetail.meta.church')}:</strong> {detail.churchName}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          <strong>{t('reportDetail.meta.reporter')}:</strong> {reporterLabel}
        </Typography>
      </Stack>

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
            label={t('reportDetail.meta.celebrants')}
            placeholder={selectedCelebrants.length === 0
              ? t('submitService.fields.celebrantsPlaceholder')
              : ''}
          />
        )}
        sx={{ mb: 3, maxWidth: 640 }}
      />

      <Divider sx={{ mb: 3 }} />

      <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
        {t('reportDetail.responsesTitle')}
      </Typography>

      <Box
        component="form"
        noValidate
        onSubmit={(e) => handleSaveClick(e)}
        sx={{ maxWidth: 640 }}
      >
        <Stack spacing={3}>
          {detail.responses.map((r) => {
            const isMoney =
              r.serviceInfoItemType === 'DOLLARS' || r.serviceInfoItemType === 'COLONES';
            const isNumeric = isMoney || r.serviceInfoItemType === 'NUMERICAL';
            const adornment = getAdornment(r.serviceInfoItemType);
            const rawValue = responses[r.serviceInfoItemId] ?? '';
            return (
              <TextField
                key={r.serviceInfoItemId}
                label={r.serviceInfoItemTitle}
                helperText={r.serviceInfoItemDescription ?? undefined}
                required={r.required ?? false}
                value={
                  isMoney && focusedItemId !== r.serviceInfoItemId
                    ? formatMoneyDisplay(rawValue)
                    : rawValue
                }
                onChange={(e) =>
                  handleResponseChange(
                    r.serviceInfoItemId,
                    isMoney ? parseMoneyInput(e.target.value) : e.target.value,
                  )
                }
                onFocus={isMoney ? () => setFocusedItemId(r.serviceInfoItemId) : undefined}
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

          {saveError && (
            <Alert severity="error">{t('reportDetail.saveError')}</Alert>
          )}
          {deleteError && (
            <Alert severity="error">{t('reportDetail.deleteError')}</Alert>
          )}
          {saveSuccess && (
            <Alert severity="success">{t('reportDetail.saveSuccess')}</Alert>
          )}

          <Stack direction="row" spacing={2} flexWrap="wrap">
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={saving || deleting}
            >
              {saving ? (
                <CircularProgress size={22} color="inherit" />
              ) : (
                t('reportDetail.actions.save')
              )}
            </Button>
            <Button
              variant="outlined"
              color="error"
              size="large"
              disabled={saving || deleting}
              onClick={handleDeleteClick}
            >
              {deleting ? (
                <CircularProgress size={22} color="inherit" />
              ) : (
                t('reportDetail.actions.delete')
              )}
            </Button>
            <Button component={RouterLink} to={listPath} variant="text" size="large">
              {t('reportDetail.backToList')}
            </Button>
          </Stack>
        </Stack>
      </Box>

      {/* Delete confirmation dialog */}
      <Dialog open={deleteConfirmOpen} onClose={() => setDeleteConfirmOpen(false)}>
        <DialogTitle>{t('reportDetail.deleteConfirmDialog.title')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('reportDetail.deleteConfirmDialog.message')}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirmOpen(false)}>
            {t('reportDetail.deleteConfirmDialog.cancel')}
          </Button>
          <Button onClick={handleDeleteConfirmed} color="error" variant="contained">
            {t('reportDetail.deleteConfirmDialog.confirm')}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Notify reporter dialog (used for both edit and delete) */}
      <Dialog open={notifyDialogMode !== null} onClose={() => handleNotifyChoice(false)}>
        <DialogTitle>
          {notifyDialogMode === 'edit'
            ? t('reportDetail.notifyDialog.editTitle')
            : t('reportDetail.notifyDialog.deleteTitle')}
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            {notifyDialogMode === 'edit'
              ? t('reportDetail.notifyDialog.editMessage')
              : t('reportDetail.notifyDialog.deleteMessage')}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => handleNotifyChoice(false)}>
            {t('reportDetail.notifyDialog.no')}
          </Button>
          <Button onClick={() => handleNotifyChoice(true)} variant="contained">
            {t('reportDetail.notifyDialog.yes')}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
