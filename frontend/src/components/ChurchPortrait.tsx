import LocationCityOutlinedIcon from '@mui/icons-material/LocationCityOutlined';
import type { SxProps, Theme } from '@mui/material/styles';
import type { Church } from '../api/churches';
import EntityPortrait from './EntityPortrait';

interface ChurchPortraitProps {
  church: Church;
  size?: number;
  sx?: SxProps<Theme>;
  testId?: string;
}

export default function ChurchPortrait({
  church,
  size,
  sx,
  testId,
}: Readonly<ChurchPortraitProps>) {
  return (
    <EntityPortrait
      icon={<LocationCityOutlinedIcon />}
      size={size}
      src={church.portraitDataUrl}
      sx={sx}
      testId={testId}
    />
  );
}
