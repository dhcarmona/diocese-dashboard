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
import Switch from '@mui/material/Switch';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createSectionHeader,
  deleteSectionHeader,
  updateSectionHeader,
} from '../api/sectionHeaders';
import {
  createServiceInfoItem,
  deleteServiceInfoItem,
  reorderTemplateItems,
  updateServiceInfoItem,
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

interface InfoTemplateItem extends ServiceInfoItemSummary {
  kind: 'INFO_ITEM';
}

interface SectionHeaderTemplateItem {
  kind: 'SECTION_HEADER';
  id: number;
  title: string;
  sortOrder?: number;
}

type TemplateItem = InfoTemplateItem | SectionHeaderTemplateItem;

function dndId(item: TemplateItem): string {
  return item.kind === 'INFO_ITEM' ? `i${item.id}` : `h${item.id}`;
}

function buildTemplateItems(
  infoItems: ServiceInfoItemSummary[],
  sectionHeaders: { id: number; title: string; sortOrder?: number }[],
): TemplateItem[] {
  const all: TemplateItem[] = [
    ...infoItems.map((item) => ({ ...item, kind: 'INFO_ITEM' as const })),
    ...sectionHeaders.map((h) => ({ ...h, kind: 'SECTION_HEADER' as const })),
  ];
  return all.sort((aa, bb) => (aa.sortOrder ?? 0) - (bb.sortOrder ?? 0));
}

interface SortableInfoItemRowProps {
  item: ServiceInfoItemSummary;
  index: number;
  submitting: boolean;
  isEditing: boolean;
  onEdit: (item: ServiceInfoItemSummary) => void;
  onRemove: (id: number) => void;
}

function SortableInfoItemRow({
  item,
  index,
  submitting,
  isEditing,
  onEdit,
  onRemove,
}: SortableInfoItemRowProps) {
  const { t } = useTranslation();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: `i${item.id}`,
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
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            size="small"
            disabled={submitting}
            color={isEditing ? 'primary' : 'inherit'}
            onClick={() => onEdit(item)}
          >
            {t('serviceTemplates.actions.editItem')}
          </Button>
          <Button
            variant="outlined"
            color="error"
            size="small"
            disabled={submitting}
            onClick={() => onRemove(item.id)}
          >
            {t('serviceTemplates.actions.removeItem')}
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}

interface SortableSectionHeaderRowProps {
  item: SectionHeaderTemplateItem;
  index: number;
  submitting: boolean;
  isEditing: boolean;
  onEdit: (item: SectionHeaderTemplateItem) => void;
  onRemove: (id: number) => void;
}

function SortableSectionHeaderRow({
  item,
  index,
  submitting,
  isEditing,
  onEdit,
  onRemove,
}: SortableSectionHeaderRowProps) {
  const { t } = useTranslation();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: `h${item.id}`,
    disabled: submitting,
  });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <Box ref={setNodeRef} style={style}>
      {index > 0 && <Divider />}
      <Box
        sx={{
          py: 1.5,
          display: 'flex',
          alignItems: 'center',
          gap: 1,
          bgcolor: 'action.hover',
          borderRadius: 1,
          px: 1,
        }}
      >
        <IconButton
          {...attributes}
          {...listeners}
          aria-label={t('serviceTemplates.actions.dragHandle')}
          size="small"
          disabled={submitting}
          sx={{
            cursor: submitting ? 'default' : 'grab',
            color: isDragging ? 'primary.main' : 'action.active',
            flexShrink: 0,
          }}
        >
          <DragIndicatorIcon fontSize="small" />
        </IconButton>
        <Typography
          variant="overline"
          fontWeight={700}
          color="text.secondary"
          sx={{ flex: 1, letterSpacing: 1.2 }}
        >
          {item.title}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            size="small"
            disabled={submitting}
            color={isEditing ? 'primary' : 'inherit'}
            onClick={() => onEdit(item)}
          >
            {t('serviceTemplates.actions.editItem')}
          </Button>
          <Button
            variant="outlined"
            color="error"
            size="small"
            disabled={submitting}
            onClick={() => onRemove(item.id)}
          >
            {t('serviceTemplates.actions.removeItem')}
          </Button>
        </Stack>
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
  const [draftLinkOnly, setDraftLinkOnly] = useState(false);
  const [templateItems, setTemplateItems] = useState<TemplateItem[]>([]);
  const [infoItemDraft, setInfoItemDraft] = useState<InfoItemDraft>(defaultInfoItemDraft);
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [headerDraft, setHeaderDraft] = useState('');
  const [editingHeaderId, setEditingHeaderId] = useState<number | null>(null);
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
    setDraftLinkOnly(false);
    setTemplateItems([]);
    setInfoItemDraft(defaultInfoItemDraft);
    setEditingItemId(null);
    setHeaderDraft('');
    setEditingHeaderId(null);
  }

  function handleCreateMode() {
    resetForm();
    setFeedback(null);
  }

  async function handleSelectTemplate(template: ServiceTemplate) {
    setFormMode('edit');
    setSelectedTemplateId(template.id);
    setFeedback(null);
    setEditingItemId(null);
    setInfoItemDraft(defaultInfoItemDraft);
    setHeaderDraft('');
    setEditingHeaderId(null);
    try {
      const full = await getServiceTemplateById(template.id);
      setSelectedTemplate(full);
      setDraftName(full.serviceTemplateName);
      setDraftLinkOnly(full.linkOnly);
      setTemplateItems(buildTemplateItems(full.serviceInfoItems ?? [], full.sectionHeaders ?? []));
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

    const draft: ServiceTemplateDraft = { serviceTemplateName: trimmedName, linkOnly: draftLinkOnly };

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

  function handleEditInfoItem(item: ServiceInfoItemSummary) {
    setEditingItemId(item.id);
    setEditingHeaderId(null);
    setHeaderDraft('');
    setInfoItemDraft({
      title: item.title,
      description: item.description ?? '',
      required: item.required,
      serviceInfoItemType: item.serviceInfoItemType,
    });
  }

  function handleCancelEditInfoItem() {
    setEditingItemId(null);
    setInfoItemDraft(defaultInfoItemDraft);
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
      if (editingItemId !== null) {
        const updated = await updateServiceInfoItem(editingItemId, selectedTemplate.id, itemDraft);
        setTemplateItems((prev) =>
          prev.map((it) =>
            it.kind === 'INFO_ITEM' && it.id === updated.id
              ? { ...updated, kind: 'INFO_ITEM' as const }
              : it,
          ),
        );
        setEditingItemId(null);
        setInfoItemDraft(defaultInfoItemDraft);
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.itemUpdated'),
        });
      } else {
        const created = await createServiceInfoItem(selectedTemplate.id, itemDraft);
        setTemplateItems((prev) => [...prev, { ...created, kind: 'INFO_ITEM' as const }]);
        setInfoItemDraft(defaultInfoItemDraft);
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.itemAdded'),
        });
      }
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
      setTemplateItems((prev) => prev.filter((it) => !(it.kind === 'INFO_ITEM' && it.id === itemId)));
      if (itemId === editingItemId) {
        setEditingItemId(null);
        setInfoItemDraft(defaultInfoItemDraft);
      }
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

  function handleEditSectionHeader(item: SectionHeaderTemplateItem) {
    setEditingHeaderId(item.id);
    setEditingItemId(null);
    setInfoItemDraft(defaultInfoItemDraft);
    setHeaderDraft(item.title);
  }

  function handleCancelEditSectionHeader() {
    setEditingHeaderId(null);
    setHeaderDraft('');
  }

  async function handleSaveSectionHeader() {
    if (!selectedTemplate) return;
    const trimmedTitle = headerDraft.trim();
    if (!trimmedTitle) {
      setFeedback({
        severity: 'error',
        message: t('serviceTemplates.validation.headerTitleRequired'),
      });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    try {
      if (editingHeaderId !== null) {
        const updated = await updateSectionHeader(editingHeaderId, { title: trimmedTitle });
        setTemplateItems((prev) =>
          prev.map((it) =>
            it.kind === 'SECTION_HEADER' && it.id === updated.id
              ? { ...it, title: updated.title }
              : it,
          ),
        );
        setEditingHeaderId(null);
        setHeaderDraft('');
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.headerUpdated'),
        });
      } else {
        const created = await createSectionHeader(selectedTemplate.id, { title: trimmedTitle });
        setTemplateItems((prev) => [...prev, { ...created, kind: 'SECTION_HEADER' as const }]);
        setHeaderDraft('');
        setFeedback({
          severity: 'success',
          message: t('serviceTemplates.feedback.headerAdded'),
        });
      }
    } catch {
      setFeedback({ severity: 'error', message: t('serviceTemplates.feedback.itemError') });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleRemoveSectionHeader(headerId: number) {
    setSubmitting(true);
    setFeedback(null);
    try {
      await deleteSectionHeader(headerId);
      setTemplateItems((prev) =>
        prev.filter((it) => !(it.kind === 'SECTION_HEADER' && it.id === headerId)),
      );
      if (headerId === editingHeaderId) {
        setEditingHeaderId(null);
        setHeaderDraft('');
      }
      setFeedback({
        severity: 'success',
        message: t('serviceTemplates.feedback.headerRemoved'),
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

    const oldIndex = templateItems.findIndex((item) => dndId(item) === active.id);
    const newIndex = templateItems.findIndex((item) => dndId(item) === over.id);
    const reordered = arrayMove(templateItems, oldIndex, newIndex);
    const previous = templateItems;
    setTemplateItems(reordered);

    try {
      await reorderTemplateItems(
        selectedTemplate!.id,
        reordered.map((item) => ({ id: item.id, kind: item.kind })),
      );
    } catch {
      setTemplateItems(previous);
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

              <Box>
                <FormControlLabel
                  control={
                    <Switch
                      checked={draftLinkOnly}
                      onChange={(e) => setDraftLinkOnly(e.target.checked)}
                      disabled={submitting}
                    />
                  }
                  label={t('serviceTemplates.form.linkOnlyLabel')}
                />
                <Typography variant="body2" color="text.secondary">
                  {t('serviceTemplates.form.linkOnlyDescription')}
                </Typography>
              </Box>

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
                {templateItems.length > 0 && (
                  <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    onDragEnd={(event) => void handleDragEnd(event)}
                  >
                    <SortableContext
                      items={templateItems.map(dndId)}
                      strategy={verticalListSortingStrategy}
                    >
                      <List disablePadding>
                        {templateItems.map((item, index) =>
                          item.kind === 'SECTION_HEADER' ? (
                            <SortableSectionHeaderRow
                              key={`h${item.id}`}
                              item={item}
                              index={index}
                              submitting={submitting}
                              isEditing={item.id === editingHeaderId}
                              onEdit={handleEditSectionHeader}
                              onRemove={(id) => void handleRemoveSectionHeader(id)}
                            />
                          ) : (
                            <SortableInfoItemRow
                              key={`i${item.id}`}
                              item={item}
                              index={index}
                              submitting={submitting}
                              isEditing={item.id === editingItemId}
                              onEdit={handleEditInfoItem}
                              onRemove={(id) => void handleRemoveInfoItem(id)}
                            />
                          ),
                        )}
                      </List>
                    </SortableContext>
                  </DndContext>
                )}

                <Divider />

                <Stack spacing={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    {t('serviceTemplates.form.addInfoItemTitle')}
                  </Typography>
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
                  <Stack direction="row" spacing={1.5}>
                    <Button
                      variant="outlined"
                      size="large"
                      disabled={submitting}
                      startIcon={editingItemId !== null ? <EditOutlinedIcon /> : <AddOutlinedIcon />}
                      onClick={() => void handleAddInfoItem()}
                      sx={{ alignSelf: 'flex-start' }}
                    >
                      {t(
                        editingItemId !== null
                          ? 'serviceTemplates.actions.saveItem'
                          : 'serviceTemplates.actions.addItem',
                      )}
                    </Button>
                    {editingItemId !== null && (
                      <Button
                        variant="text"
                        size="large"
                        disabled={submitting}
                        onClick={handleCancelEditInfoItem}
                        sx={{ alignSelf: 'flex-start' }}
                      >
                        {t('serviceTemplates.actions.cancelEditItem')}
                      </Button>
                    )}
                  </Stack>
                </Stack>

                <Divider />

                <Stack spacing={2}>
                  <Typography variant="subtitle2" color="text.secondary">
                    {t('serviceTemplates.form.addSectionHeaderTitle')}
                  </Typography>
                  <TextField
                    label={t('serviceTemplates.form.headerTitleLabel')}
                    placeholder={t('serviceTemplates.form.headerTitlePlaceholder')}
                    value={headerDraft}
                    onChange={(e) => setHeaderDraft(e.target.value)}
                    disabled={submitting}
                    fullWidth
                  />
                  <Stack direction="row" spacing={1.5}>
                    <Button
                      variant="outlined"
                      size="large"
                      disabled={submitting}
                      startIcon={
                        editingHeaderId !== null ? <EditOutlinedIcon /> : <AddOutlinedIcon />
                      }
                      onClick={() => void handleSaveSectionHeader()}
                      sx={{ alignSelf: 'flex-start' }}
                    >
                      {t(
                        editingHeaderId !== null
                          ? 'serviceTemplates.actions.saveHeader'
                          : 'serviceTemplates.actions.addHeader',
                      )}
                    </Button>
                    {editingHeaderId !== null && (
                      <Button
                        variant="text"
                        size="large"
                        disabled={submitting}
                        onClick={handleCancelEditSectionHeader}
                        sx={{ alignSelf: 'flex-start' }}
                      >
                        {t('serviceTemplates.actions.cancelEditItem')}
                      </Button>
                    )}
                  </Stack>
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
