import {
  DndContext,
  type DragEndEvent,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import FormControl from '@mui/material/FormControl';
import FormControlLabel from '@mui/material/FormControlLabel';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import InputLabel from '@mui/material/InputLabel';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createServiceInfoItem,
  deleteServiceInfoItem,
  reorderServiceInfoItems,
  type ServiceInfoItemDraft,
} from '../api/serviceInfoItems';
import {
  createServiceTemplate,
  deleteServiceTemplate,
  getServiceTemplateById,
  getServiceTemplates,
  updateServiceTemplate,
  type ServiceInfoItemSummary,
  type ServiceInfoItemType,
  type ServiceTemplate,
  type ServiceTemplateDraft,
} from '../api/serviceTemplates';
import PageHeader from '../components/PageHeader';
import ServiceTemplateBanner from '../components/ServiceTemplateBanner';

type FeedbackSeverity = 'success' | 'error';

interface FeedbackState {
  severity: FeedbackSeverity;
  message: string;
}

type FormMode = 'create' | 'edit';

interface InfoItemDraft {
  title: string;
  description: string;
  required: boolean;
  serviceInfoItemType: ServiceInfoItemType;
}

const INFO_ITEM_TYPES: ServiceInfoItemType[] = ['NUMERICAL', 'DOLLARS', 'COLONES', 'STRING'];

const defaultInfoItemDraft: InfoItemDraft = {
  title: '',
  description: '',
  required: false,
  serviceInfoItemType: 'NUMERICAL',
};

interface SortableInfoItemRowProps {
  item: ServiceInfoItemSummary;
  index: number;
  submitting: boolean;
  onRemove: (id: number) => void;
}

function SortableInfoItemRow({ item, index, submitting, onRemove }: SortableInfoItemRowProps) {
  const { t } = useTranslation();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: item.id,
    disabled: submitting,
  });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <Box ref={setNodeRef} style={style}>
      {index > 0 && <Divider />}
      <Box sx={{ py: 1.5, display: 'flex', alignItems: 'flex-start', gap: 1 }}>
        <IconButton
          {...attributes}
          {...listeners}
          aria-label={t('serviceTemplates.actions.dragHandle')}
          size="small"
          disabled={submitting}
          sx={{
            cursor: submitting ? 'default' : 'grab',
            color: isDragging ? 'primary.main' : 'action.active',
            mt: 0.25,
            flexShrink: 0,
          }}
        >
          <DragIndicatorIcon fontSize="small" />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Typography variant="body1" fontWeight={600}>
            {item.title}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t(`serviceInfoItemType.${item.serviceInfoItemType}`)}
            {item.required ? ` · ${t('serviceTemplates.form.itemRequiredLabel')}` : ''}
          </Typography>
          {item.description && (
            <Typography variant="body2" color="text.secondary">
              {item.description}
            </Typography>
          )}
        </Box>
        <Button
          variant="outlined"
          color="error"
          size="small"
          disabled={submitting}
          onClick={() => onRemove(item.id)}
        >
          {t('serviceTemplates.actions.removeItem')}
        </Button>
      </Box>
    </Box>
  );
}

function sortTemplates(templates: ServiceTemplate[]): ServiceTemplate[] {
  return [...templates].sort((left, right) =>
    left.serviceTemplateName.localeCompare(right.serviceTemplateName, undefined, {
      sensitivity: 'base',
    }),
  );
}

