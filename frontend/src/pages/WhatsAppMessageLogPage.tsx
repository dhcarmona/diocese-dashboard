import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import CircularProgress from '@mui/material/CircularProgress';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type WhatsAppMessageLogEntry, getWhatsAppMessageLogs } from '../api/whatsappLogs';
import PageHeader from '../components/PageHeader';
import { formatDateTime } from '../utils/dateFormatting';

export default function WhatsAppMessageLogPage() {
  const { t, i18n } = useTranslation();

  const [entries, setEntries] = useState<WhatsAppMessageLogEntry[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(25);
  const [loading, setLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setHasError(false);
      try {
        const data = await getWhatsAppMessageLogs(page, rowsPerPage);
        if (active) {
          setEntries(data.content);
          setTotalElements(data.totalElements);
        }
      } catch {
        if (active) setHasError(true);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [page, rowsPerPage]);

  function handlePageChange(_: unknown, newPage: number) {
    setPage(newPage);
  }

  function handleRowsPerPageChange(event: React.ChangeEvent<HTMLInputElement>) {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  }

  return (
    <>
      <PageHeader
        title={t('whatsappLog.title')}
        subtitle={t('whatsappLog.subtitle')}
      />

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={32} />
          <Typography variant="h6">{t('whatsappLog.loading')}</Typography>
        </Box>
      )}

      {!loading && hasError && (
        <Alert severity="error">{t('whatsappLog.loadError')}</Alert>
      )}

      {!loading && !hasError && totalElements === 0 && (
        <Alert severity="info">{t('whatsappLog.empty')}</Alert>
      )}

      {!loading && !hasError && totalElements > 0 && (
        <Paper>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('whatsappLog.columns.sentAt')}</TableCell>
                  <TableCell>{t('whatsappLog.columns.recipient')}</TableCell>
                  <TableCell>{t('whatsappLog.columns.message')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {entries.map((entry) => (
                  <TableRow key={entry.id} hover>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>
                      {formatDateTime(entry.sentAt, i18n.resolvedLanguage)}
                    </TableCell>
                    <TableCell>{entry.recipientUsername}</TableCell>
                    <TableCell>
                      {entry.otp ? (
                        <Chip
                          label={t('whatsappLog.otpSent')}
                          size="small"
                          color="default"
                          variant="outlined"
                        />
                      ) : (
                        <Typography
                          variant="body2"
                          sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
                        >
                          {entry.body}
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={totalElements}
            page={page}
            rowsPerPage={rowsPerPage}
            rowsPerPageOptions={[10, 25, 50]}
            onPageChange={handlePageChange}
            onRowsPerPageChange={handleRowsPerPageChange}
            labelRowsPerPage={t('whatsappLog.pagination.rowsPerPage')}
          />
        </Paper>
      )}
    </>
  );
}
