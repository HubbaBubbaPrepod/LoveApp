export interface IntimacyLevelDefinition {
  level: number;
  name: string;
  minScore: number;
}

export const INTIMACY_LEVELS: IntimacyLevelDefinition[] = [
  { level: 1, name: 'Знакомство', minScore: 0 },
  { level: 2, name: 'Симпатия', minScore: 100 },
  { level: 3, name: 'Влюблённость', minScore: 300 },
  { level: 4, name: 'Любовь', minScore: 600 },
  { level: 5, name: 'Глубокая связь', minScore: 1000 },
  { level: 6, name: 'Родственные души', minScore: 1500 },
  { level: 7, name: 'Вечная любовь', minScore: 2500 },
];
