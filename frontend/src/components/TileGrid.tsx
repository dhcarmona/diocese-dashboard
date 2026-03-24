import type { ReactNode } from 'react';
import Box from '@mui/material/Box';

export default function TileGrid({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <Box
      sx={{
        display: 'grid',
        gap: 3,
        gridTemplateColumns: {
          xs: '1fr',
          sm: 'repeat(2, minmax(0, 1fr))',
          lg: 'repeat(3, minmax(0, 1fr))',
        },
      }}
    >
      {children}
    </Box>
  );
}
