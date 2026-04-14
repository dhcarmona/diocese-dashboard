import dayjs, { type ConfigType } from 'dayjs';
import updateLocale from 'dayjs/plugin/updateLocale';
import utc from 'dayjs/plugin/utc';
import 'dayjs/locale/en';
import 'dayjs/locale/es';

dayjs.extend(updateLocale);
dayjs.extend(utc);

dayjs.updateLocale('es', {
  monthsShort: [
    'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
    'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic',
  ],
});

export const APP_DATE_FORMAT = 'DD MMM YYYY';

export function getAppDateLocale(language?: string | null): 'en' | 'es' {
  return language?.toLowerCase().startsWith('en') ? 'en' : 'es';
}

function getLocalizedDate(value: ConfigType, language?: string | null) {
  return dayjs(value).locale(getAppDateLocale(language));
}

export function formatDate(value: ConfigType, language?: string | null): string {
  return getLocalizedDate(value, language).format(APP_DATE_FORMAT);
}

export function formatDateTime(value: ConfigType, language?: string | null): string {
  return getLocalizedDate(value, language).format(`${APP_DATE_FORMAT}, HH:mm`);
}

export function formatDateTimeAtFixedOffset(
  value: ConfigType,
  offsetHours: number,
  language?: string | null,
): string {
  return dayjs
    .utc(value)
    .add(offsetHours, 'hour')
    .locale(getAppDateLocale(language))
    .format(`${APP_DATE_FORMAT}, HH:mm`);
}
