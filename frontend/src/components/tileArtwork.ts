const NAVY = '#1C3A6E';

function buildIconArtwork(shapes: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 320">
    <rect width="480" height="320" fill="#E8EEF8"/>
    ${shapes}
  </svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

const s = (opacity: number) => `fill="${NAVY}" fill-opacity="${opacity}"`;

export const tileArtwork = {

  report: buildIconArtwork(`
    <rect x="300" y="55" width="145" height="200" rx="14" ${s(0.28)}/>
    <rect x="322" y="36" width="100" height="36" rx="8" ${s(0.28)}/>
    <rect x="320" y="112" width="103" height="10" rx="5" ${s(0.55)}/>
    <rect x="320" y="138" width="85" height="10" rx="5" ${s(0.55)}/>
    <rect x="320" y="164" width="94" height="10" rx="5" ${s(0.55)}/>
    <rect x="320" y="190" width="70" height="10" rx="5" ${s(0.55)}/>
  `),

  templates: buildIconArtwork(`
    <rect x="285" y="104" width="148" height="110" rx="10" ${s(0.15)}/>
    <rect x="299" y="118" width="148" height="110" rx="10" ${s(0.25)}/>
    <rect x="313" y="132" width="148" height="110" rx="10" ${s(0.38)}/>
    <rect x="330" y="156" width="108" height="10" rx="4" ${s(0.65)}/>
    <rect x="330" y="176" width="88" height="10" rx="4" ${s(0.65)}/>
    <rect x="330" y="196" width="98" height="10" rx="4" ${s(0.65)}/>
  `),

  users: buildIconArtwork(`
    <circle cx="400" cy="108" r="32" ${s(0.22)}/>
    <path d="M335,222 Q400,184 465,222 L468,270 L332,270 Z" ${s(0.22)}/>
    <circle cx="340" cy="116" r="38" ${s(0.50)}/>
    <path d="M265,232 Q340,190 415,232 L420,278 L260,278 Z" ${s(0.50)}/>
  `),

  celebrants: buildIconArtwork(`
    <rect x="370" y="24" width="16" height="54" rx="7" ${s(0.60)}/>
    <rect x="355" y="42" width="46" height="16" rx="7" ${s(0.60)}/>
    <circle cx="378" cy="130" r="40" ${s(0.48)}/>
    <path d="M304,224 Q378,182 452,224 L460,278 L296,278 Z" ${s(0.48)}/>
  `),

  churches: buildIconArtwork(`
    <polygon points="378,44 444,158 312,158" ${s(0.48)}/>
    <rect x="370" y="18" width="16" height="48" rx="5" ${s(0.65)}/>
    <rect x="354" y="36" width="48" height="16" rx="5" ${s(0.65)}/>
    <rect x="298" y="155" width="158" height="120" rx="4" ${s(0.42)}/>
    <rect x="348" y="218" width="56" height="57" rx="5" ${s(0.58)}/>
    <rect x="312" y="174" width="30" height="30" rx="4" ${s(0.65)}/>
    <rect x="414" y="174" width="30" height="30" rx="4" ${s(0.65)}/>
  `),

  links: buildIconArtwork(`
    <circle cx="348" cy="188" r="34" ${s(0.55)}/>
    <circle cx="436" cy="136" r="30" ${s(0.55)}/>
    <circle cx="436" cy="240" r="30" ${s(0.55)}/>
    <rect x="370" y="153" width="58" height="16" rx="8"
      transform="rotate(-26 399 161)" ${s(0.62)}/>
    <rect x="370" y="193" width="58" height="16" rx="8"
      transform="rotate(26 399 201)" ${s(0.62)}/>
  `),

  viewReports: buildIconArtwork(`
    <rect x="296" y="274" width="172" height="10" rx="4" ${s(0.60)}/>
    <rect x="300" y="196" width="46" height="78" rx="6" ${s(0.38)}/>
    <rect x="356" y="140" width="46" height="134" rx="6" ${s(0.60)}/>
    <rect x="412" y="166" width="46" height="108" rx="6" ${s(0.48)}/>
  `),

  whatsappLog: buildIconArtwork(`
    <rect x="284" y="70" width="176" height="144" rx="26" ${s(0.48)}/>
    <polygon points="304,214 278,262 360,214" ${s(0.48)}/>
    <rect x="306" y="104" width="132" height="12" rx="5" ${s(0.70)}/>
    <rect x="306" y="130" width="112" height="12" rx="5" ${s(0.70)}/>
    <rect x="306" y="156" width="122" height="12" rx="5" ${s(0.70)}/>
  `),

} as const;



