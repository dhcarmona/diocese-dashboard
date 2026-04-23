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
  if (!resp.ok) {
    throw new Error(`Failed to fetch logo: ${resp.status} ${resp.statusText}`);
  }
  const blob = await resp.blob();
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => { resolve(reader.result as string); };
    reader.onerror = () => { reject(new Error('Failed to read logo file')); };
    reader.onabort = () => { reject(new Error('Logo file read was aborted')); };
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
 * Renders the chart content element into the PDF page by page, capturing one
 * page-sized slice at a time so memory usage stays bounded on long reports.
 * Returns the Y position after the last rendered slice.
 */
async function renderChartContent(
  pdf: jsPDF,
  el: HTMLElement,
  startY: number,
): Promise<number> {
  const RENDER_SCALE = 2;
  const sourceW = Math.max(1, el.scrollWidth);
  const sourceH = Math.max(1, el.scrollHeight);
  // CSS pixels per mm in the PDF
  const sourcePxPerMm = sourceW / CONTENT_W;

  const baseOptions = {
    scale: RENDER_SCALE,
    useCORS: true,
    logging: false,
    backgroundColor: '#ffffff',
    width: sourceW,
    windowWidth: Math.max(document.documentElement.clientWidth, sourceW),
    windowHeight: Math.max(window.innerHeight, sourceH),
    scrollX: 0,
    scrollY: 0,
  };

  let srcY = 0;
  let yPos = startY;

  while (srcY < sourceH) {
    let availMm = PAGE_H - yPos - MARGIN;

    if (availMm < 5) {
      pdf.addPage();
      yPos = MARGIN;
      availMm = PAGE_H - 2 * MARGIN;
    }

    // Source CSS pixels that fit into the available PDF height
    const slicePx = Math.min(
      Math.max(1, Math.round(availMm * sourcePxPerMm)),
      sourceH - srcY,
    );
    const sliceMm = slicePx / sourcePxPerMm;

    // Capture only this slice of the element; avoids a single giant canvas
    const sliceCanvas = await html2canvas(el, { ...baseOptions, y: srcY, height: slicePx });

    pdf.addImage(sliceCanvas.toDataURL('image/png'), 'PNG', MARGIN, yPos, CONTENT_W, sliceMm);
    yPos += sliceMm;
    srcY += slicePx;
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

    // Measure title width so the logo+title block can be centered together
    pdf.setFontSize(20);
    pdf.setFont('helvetica', 'bold');
    const titleWidth = pdf.getTextWidth(labels.templateName);
    const blockW = logoW + 5 + titleWidth;
    const blockX = (PAGE_W - blockW) / 2;

    pdf.addImage(logoDataUrl, 'PNG', blockX, yPos, logoW, logoH);
    pdf.setTextColor(...BRAND_BLUE);
    pdf.text(labels.templateName, blockX + logoW + 5, yPos + logoH / 2 + 3.5);
  } catch {
    // Logo failed to load – render title without it
    pdf.setFontSize(20);
    pdf.setFont('helvetica', 'bold');
    pdf.setTextColor(...BRAND_BLUE);
    pdf.text(labels.templateName, PAGE_W / 2, yPos + logoH / 2 + 3.5, { align: 'center' });
  }

  yPos += logoH + 5;

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