export default function ServiceTemplateManagementPage() {
  const { t } = useTranslation();
  const [templates, setTemplates] = useState<ServiceTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [selectedTemplate, setSelectedTemplate] = useState<ServiceTemplate | null>(null);
  const [formMode, setFormMode] = useState<FormMode>('create');
  const [draftName, setDraftName] = useState('');
  const [infoItems, setInfoItems] = useState<ServiceInfoItemSummary[]>([]);
  const [infoItemDraft, setInfoItemDraft] = useState<InfoItemDraft>(defaultInfoItemDraft);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const sortedTemplates = useMemo(() => sortTemplates(templates), [templates]);
  const normalizedSearch = searchTerm.trim().toLocaleLowerCase();
  const filteredTemplates = useMemo(    () =>
      sortedTemplates.filter((tmpl) =>
        tmpl.serviceTemplateName.toLocaleLowerCase().includes(normalizedSearch),
      ),
    [normalizedSearch, sortedTemplates],
  );

  const isEditing = formMode === 'edit' && selectedTemplate !== null;

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setLoadError(false);
      try {
        const loaded = await getServiceTemplates();
        if (!active) return;
        setTemplates(loaded);
      } catch {
        if (active) setLoadError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!feedback) return undefined;
    const id = window.setTimeout(() => setFeedback(null), 5000);
    return () => window.clearTimeout(id);
  }, [feedback]);

  function resetForm() {
    setFormMode('create');
    setSelectedTemplateId(null);
    setSelectedTemplate(null);
    setDraftName('');
    setInfoItems([]);
    setInfoItemDraft(defaultInfoItemDraft);
  }

  function handleCreateMode() {
    resetForm();
    setFeedback(null);
  }

  async function handleSelectTemplate(template: ServiceTemplate) {
    setFormMode('edit');
    setSelectedTemplateId(template.id);
    setFeedback(null);
    try {
      const full = await getServiceTemplateById(template.id);
      setSelectedTemplate(full);
      setDraftName(full.serviceTemplateName);
      setInfoItems(full.serviceInfoItems ?? []);
    } catch {
      setFeedback({ severity: 'error', message: t('serviceTemplates.list.loadError') });
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = draftName.trim();

    if (!trimmedName) {
      setFeedback({
        severity: 'error',
        message: t('serviceTemplates.validation.nameRequired'),
      });
      return;
    }

    setSubmitting(true);
    setFeedback(null);

    const draft: ServiceTemplateDraft = { serviceTemplateName: trimmedName };

    try {
      if (selectedTemplate) {
        const updated = await updateServiceTemplate(selectedTemplate.id, draft);
        setTemplates((prev) => prev.map((tpl) => (tpl.id === updated.id ? updated : tpl)));
        setSelectedTemplate(updated);
        setDraftName(updated.serviceTemplateName);
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.updated', {
            name: updated.serviceTemplateName,
          }),
        });
      } else {
        const created = await createServiceTemplate(draft);
        setTemplates((prev) => [...prev, created]);
        resetForm();
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.created', {
            name: created.serviceTemplateName,
          }),
        });
      }
    } catch {
      setFeedback({ severity: 'error', message: t('serviceTemplates.feedback.saveError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteTemplate() {
    if (!selectedTemplate) return;
    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteServiceTemplate(selectedTemplate.id);
      const deletedName = selectedTemplate.serviceTemplateName;
      setTemplates((prev) => prev.filter((tpl) => tpl.id !== selectedTemplate.id));
      resetForm();
      setFeedback({
        severity: 'success',
        message: t('serviceTemplates.feedback.deleted', { name: deletedName }),
      });
    } catch {
      setFeedback({
        severity: 'error',
        message: t('serviceTemplates.feedback.deleteError'),
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAddInfoItem() {
    if (!selectedTemplate) return;
    const trimmedTitle = infoItemDraft.title.trim();
    if (!trimmedTitle) {
      setFeedback({
        severity: 'error',
        message: t('serviceTemplates.validation.itemTitleRequired'),
      });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    const itemDraft: ServiceInfoItemDraft = {
      title: trimmedTitle,
      description: infoItemDraft.description.trim() || null,
      required: infoItemDraft.required,
      serviceInfoItemType: infoItemDraft.serviceInfoItemType,
    };
    try {
      const created = await createServiceInfoItem(selectedTemplate.id, itemDraft);
      setInfoItems((prev) => [...prev, created]);
      setInfoItemDraft(defaultInfoItemDraft);
      setFeedback({
        severity: 'success',
        message: t('serviceTemplates.feedback.itemAdded'),
      });
    } catch {
      setFeedback({ severity: 'error', message: t('serviceTemplates.feedback.itemError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRemoveInfoItem(itemId: number) {
    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteServiceInfoItem(itemId);
      setInfoItems((prev) => prev.filter((item) => item.id !== itemId));
      setFeedback({
        severity: 'success',
        message: t('serviceTemplates.feedback.itemRemoved'),
      });
    } catch {
      setFeedback({ severity: 'error', message: t('serviceTemplates.feedback.itemError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const oldIndex = infoItems.findIndex((item) => item.id === active.id);
    const newIndex = infoItems.findIndex((item) => item.id === over.id);
    const reordered = arrayMove(infoItems, oldIndex, newIndex);
    const previous = infoItems;
    setInfoItems(reordered);

    try {
      await reorderServiceInfoItems(reordered.map((item) => item.id));
    } catch {
      setInfoItems(previous);
      setFeedback({
        severity: 'error',
        message: t('serviceTemplates.feedback.reorderError'),
      });
    }
  }

  return (
    <>
      <PageHeader
        title={t('areas.serviceTemplates.title')}
        subtitle={t('serviceTemplates.subtitle')}
      />

      {feedback && (
        <Alert severity={feedback.severity} sx={{ mb: 3 }}>
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
            <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 2 }}>
              {t(
                isEditing
                  ? 'serviceTemplates.form.editTitle'
                  : 'serviceTemplates.form.createTitle',
              )}
            </Typography>

            {selectedTemplate && (
              <Box sx={{ mb: 2, mx: -3, mt: 1.5, overflow: 'hidden' }}>
                <ServiceTemplateBanner
                  template={selectedTemplate}
                  sx={{ borderRadius: 0 }}
                  testId="template-form-banner"
                />
              </Box>
            )}

            <Stack spacing={2.5}>
              <TextField
                label={t('serviceTemplates.form.nameLabel')}
                placeholder={t('serviceTemplates.form.namePlaceholder')}
                value={draftName}
                onChange={(e) => setDraftName(e.target.value)}
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
                  {t(
                    isEditing
                      ? 'serviceTemplates.actions.save'
                      : 'serviceTemplates.actions.create',
                  )}
                </Button>
                <Button
                  type="button"
                  variant="outlined"
                  size="large"
                  disabled={submitting}
                  onClick={handleCreateMode}
                >
                  {t('serviceTemplates.actions.reset')}
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
                  onClick={() => void handleDeleteTemplate()}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {t('serviceTemplates.actions.delete')}
                </Button>
              )}
            </Stack>
          </CardContent>
        </Card>

        {isEditing && (
          <Card elevation={2} sx={{ borderRadius: 4 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 2 }}>
                {t('serviceTemplates.form.infoItemsTitle')}
              </Typography>

              <Stack spacing={2}>
                {infoItems.length > 0 && (
                  <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragEnd={(event) => void handleDragEnd(event)}
                  >
                    <SortableContext
                      items={infoItems.map((item) => item.id)}
                      strategy={verticalListSortingStrategy}
                    >
                      <List disablePadding>
                        {infoItems.map((item, index) => (
                          <SortableInfoItemRow
                            key={item.id}
                            item={item}
                            index={index}
                            submitting={submitting}
                            onRemove={(id) => void handleRemoveInfoItem(id)}
                          />
                        ))}
                      </List>
                    </SortableContext>
                  </DndContext>
                )}

                <Divider />

                <Stack spacing={2}>
                  <TextField
                    label={t('serviceTemplates.form.itemTitleLabel')}
                    placeholder={t('serviceTemplates.form.itemTitlePlaceholder')}
                    value={infoItemDraft.title}
                    onChange={(e) =>
                      setInfoItemDraft((prev) => ({ ...prev, title: e.target.value }))
                    }
                    disabled={submitting}
                    fullWidth
                  />
                  <TextField
                    label={t('serviceTemplates.form.itemDescLabel')}
                    placeholder={t('serviceTemplates.form.itemDescPlaceholder')}
                    value={infoItemDraft.description}
                    onChange={(e) =>
                      setInfoItemDraft((prev) => ({ ...prev, description: e.target.value }))
                    }
                    disabled={submitting}
                    fullWidth
                  />
                  <FormControl fullWidth disabled={submitting}>
                    <InputLabel>{t('serviceTemplates.form.itemTypeLabel')}</InputLabel>
                    <Select
                      value={infoItemDraft.serviceInfoItemType}
                      label={t('serviceTemplates.form.itemTypeLabel')}
                      onChange={(e) =>
                        setInfoItemDraft((prev) => ({
                          ...prev,
                          serviceInfoItemType: e.target.value as ServiceInfoItemType,
                        }))
                      }
                    >
                      {INFO_ITEM_TYPES.map((type) => (
                        <MenuItem key={type} value={type}>
                          {t(`serviceInfoItemType.${type}`)}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={infoItemDraft.required}
                        onChange={(e) =>
                          setInfoItemDraft((prev) => ({ ...prev, required: e.target.checked }))
                        }
                        disabled={submitting}
                      />
                    }
                    label={t('serviceTemplates.form.itemRequiredLabel')}
                  />
                  <Button
                    variant="outlined"
                    size="large"
                    disabled={submitting}
                    startIcon={<AddOutlinedIcon />}
                    onClick={() => void handleAddInfoItem()}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    {t('serviceTemplates.actions.addItem')}
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        )}

        <Card elevation={2} sx={{ borderRadius: 4 }}>
          <CardContent sx={{ p: 0 }}>
            <Box sx={{ p: 3 }}>
              <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 0.75 }}>
                {t('serviceTemplates.list.title')}
              </Typography>
              <TextField
                fullWidth
                label={t('serviceTemplates.search.label')}
                placeholder={t('serviceTemplates.search.placeholder')}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlinedIcon />
                    </InputAdornment>
                  ),
                  endAdornment: searchTerm ? (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label={t('serviceTemplates.actions.clearSearch')}
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
                <Typography variant="body1">{t('serviceTemplates.list.loading')}</Typography>
              </Box>
            )}

            {!loading && loadError && (
              <Box sx={{ p: 3 }}>
                <Alert severity="error">{t('serviceTemplates.list.loadError')}</Alert>
              </Box>
            )}

            {!loading && !loadError && filteredTemplates.length === 0 && (
              <Box sx={{ p: 3 }}>
                <Alert severity="info">
                  {templates.length === 0
                    ? t('serviceTemplates.list.empty')
                    : t('serviceTemplates.list.noMatches')}
                </Alert>
              </Box>
            )}

            {!loading && !loadError && filteredTemplates.length > 0 && (
              <List disablePadding>
                {filteredTemplates.map((template, index) => {
                  const isSelected = template.id === selectedTemplateId;
                  return (
                    <Box component="li" key={template.id} sx={{ listStyle: 'none' }}>
                      {index > 0 && <Divider component="div" />}
                      <ListItemButton
                        selected={isSelected}
                        onClick={() => void handleSelectTemplate(template)}
                        sx={{ px: 3, py: 2.5, alignItems: 'center' }}
                      >
                        <ImageOutlinedIcon
                          sx={{
                            mr: 2,
                            flexShrink: 0,
                            color: isSelected ? 'primary.main' : 'action.active',
                          }}
                        />
                        <ListItemText
                          primary={
                            <Typography variant="h6" fontWeight={700} sx={{ mb: 0.5 }}>
                              {template.serviceTemplateName}
                            </Typography>
                          }
                          secondary={
                            <Typography variant="body2" color="text.secondary">
                              {t('serviceTemplates.list.itemsCount', {
                                count: template.serviceInfoItems?.length ?? 0,
                              })}
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
                            {t('serviceTemplates.actions.edit')}
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
                {t('serviceTemplates.summary.total')}: {templates.length}
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
