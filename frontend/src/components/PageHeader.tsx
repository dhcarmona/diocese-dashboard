import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

interface PageHeaderProps {
  title: string;
  subtitle: string;
}

export default function PageHeader({ title, subtitle }: Readonly<PageHeaderProps>) {
  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h3" component="h1" fontWeight={700} sx={{ mb: 1 }}>
        {title}
      </Typography>
      <Typography variant="h6" color="text.secondary" sx={{ maxWidth: 720 }}>
        {subtitle}
      </Typography>
    </Box>
  );
}
