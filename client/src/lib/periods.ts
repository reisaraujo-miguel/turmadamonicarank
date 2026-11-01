const DAY_MS = 86_400_000;

export function daysUntil(instant: string, now: Date = new Date()): number {
  const end = new Date(instant).getTime();
  return Math.max(0, Math.ceil((end - now.getTime()) / DAY_MS));
}

export function nextCountLabel(instant: string, now: Date = new Date()): string {
  const days = daysUntil(instant, now);
  if (days === 0) {
    return "A contagem fecha hoje";
  }
  if (days === 1) {
    return "A contagem fecha amanhã";
  }
  return `A contagem fecha em ${days} dias`;
}
