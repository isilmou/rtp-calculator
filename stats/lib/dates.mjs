// Utilitaires de dates. Toutes les dates circulent au format AAAA-MM-JJ.

export function toYMD(date) {
  return date.toISOString().slice(0, 10);
}

export function parseYMD(ymd) {
  const [y, m, d] = ymd.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}

// Format attendu par les API AdMob et Play (objet { year, month, day }).
export function toDateParts(ymd) {
  const [year, month, day] = ymd.split('-').map(Number);
  return { year, month, day };
}

export function fromDateParts(parts) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${parts.year}-${pad(parts.month)}-${pad(parts.day)}`;
}

export function addDays(ymd, days) {
  const d = parseYMD(ymd);
  d.setUTCDate(d.getUTCDate() + days);
  return toYMD(d);
}

export function today() {
  return toYMD(new Date());
}

export function minYMD(a, b) {
  return a < b ? a : b;
}

export function maxYMD(a, b) {
  return a > b ? a : b;
}
