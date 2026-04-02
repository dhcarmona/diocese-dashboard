import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createCelebrant,
  deleteCelebrant,
  getCelebrants,
  updateCelebrant,
  type Celebrant,
} from '../api/celebrants';
import CelebrantPortrait from '../components/CelebrantPortrait';
import PageHeader from '../components/PageHeader';

type FeedbackSeverity = 'success' | 'error';

interface FeedbackState {
  severity: FeedbackSeverity;
  message: string;
}

type FormMode = 'create' | 'edit';

function sortCelebrants(celebrants: Celebrant[]): Celebrant[] {
  return [...celebrants].sort((left, right) =>
    left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }),
  );
}

export default function CelebrantManagementPage() {
  const { t } = useTranslation();
  const [celebrants, setCelebrants] = useState<Celebrant[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCelebrantId, setSelectedCelebrantId] = useState<number | null>(null);
  const [formMode, setFormMode] = useState<FormMode>('create');
  const [draftName, setDraftName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const sortedCelebrants = useMemo(() => sortCelebrants(celebrants), [celebrants]);
  const normalizedSearchTerm = searchTerm.trim().toLocaleLowerCase();
  const filteredCelebrants = useMemo(
    () =>
      sortedCelebrants.filter((celebrant) =>
        celebrant.name.toLocaleLowerCase().includes(normalizedSearchTerm),
      ),
    [normalizedSearchTerm, sortedCelebrants],
  );

  const selectedCelebrant = useMemo(
    () => celebrants.find((celebrant) => celebrant.id === selectedCelebrantId) ?? null,
    [celebrants, selectedCelebrantId],
  );

  const isEditing = formMode === 'edit' && selectedCelebrant !== null;

  useEffect(() => {
    let active = true;

    async function loadCelebrants() {
      setLoading(true);
      setLoadError(false);
      try {
        const loadedCelebrants = await getCelebrants();
        if (!active) {
          return;
        }
        setCelebrants(loadedCelebrants);
      } catch {
        if (active) {
          setLoadError(true);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadCelebrants();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!feedback) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setFeedback(null);
    }, 5000);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [feedback]);

  function resetForm() {
    setFormMode('create');
    setSelectedCelebrantId(null);
    setDraftName('');
  }

  function handleCreateMode() {
    resetForm();
    setFeedback(null);
  }

  function handleSelectCelebrant(celebrant: Celebrant) {
    setFormMode('edit');
    setSelectedCelebrantId(celebrant.id);
    setDraftName(celebrant.name);
    setFeedback(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = draftName.trim();

    if (!trimmedName) {
      setFeedback({
        severity: 'error',
        message: t('celebrants.validation.nameRequired'),
      });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      if (selectedCelebrant) {
        const updatedCelebrant = await updateCelebrant(selectedCelebrant.id, { name: trimmedName });
        setCelebrants((currentCelebrants) =>
          currentCelebrants.map((celebrant) =>
            celebrant.id === updatedCelebrant.id ? updatedCelebrant : celebrant,
          ),
        );
        setSelectedCelebrantId(updatedCelebrant.id);
        setFormMode('edit');
        setDraftName(updatedCelebrant.name);
        setFeedback({
          severity: 'success',
          message: t('celebrants.feedback.updated', { name: updatedCelebrant.name }),
        });
      } else {
        const createdCelebrant = await createCelebrant({ name: trimmedName });
        setCelebrants((currentCelebrants) =>
          [...currentCelebrants, createdCelebrant],
        );
        resetForm();
        setFeedback({
          severity: 'success',
          message: t('celebrants.feedback.created', { name: createdCelebrant.name }),
        });
      }
    } catch {
      setFeedback({
        severity: 'error',
        message: t('celebrants.feedback.saveError'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteCelebrant() {
    if (!selectedCelebrant) {
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteCelebrant(selectedCelebrant.id);
      setCelebrants((currentCelebrants) =>
        currentCelebrants.filter((celebrant) => celebrant.id !== selectedCelebrant.id),
      );
      resetForm();
      setFeedback({
        severity: 'success',
        message: t('celebrants.feedback.deleted', { name: selectedCelebrant.name }),
      });
    } catch {
      setFeedback({
        severity: 'error',
        message: t('celebrants.feedback.deleteError'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('areas.celebrants.title')}
        subtitle={t('celebrants.subtitle')}
      />

      {feedback && (
        <Alert
          severity={feedback.severity}
          onClose={() => setFeedback(null)}
          sx={{ mb: 3 }}
        >
          {feedback.message}
        </Alert>
      )}

      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          gap: 3,
        }}
      >
        <Card
          elevation={2}
          sx={{ borderRadius: 4 }}
          component="form"
          onSubmit={(event: FormEvent<HTMLFormElement>) => void handleSubmit(event)}
        >
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
              {t(isEditing ? 'celebrants.form.editTitle' : 'celebrants.form.createTitle')}
            </Typography>

            <Stack spacing={2.5}>
              {selectedCelebrant && (
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    py: 1,
                  }}
                >
                  <CelebrantPortrait
                    celebrant={selectedCelebrant}
                    size={240}
                    testId="celebrant-form-portrait"
                  />
                </Box>
              )}

              <TextField
                label={t('celebrants.form.nameLabel')}
                placeholder={t('celebrants.form.namePlaceholder')}
                value={draftName}
                onChange={(event) => setDraftName(event.target.value)}
                disabled={submitting}
                autoComplete="off"
                fullWidth
              />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={submitting}
                  startIcon={isEditing ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
                >
                  {t(isEditing ? 'celebrants.actions.save' : 'celebrants.actions.create')}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={submitting}
                  onClick={handleCreateMode}
                >
                  {t('celebrants.actions.reset')}
                </Button>
              </Stack>

              {isEditing && (
                <Button
                  type="button"
                  color="error"
                  variant="text"
                  size="large"
                  disabled={submitting}
                  startIcon={<DeleteOutlineIcon />}
                  onClick={() => void handleDeleteCelebrant()}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {t('celebrants.actions.delete')}
                </Button>
              )}
            </Stack>
          </CardContent>
        </Card>

        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
                {t('celebrants.list.title')}
              </Typography>
              <TextField
                fullWidth
                label={t('celebrants.search.label')}
                placeholder={t('celebrants.search.placeholder')}
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlinedIcon />
                    </InputAdornment>
                  ),
                  endAdornment: searchTerm ? (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label={t('celebrants.actions.clearSearch')}
                        edge="end"
                        onClick={() => setSearchTerm('')}
                      >
                        <CloseOutlinedIcon />
                      </IconButton>
                    </InputAdornment>
                  ) : undefined,
                }}
              />
            </Box>
            <Divider />

            {loading && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 3 }}>
                <CircularProgress size={28} />
                <Typography variant="body1">{t('celebrants.list.loading')}</Typography>
              </Box>
            )}

            {!loading && loadError && (
              <Box sx={{ p: 3 }}>
                <Alert severity="error">{t('celebrants.list.loadError')}</Alert>
              </Box>
            )}

            {!loading && !loadError && filteredCelebrants.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">
                  {celebrants.length === 0
                    ? t('celebrants.list.empty')
                    : t('celebrants.list.noMatches')}
                </Alert>
              </Box>
            )}

            {!loading && !loadError && filteredCelebrants.length > 0 && (
              <List disablePadding>
                {filteredCelebrants.map((celebrant, index) => {
                  const isSelected = celebrant.id === selectedCelebrantId;
                  return (
                    <Box component="li" key={celebrant.id} sx={{ listStyle: 'none' }}>
                      {index > 0 && <Divider component="div" />}
                        <ListItemButton
                          selected={isSelected}
                          onClick={() => handleSelectCelebrant(celebrant)}
                          sx={{ px: 3, py: 2.5, alignItems: 'center' }}
                        >
                          <CelebrantPortrait
                            celebrant={celebrant}
                            sx={{
                              mr: 2,
                              color: isSelected ? 'primary.main' : 'action.active',
                            }}
                          />
                          <ListItemText
                            primary={
                            <Typography variant="h6" fontWeight={700} sx={{ mb: 0.5 }}>
                              {celebrant.name}
                            </Typography>
                          }
                          secondary={
                            <Typography variant="body2" color="text.secondary">
                              {t('celebrants.list.identifier', { id: celebrant.id })}
                            </Typography>
                          }
                        />
                        <Stack
                          direction="row"
                          spacing={1}
                          alignItems="center"
                          color={isSelected ? 'primary.main' : 'text.secondary'}
                          sx={{ ml: 2 }}
                        >
                          <EditOutlinedIcon fontSize="small" />
                          <Typography variant="body2" fontWeight={700}>
                            {t('celebrants.actions.edit')}
                          </Typography>
                        </Stack>
                      </ListItemButton>
                    </Box>
                  );
                })}
              </List>
            )}

            <Divider />
            <Box sx={{ px: 3, py: 2 }}>
              <Typography variant="body2" color="text.secondary">
                {t('celebrants.summary.total')}: {celebrants.length}
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
