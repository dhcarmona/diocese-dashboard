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
  to,
  imageUrl,
}: Readonly<ActionTileProps>) {
  return (
    <Card
      elevation={2}
      sx={{
        height: '100%',
        borderRadius: 3,
        overflow: 'hidden',
        transition: 'box-shadow 0.2s, transform 0.2s',
        '&:hover': {
          boxShadow: 8,
          transform: 'translateY(-2px)',
        },
      }}
    >
      <CardActionArea
        component={RouterLink}
        to={to}
        sx={{
          position: 'relative',
          height: '100%',
          minHeight: 180,
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
            backgroundColor: imageUrl ? '#E8EEF8' : 'transparent',
            backgroundImage: imageUrl
              ? `linear-gradient(to right, #ffffff 40%, rgba(255,255,255,0) 68%), url("${imageUrl}")`
              : 'linear-gradient(135deg, rgba(28,58,110,0.06), rgba(28,58,110,0.14))',
            // Two comma-separated values — one per background layer.
            // Gradient covers the full tile; SVG is 75% height, anchored bottom-right.
            backgroundSize: imageUrl ? '100% 100%, auto 75%' : '100% 100%',
            backgroundPosition: imageUrl ? '0 0, right bottom' : '0 0',
            backgroundRepeat: 'no-repeat',
          }}
        />
        <CardContent
          sx={{
            position: 'relative',
            zIndex: 1,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: 1.5,
            width: '100%',
            p: 3,
          }}
        >
          <Typography variant="h6" component="h2" fontWeight={700} color="text.primary">
            {title}
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'primary.main' }}>
            <ArrowForwardOutlinedIcon fontSize="small" />
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

