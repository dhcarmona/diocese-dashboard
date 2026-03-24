import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardActionArea from '@mui/material/CardActionArea';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import { Link as RouterLink } from 'react-router-dom';

interface ActionTileProps {
  title: string;
  description: string;
  to: string;
  imageUrl?: string;
}

export default function ActionTile({
  title,
  description,
  to,
  imageUrl,
}: Readonly<ActionTileProps>) {
  return (
    <Card elevation={3} sx={{ height: '100%', borderRadius: 4, overflow: 'hidden' }}>
      <CardActionArea
        component={RouterLink}
        to={to}
        sx={{
          position: 'relative',
          height: '100%',
          minHeight: 224,
          display: 'flex',
          alignItems: 'stretch',
          textAlign: 'left',
        }}
      >
        <Box
          aria-hidden="true"
          sx={{
            position: 'absolute',
            inset: 0,
            backgroundImage: imageUrl
              ? `linear-gradient(180deg, rgba(255,255,255,0.82), rgba(255,255,255,0.94)), url(${imageUrl})`
              : 'linear-gradient(135deg, rgba(25,118,210,0.08), rgba(25,118,210,0.18))',
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }}
        />
        <CardContent
          sx={{
            position: 'relative',
            zIndex: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: 2,
            width: '100%',
            p: 3,
          }}
        >
          <Box>
            <Typography variant="h5" component="h2" fontWeight={700} sx={{ mb: 1.5 }}>
              {title}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ fontSize: '1rem' }}>
              {description}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'primary.main' }}>
            <ArrowForwardOutlinedIcon />
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}
