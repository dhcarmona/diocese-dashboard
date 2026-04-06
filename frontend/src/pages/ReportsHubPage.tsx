import { useTranslation } from 'react-i18next';
import ActionTile from '../components/ActionTile';
import PageHeader from '../components/PageHeader';
import TileGrid from '../components/TileGrid';
import { tileArtwork } from '../components/tileArtwork';

export default function ReportsHubPage() {
  const { t } = useTranslation();

  return (
    <>
      <PageHeader title={t('reportsHub.title')} subtitle={t('reportsHub.subtitle')} />
      <TileGrid>
        <ActionTile
          title={t('reportsHub.tiles.individualReports.title')}
          description={t('reportsHub.tiles.individualReports.description')}
          to="/reports/view/individual"
          imageUrl={tileArtwork.report}
        />
      </TileGrid>
    </>
  );
}
