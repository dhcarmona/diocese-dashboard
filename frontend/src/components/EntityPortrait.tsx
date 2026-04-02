import Avatar from '@mui/material/Avatar';
import type { SxProps, Theme } from '@mui/material/styles';
import type { ReactNode } from 'react';

interface EntityPortraitProps {
  icon: ReactNode;
  size?: number;
  src?: string | null;
  sx?: SxProps<Theme>;
  testId?: string;
}

export default function EntityPortrait({
  icon,
  size = 88,
  src,
  sx,
  testId,
}: Readonly<EntityPortraitProps>) {
  return (
    <Avatar
      alt=""
      aria-hidden="true"
      data-testid={testId}
      src={src ?? undefined}
      variant="rounded"
      sx={[
        {
          width: size,
          height: size,
          flexShrink: 0,
          borderRadius: 2,
          bgcolor: 'grey.100',
          color: 'primary.main',
          '& .MuiAvatar-img': {
            objectFit: 'cover',
          },
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {icon}
    </Avatar>
  );
}
