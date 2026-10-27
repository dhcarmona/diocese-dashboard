import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/auth-context';
import ActionTile from '../components/ActionTile';
import PageHeader from '../components/PageHeader';
import TileGrid from '../components/TileGrid';
import { tileArtwork } from '../components/tileArtwork';

export default function ReportsHubPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

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
        <ActionTile
          title={t('reportsHub.tiles.statistics.title')}
          description={t('reportsHub.tiles.statistics.description')}
          to="/statistics"
          imageUrl={tileArtwork.viewReports}
        />
        {isAdmin && (
          <ActionTile
            title={t('reportsHub.tiles.conventionReport.title')}
            description={t('reportsHub.tiles.conventionReport.description')}
            to="/convention-report"
            imageUrl={tileArtwork.templates}
          />
        )}
      </TileGrid>
    </>
  );
}
