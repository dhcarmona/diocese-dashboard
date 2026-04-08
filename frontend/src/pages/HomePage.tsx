import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/auth-context';
import ActionTile from '../components/ActionTile';
import PageHeader from '../components/PageHeader';
import TileGrid from '../components/TileGrid';
import { tileArtwork } from '../components/tileArtwork';

interface HomeTile {
  id: string;
  title: string;
  description: string;
  to: string;
  imageUrl: string;
}

export default function HomePage() {
  const { t } = useTranslation();
  const { user } = useAuth();

  const reporterTiles: HomeTile[] = [
    {
      id: 'submit-report',
      title: t('home.tiles.submitReport.title'),
      description: t('home.tiles.submitReport.description'),
      to: '/reports/new',
      imageUrl: tileArtwork.report,
    },
  ];

  const adminTiles: HomeTile[] = [
    {
      id: 'create-service-templates',
      title: t('home.tiles.createServiceTemplates.title'),
      description: t('home.tiles.createServiceTemplates.description'),
      to: '/service-templates/manage',
      imageUrl: tileArtwork.templates,
    },
    {
      id: 'create-reporter-users',
      title: t('home.tiles.createReporterUsers.title'),
      description: t('home.tiles.createReporterUsers.description'),
      to: '/users/manage',
      imageUrl: tileArtwork.users,
    },
    {
      id: 'manage-celebrants',
      title: t('home.tiles.manageCelebrants.title'),
      description: t('home.tiles.manageCelebrants.description'),
      to: '/celebrants/manage',
      imageUrl: tileArtwork.celebrants,
    },
    {
      id: 'manage-churches',
      title: t('home.tiles.manageChurches.title'),
      description: t('home.tiles.manageChurches.description'),
      to: '/churches/manage',
      imageUrl: tileArtwork.churches,
    },
    {
      id: 'create-reporter-links',
      title: t('home.tiles.createReporterLinks.title'),
      description: t('home.tiles.createReporterLinks.description'),
      to: '/reporter-links/manage',
      imageUrl: tileArtwork.links,
    },
    {
      id: 'see-reports',
      title: t('home.tiles.seeReports.title'),
      description: t('home.tiles.seeReports.description'),
      to: '/reports/view',
      imageUrl: tileArtwork.viewReports,
    },
    {
      id: 'whatsapp-log',
      title: t('home.tiles.whatsappLog.title'),
      description: t('home.tiles.whatsappLog.description'),
      to: '/whatsapp-logs',
      imageUrl: tileArtwork.whatsappLog,
    },
  ];

  const tiles = user?.role === 'ADMIN' ? [...reporterTiles, ...adminTiles] : reporterTiles;
  const subtitleKey =
    user?.role === 'ADMIN' ? 'home.subtitleAdmin' : 'home.subtitleReporter';

  return (
    <>
      <PageHeader title={t('home.title')} subtitle={t(subtitleKey)} />
      <TileGrid>
        {tiles.map((tile) => (
          <ActionTile
            key={tile.id}
            title={tile.title}
            description={tile.description}
            to={tile.to}
            imageUrl={tile.imageUrl}
          />
        ))}
      </TileGrid>
    </>
  );
}
