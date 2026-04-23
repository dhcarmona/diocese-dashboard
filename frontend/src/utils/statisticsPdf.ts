import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import type { StatisticsReport } from '../api/statistics';

const PAGE_W = 210; // A4 width mm
const PAGE_H = 297; // A4 height mm
const MARGIN = 15;
const CONTENT_W = PAGE_W - 2 * MARGIN;
const BRAND_BLUE: [number, number, number] = [28, 58, 110];

export interface PdfLabels {
  templateName: string;
  churchDisplay: string;
  dateRange: string;
  totalServicesLabel: string;
  totalServicesCount: number;
  pendingLinksTitle: string;
  pendingLinksSubtitle: string;
  noPendingLinks: string;
  reporterHeader: string;
  churchHeader: string;
  activeDateHeader: string;
  linkHeader: string;
}

async function fetchLogoDataUrl(): Promise<string> {
  const resp = await fetch('/logo.png');
  const blob = await resp.blob();
  return new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onload = () => { resolve(reader.result as string); };
    reader.readAsDataURL(blob);
  });
}

async function loadImageElement(src: string): Promise<HTMLImageElement> {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image();
    img.onload = () => { resolve(img); };
    img.onerror = reject;
    img.src = src;
  });
}

/**
 * Renders the chart content element into the PDF, splitting across pages if needed.
 * Returns the Y position after the last rendered slice.
 */
async function renderChartContent(
  pdf: jsPDF,
  el: HTMLElement,
  startY: number,
): Promise<number> {
  const canvas = await html2canvas(el, {
    scale: 2,
    useCORS: true,
    logging: false,
    backgroundColor: '#ffffff',
  });

  const pxPerMm = canvas.width / CONTENT_W;
  let srcY = 0;
  let yPos = startY;
  let firstSlice = true;

  while (srcY < canvas.height) {
    const availMm = firstSlice
      ? PAGE_H - MARGIN - yPos - MARGIN
      : PAGE_H - 2 * MARGIN;
    const slicePx = Math.min(Math.round(availMm * pxPerMm), canvas.height - srcY);
    const sliceMm = slicePx / pxPerMm;

    const sliceCanvas = document.createElement('canvas');
    sliceCanvas.width = canvas.width;
    sliceCanvas.height = slicePx;
    const ctx = sliceCanvas.getContext('2d');
    if (!ctx) throw new Error('Could not get 2D context');
    ctx.drawImage(canvas, 0, -srcY);

    if (!firstSlice) {
      pdf.addPage();
      yPos = MARGIN;
    }

    pdf.addImage(sliceCanvas.toDataURL('image/png'), 'PNG', MARGIN, yPos, CONTENT_W, sliceMm);
    yPos += sliceMm;
    srcY += slicePx;
    firstSlice = false;
  }

  return yPos;
}

/**
 * Generates and downloads a PDF for the statistics report page.
 */
