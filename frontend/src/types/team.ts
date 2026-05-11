import type { GameType } from "./common";

export interface Team {
  id: number;
  name: string;
  game: GameType;
  logoUrl: string;
  totalMatches: number;
  registeredAt: string;
}
