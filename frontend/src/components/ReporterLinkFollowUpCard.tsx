import ThumbUpAltOutlinedIcon from '@mui/icons-material/ThumbUpAltOutlined';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';

interface ReporterLinkFollowUpCardProps {
  nextReporterLinkToken: string | null;
  nextReporterLinkActiveDate: string | null;
}

export default function ReporterLinkFollowUpCard({
  nextReporterLinkToken,
  nextReporterLinkActiveDate,
}: Readonly<ReporterLinkFollowUpCardProps>) {
  const { t } = useTranslation();

  if (nextReporterLinkToken) {
    const formattedDate = nextReporterLinkActiveDate
      ? dayjs(nextReporterLinkActiveDate).format('DD/MM/YYYY')
      : null;

    return (
      <Stack alignItems="center" spacing={1.5}>
        <Typography variant="h6" fontWeight={700}>
          {t('submitService.nextPendingTitle')}
        </Typography>
        <Typography color="text.secondary">
          {formattedDate
            ? t('submitService.nextPendingMessage', { date: formattedDate })
            : t('submitService.nextPendingMessageNoDate')}
        </Typography>
        <Button
          variant="contained"
          component={RouterLink}
          to={`/r/${nextReporterLinkToken}`}
        >
          {t('submitService.openNextPendingLink')}
        </Button>
      </Stack>
    );
  }

  return (
    <Stack alignItems="center" spacing={1.5}>
      <ThumbUpAltOutlinedIcon color="primary" sx={{ fontSize: 64 }} />
      <Typography variant="h6" fontWeight={700}>
        {t('submitService.upToDateTitle')}
      </Typography>
      <Typography color="text.secondary">{t('submitService.upToDateMessage')}</Typography>
    </Stack>
  );
}