export async function downloadStatisticsPdf(
  report: StatisticsReport,
  chartContentEl: HTMLElement,
  labels: PdfLabels,
): Promise<void> {
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

  // ── Header ────────────────────────────────────────────────────────────────
  let yPos = MARGIN;
  const logoH = 12;

  try {
    const logoDataUrl = await fetchLogoDataUrl();
    const logoImg = await loadImageElement(logoDataUrl);
    const aspect = logoImg.naturalWidth / logoImg.naturalHeight;
    const logoW = Math.min(logoH * aspect, 55);
    pdf.addImage(logoDataUrl, 'PNG', MARGIN, yPos, logoW, logoH);

    // Title vertically centered next to the logo
    pdf.setFontSize(20);
    pdf.setFont('helvetica', 'bold');
    pdf.setTextColor(...BRAND_BLUE);
    const titleX = MARGIN + logoW + 5;
    const titleY = yPos + logoH / 2 + 3.5; // center on logo height (cap-height offset ~3.5mm)
    pdf.text(labels.templateName, titleX, titleY);
  } catch {
    // Logo failed to load – render title without it
    pdf.setFontSize(20);
    pdf.setFont('helvetica', 'bold');
    pdf.setTextColor(...BRAND_BLUE);
    pdf.text(labels.templateName, MARGIN, yPos + logoH / 2 + 3.5);
  }

  yPos += logoH + 5;

  pdf.setFontSize(10);
  pdf.setFont('helvetica', 'normal');
  pdf.setTextColor(80, 80, 80);
  const subtitle = [
    labels.churchDisplay,
    labels.dateRange,
    `${labels.totalServicesLabel}: ${labels.totalServicesCount}`,
  ].join('  ·  ');
  pdf.text(subtitle, MARGIN, yPos);
  yPos += 5;

  pdf.setDrawColor(200);
  pdf.setLineWidth(0.3);
  pdf.line(MARGIN, yPos, PAGE_W - MARGIN, yPos);
  yPos += 7;
  pdf.setTextColor(0);

  // ── Chart content ─────────────────────────────────────────────────────────
  yPos = await renderChartContent(pdf, chartContentEl, yPos);

  // Ensure there is space for the pending links header + at least a couple rows
  if (yPos + 50 > PAGE_H - MARGIN) {
    pdf.addPage();
    yPos = MARGIN;
  } else {
    yPos += 8;
  }

  // ── Pending links ─────────────────────────────────────────────────────────
  pdf.setFontSize(13);
  pdf.setFont('helvetica', 'bold');
  pdf.setTextColor(...BRAND_BLUE);
  pdf.text(labels.pendingLinksTitle, MARGIN, yPos);
  yPos += 5;

  pdf.setFontSize(9);
  pdf.setFont('helvetica', 'normal');
  pdf.setTextColor(100);
  pdf.text(labels.pendingLinksSubtitle, MARGIN, yPos);
  yPos += 5;
  pdf.setTextColor(0);

  if (report.pendingLinks.length === 0) {
    pdf.setFontSize(10);
    pdf.setFont('helvetica', 'italic');
    pdf.setTextColor(150);
    pdf.text(labels.noPendingLinks, MARGIN, yPos);
  } else {
    const urlColIdx = report.global ? 3 : 2;

    const head = report.global
      ? [[labels.reporterHeader, labels.churchHeader, labels.activeDateHeader, labels.linkHeader]]
      : [[labels.reporterHeader, labels.activeDateHeader, labels.linkHeader]];

    const body = report.pendingLinks.map((link) => {
      const reporter = link.reporterFullName
        ? `${link.reporterFullName} (${link.reporterUsername})`
        : link.reporterUsername;
      const url = `${window.location.origin}/r/${link.token}`;
      return report.global
        ? [reporter, link.churchName, link.activeDate, url]
        : [reporter, link.activeDate, url];
    });

    autoTable(pdf, {
      startY: yPos,
      head,
      body,
      margin: { left: MARGIN, right: MARGIN },
      styles: { fontSize: 9, cellPadding: 3, overflow: 'linebreak' },
      headStyles: { fillColor: BRAND_BLUE, textColor: [255, 255, 255], fontStyle: 'bold' },
      alternateRowStyles: { fillColor: [240, 245, 255] },
      columnStyles: {
        [urlColIdx]: { textColor: [30, 100, 200] },
      },
      didDrawCell: (data) => {
        if (data.section === 'body' && data.column.index === urlColIdx) {
          const link = report.pendingLinks[data.row.index];
          if (link) {
            const url = `${window.location.origin}/r/${link.token}`;
            pdf.link(data.cell.x, data.cell.y, data.cell.width, data.cell.height, { url });
          }
        }
      },
    });
  }

  const safeName = labels.templateName.replace(/[^a-z0-9]/gi, '-').toLowerCase();
  pdf.save(`statistics-${safeName}-${report.startDate}-${report.endDate}.pdf`);
}
