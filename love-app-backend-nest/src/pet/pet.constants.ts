export const MAX_LEVEL = 60;
export const MAX_STAT = 100;
export const MAX_ENERGY = 100;
export const DECAY_INTERVAL_MS = 4 * 60 * 60 * 1000; // 4 hours
export const DECAY_AMOUNT = 5;
export const CARE_COOLDOWN_MS = 60 * 60 * 1000; // 1 hour
export const ENERGY_REGEN_PER_HOUR = 10;

export function xpForLevel(level: number): number {
  return Math.floor(100 * Math.pow(1.15, level - 1));
}

export function calcLevel(totalXp: number): { level: number; remainingXp: number } {
  let level = 1;
  let xpUsed = 0;
  while (level < MAX_LEVEL) {
    const needed = xpForLevel(level);
    if (xpUsed + needed > totalXp) break;
    xpUsed += needed;
    level++;
  }
  return { level, remainingXp: totalXp - xpUsed };
}

export function computeMood(hunger: number, happiness: number, cleanliness: number): string {
  const avg = (hunger + happiness + cleanliness) / 3;
  if (avg >= 70) return 'happy';
  if (avg >= 40) return 'neutral';
  return 'sad';
}
