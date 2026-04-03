import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import Alert from '@mui/material/Alert';
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
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
import { MuiTelInput } from 'mui-tel-input';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getChurches } from '../api/churches';
import {
  createReporterUser,
  deleteReporterUser,
  getReporterUsers,
  updateReporterUser,
  type ReporterUser,
} from '../api/users';
import PageHeader from '../components/PageHeader';

type FeedbackSeverity = 'success' | 'error';

interface FeedbackState {
  severity: FeedbackSeverity;
  message: string;
}

type FormMode = 'create' | 'edit';

function sortUsers(users: ReporterUser[]): ReporterUser[] {
  return [...users].sort((left, right) =>
    left.fullName.localeCompare(right.fullName, undefined, { sensitivity: 'base' }),
  );
}

export default function ReporterUserManagementPage() {
  const { t } = useTranslation();
  const [users, setUsers] = useState<ReporterUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [formMode, setFormMode] = useState<FormMode>('create');
  const [draftUsername, setDraftUsername] = useState('');
  const [draftFullName, setDraftFullName] = useState('');
  const [draftPhone, setDraftPhone] = useState('+506');
  const [draftChurches, setDraftChurches] = useState<string[]>([]);
  const [churchOptions, setChurchOptions] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const sortedUsers = useMemo(() => sortUsers(users), [users]);
  const normalizedSearchTerm = searchTerm.trim().toLocaleLowerCase();
  const filteredUsers = useMemo(
    () =>
      sortedUsers.filter(
        (user) =>
          user.fullName.toLocaleLowerCase().includes(normalizedSearchTerm) ||
          user.username.toLocaleLowerCase().includes(normalizedSearchTerm),
      ),
    [normalizedSearchTerm, sortedUsers],
  );

  const selectedUser = useMemo(
    () => users.find((user) => user.id === selectedUserId) ?? null,
    [users, selectedUserId],
  );

  const isEditing = formMode === 'edit' && selectedUser !== null;

  useEffect(() => {
    let active = true;

    async function loadUsers() {
      setLoading(true);
      setLoadError(false);
      try {
        const loaded = await getReporterUsers();
        if (!active) {
          return;
        }
        setUsers(loaded);
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

    void loadUsers();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function loadChurches() {
      try {
        const churches = await getChurches();
        if (active) {
          setChurchOptions(churches.map((c) => c.name).sort());
        }
      } catch {
        // Non-critical — form still works; user just won't see suggestions
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
    setSelectedUserId(null);
    setDraftUsername('');
    setDraftFullName('');
    setDraftPhone('+506');
    setDraftChurches([]);
  }

  function handleCreateMode() {
    resetForm();
    setFeedback(null);
  }

  function handleSelectUser(user: ReporterUser) {
    setFormMode('edit');
    setSelectedUserId(user.id);
    setDraftUsername(user.username);
    setDraftFullName(user.fullName);
    setDraftPhone(user.phoneNumber);
    setDraftChurches(user.assignedChurches.map((c) => c.name));
    setFeedback(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedUsername = draftUsername.trim();
    const trimmedFullName = draftFullName.trim();
    const trimmedPhone = draftPhone.replace(/\s/g, '');
    const phoneHasLocalNumber = trimmedPhone.replace(/^\+\d{1,4}/, '').length > 0;
    const churchNames = draftChurches;

    if (!trimmedUsername) {
      setFeedback({ severity: 'error', message: t('users.validation.usernameRequired') });
      return;
    }
    if (!trimmedFullName) {
      setFeedback({ severity: 'error', message: t('users.validation.fullNameRequired') });
      return;
    }
    if (!phoneHasLocalNumber) {
      setFeedback({ severity: 'error', message: t('users.validation.phoneRequired') });
      return;
    }
    if (churchNames.length === 0) {
      setFeedback({ severity: 'error', message: t('users.validation.churchesRequired') });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      if (selectedUser) {
        const updated = await updateReporterUser(selectedUser.id, {
          username: trimmedUsername,
          fullName: trimmedFullName,
          phoneNumber: trimmedPhone,
          churchNames,
        });
        setUsers((current) => current.map((u) => (u.id === updated.id ? updated : u)));
        setSelectedUserId(updated.id);
        setFormMode('edit');
        setDraftUsername(updated.username);
        setDraftFullName(updated.fullName);
        setDraftPhone(updated.phoneNumber);
        setDraftChurches(updated.assignedChurches.map((c) => c.name));
        setFeedback({
          severity: 'success',
          message: t('users.feedback.updated', { name: updated.fullName }),
        });
      } else {
        const created = await createReporterUser({
          username: trimmedUsername,
          fullName: trimmedFullName,
          phoneNumber: trimmedPhone,
          churchNames,
        });
        setUsers((current) => [...current, created]);
        resetForm();
        setFeedback({
          severity: 'success',
          message: t('users.feedback.created', { name: created.fullName }),
        });
      }
    } catch {
      setFeedback({ severity: 'error', message: t('users.feedback.saveError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteUser() {
    if (!selectedUser) {
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteReporterUser(selectedUser.id);
      setUsers((current) => current.filter((u) => u.id !== selectedUser.id));
      resetForm();
      setFeedback({
        severity: 'success',
        message: t('users.feedback.deleted', { name: selectedUser.fullName }),
      });
    } catch {
      setFeedback({ severity: 'error', message: t('users.feedback.deleteError') });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t('areas.users.title')}
        subtitle={t('users.subtitle')}
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

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Card
          elevation={2}
          sx={{ borderRadius: 4 }}
          component="form"
          onSubmit={(event: FormEvent<HTMLFormElement>) => void handleSubmit(event)}
        >
          <CardContent sx={{ p: 3 }}>
            <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
              {t(isEditing ? 'users.form.editTitle' : 'users.form.createTitle')}
            </Typography>

            <Stack spacing={2.5}>
              <TextField
                label={t('users.form.usernameLabel')}
                placeholder={t('users.form.usernamePlaceholder')}
                value={draftUsername}
                onChange={(event) => setDraftUsername(event.target.value)}
                disabled={submitting || isEditing}
                helperText={isEditing ? t('users.form.usernameHelperEdit') : undefined}
                autoComplete="off"
                fullWidth
              />

              <TextField
                label={t('users.form.fullNameLabel')}
                placeholder={t('users.form.fullNamePlaceholder')}
                value={draftFullName}
                onChange={(event) => setDraftFullName(event.target.value)}
                disabled={submitting}
                autoComplete="off"
                fullWidth
              />

              <MuiTelInput
                label={t('users.form.phoneNumberLabel')}
                value={draftPhone}
                onChange={(value) => setDraftPhone(value)}
                defaultCountry="CR"
                disabled={submitting}
                fullWidth
              />

              <Autocomplete
                multiple
                options={churchOptions}
                value={draftChurches}
                onChange={(_event, newValue) => setDraftChurches(newValue)}
                disabled={submitting}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => {
                    const { key, ...tagProps } = getTagProps({ index });
                    return <Chip key={key} label={option} size="small" {...tagProps} />;
                  })
                }
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={t('users.form.churchNamesLabel')}
                    placeholder={
                      draftChurches.length === 0
                        ? t('users.form.churchNamesPlaceholder')
                        : undefined
                    }
                  />
                )}
              />

              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={submitting}
                  startIcon={isEditing ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
                >
                  {t(isEditing ? 'users.actions.save' : 'users.actions.create')}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={submitting}
                  onClick={handleCreateMode}
                >
                  {t('users.actions.reset')}
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
                  onClick={() => void handleDeleteUser()}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {t('users.actions.delete')}
                </Button>
              )}
            </Stack>
          </CardContent>
        </Card>

        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
                {t('users.list.title')}
              </Typography>
              <TextField
                fullWidth
                label={t('users.search.label')}
                placeholder={t('users.search.placeholder')}
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
                        aria-label={t('users.actions.clearSearch')}
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
                <Typography variant="body1">{t('users.list.loading')}</Typography>
              </Box>
            )}

            {!loading && loadError && (
              <Box sx={{ p: 3 }}>
                <Alert severity="error">{t('users.list.loadError')}</Alert>
              </Box>
            )}

            {!loading && !loadError && filteredUsers.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">
                  {users.length === 0
                    ? t('users.list.empty')
                    : t('users.list.noMatches')}
                </Alert>
              </Box>
            )}

            {!loading && !loadError && filteredUsers.length > 0 && (
              <List disablePadding>
                {filteredUsers.map((user, index) => {
                  const isSelected = user.id === selectedUserId;
                  return (
                    <Box component="li" key={user.id} sx={{ listStyle: 'none' }}>
                      {index > 0 && <Divider component="div" />}
                      <ListItemButton
                        selected={isSelected}
                        onClick={() => handleSelectUser(user)}
                        sx={{ px: 3, py: 2.5, alignItems: 'center' }}
                      >
                        <PersonOutlineIcon
                          sx={{
                            mr: 2,
                            color: isSelected ? 'primary.main' : 'action.active',
                          }}
                        />
                        <ListItemText
                          primary={
                            <Typography variant="h6" fontWeight={700} sx={{ mb: 0.5 }}>
                              {user.fullName}
                            </Typography>
                          }
                          secondary={
                            <Stack component="span" spacing={0.25}>
                              <Typography component="span" variant="body2" color="text.secondary">
                                @{user.username}
                              </Typography>
                              <Typography component="span" variant="body2" color="text.secondary">
                                {t('users.list.phone', { phone: user.phoneNumber })}
                              </Typography>
                              {user.assignedChurches.length > 0 && (
                                <Typography
                                  component="span"
                                  variant="body2"
                                  color="text.secondary"
                                >
                                  {t('users.list.churches', {
                                    churches: user.assignedChurches
                                      .map((c) => c.name)
                                      .join(', '),
                                  })}
                                </Typography>
                              )}
                            </Stack>
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
                            {t('users.actions.edit')}
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
                {t('users.summary.total')}: {users.length}
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
