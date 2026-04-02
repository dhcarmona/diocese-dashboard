import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import LocationCityOutlinedIcon from '@mui/icons-material/LocationCityOutlined';
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
  createChurch,
  deleteChurch,
  getChurches,
  updateChurch,
  type Church,
} from '../api/churches';
import PageHeader from '../components/PageHeader';

type FeedbackSeverity = 'success' | 'error';

interface FeedbackState {
  severity: FeedbackSeverity;
  message: string;
}

type FormMode = 'create' | 'edit';

function sortChurches(churches: Church[]): Church[] {
  return [...churches].sort((left, right) =>
    left.name.localeCompare(right.name, undefined, { sensitivity: 'base' }),
  );
}

function normalizeLocation(location: string | null | undefined): string {
  return location?.trim() ?? '';
}

export default function ChurchManagementPage() {
  const { t } = useTranslation();
  const [churches, setChurches] = useState<Church[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedChurchName, setSelectedChurchName] = useState<string | null>(null);
  const [formMode, setFormMode] = useState<FormMode>('create');
  const [draftName, setDraftName] = useState('');
  const [draftLocation, setDraftLocation] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const sortedChurches = useMemo(() => sortChurches(churches), [churches]);
  const normalizedSearchTerm = searchTerm.trim().toLocaleLowerCase();
  const filteredChurches = useMemo(
    () =>
      sortedChurches.filter((church) => {
        const nameMatches = church.name.toLocaleLowerCase().includes(normalizedSearchTerm);
        const locationMatches = normalizeLocation(church.location)
          .toLocaleLowerCase()
          .includes(normalizedSearchTerm);
        return nameMatches || locationMatches;
      }),
    [normalizedSearchTerm, sortedChurches],
  );

  const selectedChurch = useMemo(
    () => churches.find((church) => church.name === selectedChurchName) ?? null,
    [churches, selectedChurchName],
  );

  const isEditing = formMode === 'edit' && selectedChurch !== null;

  useEffect(() => {
    let active = true;

    async function loadChurches() {
      setLoading(true);
      setLoadError(false);
      try {
        const loadedChurches = await getChurches();
        if (!active) {
          return;
        }
        setChurches(loadedChurches);
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

    void loadChurches();

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
    setSelectedChurchName(null);
    setDraftName('');
    setDraftLocation('');
  }

  function handleCreateMode() {
    resetForm();
    setFeedback(null);
  }

  function handleSelectChurch(church: Church) {
    setFormMode('edit');
    setSelectedChurchName(church.name);
    setDraftName(church.name);
    setDraftLocation(normalizeLocation(church.location));
    setFeedback(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = draftName.trim();
    const trimmedLocation = draftLocation.trim();

    if (!trimmedName) {
      setFeedback({
        severity: 'error',
        message: t('churches.validation.nameRequired'),
      });
      return;
    }

    const payload = {
      name: trimmedName,
      location: trimmedLocation,
      mainCelebrant: selectedChurch?.mainCelebrant ?? null,
    };

    setSubmitting(true);
    setFeedback(null);
    try {
      if (selectedChurch) {
        const updatedChurch = await updateChurch(selectedChurch.name, payload);
        setChurches((currentChurches) =>
          currentChurches.map((church) =>
            church.name === selectedChurch.name ? updatedChurch : church,
          ),
        );
        setSelectedChurchName(updatedChurch.name);
        setFormMode('edit');
        setDraftName(updatedChurch.name);
        setDraftLocation(normalizeLocation(updatedChurch.location));
        setFeedback({
          severity: 'success',
          message: t('churches.feedback.updated', { name: updatedChurch.name }),
        });
      } else {
        const createdChurch = await createChurch(payload);
        setChurches((currentChurches) => [...currentChurches, createdChurch]);
        resetForm();
        setFeedback({
          severity: 'success',
          message: t('churches.feedback.created', { name: createdChurch.name }),
        });
      }
    } catch {
      setFeedback({
        severity: 'error',
        message: t('churches.feedback.saveError'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteChurch() {
    if (!selectedChurch) {
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteChurch(selectedChurch.name);
      setChurches((currentChurches) =>
        currentChurches.filter((church) => church.name !== selectedChurch.name),
      );
      resetForm();
      setFeedback({
        severity: 'success',
        message: t('churches.feedback.deleted', { name: selectedChurch.name }),
      });
    } catch {
      setFeedback({
        severity: 'error',
        message: t('churches.feedback.deleteError'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('areas.churches.title')}
        subtitle={t('churches.subtitle')}
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
              {t(isEditing ? 'churches.form.editTitle' : 'churches.form.createTitle')}
            </Typography>

            <Stack spacing={2.5}>
              <TextField
                label={t('churches.form.nameLabel')}
                placeholder={t('churches.form.namePlaceholder')}
                value={draftName}
                onChange={(event) => setDraftName(event.target.value)}
                disabled={submitting || isEditing}
                helperText={isEditing ? t('churches.form.nameHelperEdit') : ' '}
                autoComplete="off"
                fullWidth
              />

              <TextField
                label={t('churches.form.locationLabel')}
                placeholder={t('churches.form.locationPlaceholder')}
                value={draftLocation}
                onChange={(event) => setDraftLocation(event.target.value)}
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
                  {t(isEditing ? 'churches.actions.save' : 'churches.actions.create')}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={submitting}
                  onClick={handleCreateMode}
                >
                  {t('churches.actions.reset')}
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
                  onClick={() => void handleDeleteChurch()}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {t('churches.actions.delete')}
                </Button>
              )}
            </Stack>
          </CardContent>
        </Card>

        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
                {t('churches.list.title')}
              </Typography>
              <TextField
                fullWidth
                label={t('churches.search.label')}
                placeholder={t('churches.search.placeholder')}
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
                        aria-label={t('churches.actions.clearSearch')}
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
                <Typography variant="body1">{t('churches.list.loading')}</Typography>
              </Box>
            )}

            {!loading && loadError && (
              <Box sx={{ p: 3 }}>
                <Alert severity="error">{t('churches.list.loadError')}</Alert>
              </Box>
            )}

            {!loading && !loadError && filteredChurches.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">
                  {churches.length === 0
                    ? t('churches.list.empty')
                    : t('churches.list.noMatches')}
                </Alert>
              </Box>
            )}

            {!loading && !loadError && filteredChurches.length > 0 && (
              <List disablePadding>
                {filteredChurches.map((church, index) => {
                  const isSelected = church.name === selectedChurchName;
                  const location = normalizeLocation(church.location);
                  return (
                    <Box component="li" key={church.name} sx={{ listStyle: 'none' }}>
                      {index > 0 && <Divider component="div" />}
                      <ListItemButton
                        selected={isSelected}
                        onClick={() => handleSelectChurch(church)}
                        sx={{ px: 3, py: 2.5, alignItems: 'flex-start' }}
                      >
                        <LocationCityOutlinedIcon
                          color={isSelected ? 'primary' : 'action'}
                          sx={{ mt: 0.5, mr: 2 }}
                        />
                        <ListItemText
                          primary={
                            <Typography variant="h6" fontWeight={700} sx={{ mb: 0.5 }}>
                              {church.name}
                            </Typography>
                          }
                          secondary={
                            <Typography variant="body2" color="text.secondary">
                              {location
                                ? t('churches.list.location', { location })
                                : t('churches.list.noLocation')}
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
                            {t('churches.actions.edit')}
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
                {t('churches.summary.total')}: {churches.length}
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
