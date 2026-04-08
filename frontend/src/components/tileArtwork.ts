function buildArtwork(primary: string, secondary: string, accent: string): string {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 320">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${primary}" />
          <stop offset="100%" stop-color="${secondary}" />
        </linearGradient>
      </defs>
      <rect width="480" height="320" fill="url(#bg)" />
      <circle cx="390" cy="72" r="54" fill="${accent}" fill-opacity="0.22" />
      <circle cx="120" cy="268" r="80" fill="${accent}" fill-opacity="0.18" />
      <rect x="76" y="58" width="142" height="26" rx="13" fill="${accent}" fill-opacity="0.18" />
      <rect x="76" y="106" width="234" height="18" rx="9" fill="${accent}" fill-opacity="0.12" />
      <rect x="76" y="142" width="200" height="18" rx="9" fill="${accent}" fill-opacity="0.12" />
      <rect x="290" y="176" width="112" height="72" rx="18" fill="${accent}" fill-opacity="0.2" />
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

export const tileArtwork = {
  report: buildArtwork('#E3F2FD', '#BBDEFB', '#1565C0'),
  templates: buildArtwork('#F3E5F5', '#E1BEE7', '#7B1FA2'),
  users: buildArtwork('#FFF3E0', '#FFE0B2', '#EF6C00'),
  celebrants: buildArtwork('#E8F5E9', '#C8E6C9', '#2E7D32'),
  churches: buildArtwork('#E0F7FA', '#B2EBF2', '#00838F'),
  links: buildArtwork('#FBE9E7', '#FFCCBC', '#D84315'),
  viewReports: buildArtwork('#FFF8E1', '#FFECB3', '#F57F17'),
  whatsappLog: buildArtwork('#E8F5E9', '#A5D6A7', '#1B5E20'),
} as const;
