import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import Box from '@mui/material/Box';
import type { SxProps, Theme } from '@mui/material/styles';
import type { ServiceTemplateSummary } from '../api/serviceTemplates';

interface ServiceTemplateBannerProps {
  template: ServiceTemplateSummary;
  sx?: SxProps<Theme>;
  testId?: string;
}

export default function ServiceTemplateBanner({
  template,
  sx,
  testId,
}: Readonly<ServiceTemplateBannerProps>) {
  if (template.bannerUrl) {
    return (
      <Box
        component="img"
        aria-hidden="true"
        data-testid={testId}
        src={template.bannerUrl}
        alt=""
        sx={[
          {
            display: 'block',
            width: '100%',
            aspectRatio: '4 / 1',
            objectFit: 'cover',
            borderRadius: 3,
            bgcolor: 'grey.100',
          },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
        ]}
      />
    );
  }

  return (
    <Box
      aria-hidden="true"
      data-testid={testId}
      sx={[
        {
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '100%',
          aspectRatio: '4 / 1',
          borderRadius: 3,
          bgcolor: 'grey.100',
          color: 'primary.main',
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      <ImageOutlinedIcon sx={{ fontSize: 48, opacity: 0.4 }} />
    </Box>
  );
}

