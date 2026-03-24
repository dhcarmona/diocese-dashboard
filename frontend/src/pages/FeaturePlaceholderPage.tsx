import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';
import PageHeader from '../components/PageHeader';

interface FeaturePlaceholderPageProps {
  titleKey: string;
  descriptionKey: string;
}

export default function FeaturePlaceholderPage({
  titleKey,
  descriptionKey,
}: Readonly<FeaturePlaceholderPageProps>) {
  const { t } = useTranslation();

  return (
    <>
      <PageHeader title={t(titleKey)} subtitle={t(descriptionKey)} />
      <Card elevation={2} sx={{ maxWidth: 720, borderRadius: 4 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} sx={{ mb: 2 }}>
            {t('placeholder.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {t('placeholder.description')}
          </Typography>
          <Box sx={{ mt: 3 }}>
            <Button component={RouterLink} to="/" variant="contained" size="large">
              {t('placeholder.backHome')}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </>
  );
}
