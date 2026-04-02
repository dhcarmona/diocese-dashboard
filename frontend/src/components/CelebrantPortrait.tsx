import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import type { SxProps, Theme } from '@mui/material/styles';
import type { Celebrant } from '../api/celebrants';
import EntityPortrait from './EntityPortrait';

interface CelebrantPortraitProps {
  celebrant: Celebrant;
  size?: number;
  sx?: SxProps<Theme>;
  testId?: string;
}

export default function CelebrantPortrait({
  celebrant,
  size,
  sx,
  testId,
}: Readonly<CelebrantPortraitProps>) {
  return (
    <EntityPortrait
      icon={<PersonOutlineOutlinedIcon />}
      size={size}
      src={celebrant.portraitUrl}
      sx={sx}
      testId={testId}
    />
  );
}
