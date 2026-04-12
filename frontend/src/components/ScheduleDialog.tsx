import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControl from '@mui/material/FormControl';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import FormHelperText from '@mui/material/FormHelperText';
import FormLabel from '@mui/material/FormLabel';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Stack from '@mui/material/Stack';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type Church } from '../api/churches';
import { ALL_DAYS, type DayOfWeek } from '../api/linkSchedules';

export interface ScheduleFormValues {
  daysOfWeek: DayOfWeek[];
  sendHour: number;
  /** Present when the dialog is used in edit mode (allChurches prop supplied). */
  churchNames?: string[];
}

interface ScheduleDialogProps {
  open: boolean;
  /** Initial values for editing an existing schedule. Leave undefined when creating. */
  initialValues?: ScheduleFormValues;
  /**
   * When provided, the dialog shows a church picker (edit mode).
   * Omit this prop when creating a new schedule — churches are taken from the main form.
   */
  allChurches?: Church[];
  onConfirm: (values: ScheduleFormValues) => void;
  onCancel: () => void;
}

/** Hours available for selection (every hour, 0–23). */
const HOURS = Array.from({ length: 24 }, (_, ii) => ii);

function formatHour(hour: number): string {
  if (hour === 0) return '12:00 AM';
  if (hour < 12) return `${hour}:00 AM`;
  if (hour === 12) return '12:00 PM';
  return `${hour - 12}:00 PM`;
}

/**
 * Dialog for configuring the days of the week and hour (Costa Rica time) for a link schedule.
 * Used both for creating a new schedule and editing an existing one.
 *
 * Pass a different `key` each time this dialog opens to reset its internal state.
 */
export default function ScheduleDialog({
  open,
  initialValues,
  allChurches,
  onConfirm,
  onCancel,
}: ScheduleDialogProps) {
  const { t } = useTranslation();

  const [selectedDays, setSelectedDays] = useState<DayOfWeek[]>(
    initialValues?.daysOfWeek ?? [],
  );
  const [sendHour, setSendHour] = useState<number>(initialValues?.sendHour ?? 8);
  const [selectedChurches, setSelectedChurches] = useState<string[]>(
    initialValues?.churchNames ?? [],
  );
  const [daysError, setDaysError] = useState(false);
  const [churchesError, setChurchesError] = useState(false);

  function toggleDay(day: DayOfWeek) {
    setSelectedDays((prev) =>
      prev.includes(day) ? prev.filter((dd) => dd !== day) : [...prev, day],
    );
    setDaysError(false);
  }

  function toggleChurch(name: string) {
    setSelectedChurches((prev) =>
      prev.includes(name) ? prev.filter((nn) => nn !== name) : [...prev, name],
    );
    setChurchesError(false);
  }

  function handleConfirm() {
    let valid = true;
    if (selectedDays.length === 0) {
      setDaysError(true);
      valid = false;
    }
    if (allChurches !== undefined && selectedChurches.length === 0) {
      setChurchesError(true);
      valid = false;
    }
    if (!valid) return;
    onConfirm({
      daysOfWeek: selectedDays,
      sendHour,
      ...(allChurches !== undefined ? { churchNames: selectedChurches } : {}),
    });
  }

  const isEditing = initialValues !== undefined;

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>
        {isEditing ? t('scheduleDialog.titleEdit') : t('scheduleDialog.titleCreate')}
      </DialogTitle>

      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          {/* Day of week selection */}
          <FormControl error={daysError} component="fieldset">
            <FormLabel component="legend">{t('scheduleDialog.daysLabel')}</FormLabel>
            <FormGroup>
              {ALL_DAYS.map((day) => (
                <FormControlLabel
                  key={day}
                  control={
                    <Checkbox
                      checked={selectedDays.includes(day)}
                      onChange={() => toggleDay(day)}
                      size="small"
                    />
                  }
                  label={t(`scheduleDialog.days.${day}`)}
                />
              ))}
            </FormGroup>
            {daysError && (
              <FormHelperText>{t('scheduleDialog.daysRequired')}</FormHelperText>
            )}
          </FormControl>

          {/* Hour selection */}
          <FormControl fullWidth>
            <InputLabel id="schedule-hour-label">{t('scheduleDialog.hourLabel')}</InputLabel>
            <Select
              labelId="schedule-hour-label"
              label={t('scheduleDialog.hourLabel')}
              value={sendHour}
              onChange={(ee) => setSendHour(ee.target.value as number)}
            >
              {HOURS.map((hh) => (
                <MenuItem key={hh} value={hh}>
                  {formatHour(hh)}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          {/* Church selection — shown in edit mode only */}
          {allChurches !== undefined && (
            <FormControl error={churchesError} component="fieldset">
              <FormLabel component="legend">{t('scheduleDialog.churchesLabel')}</FormLabel>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mt: 1 }}>
                {allChurches.map((church) => {
                  const isSelected = selectedChurches.includes(church.name);
                  return (
                    <Chip
                      key={church.name}
                      label={church.name}
                      onClick={() => toggleChurch(church.name)}
                      color={isSelected ? 'primary' : 'default'}
                      variant={isSelected ? 'filled' : 'outlined'}
                      clickable
                    />
                  );
                })}
              </Box>
              {churchesError && (
                <FormHelperText>{t('scheduleDialog.churchesRequired')}</FormHelperText>
              )}
            </FormControl>
          )}
        </Stack>
      </DialogContent>

      <DialogActions>
        <Button onClick={onCancel}>{t('scheduleDialog.cancel')}</Button>
        <Button variant="contained" onClick={handleConfirm}>
          {t('scheduleDialog.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

