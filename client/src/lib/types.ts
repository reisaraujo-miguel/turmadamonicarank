export interface UserSelf {
  id: string;
  email: string;
  displayName: string | null;
  emailConfirmed: boolean;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserSelf;
}

export interface RegisterResponse {
  user: UserSelf;
}

export interface ApiFieldError {
  field: string;
  code: string;
  message: string;
}

export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: ApiFieldError[];
}

export type CharacterStatus = "ACTIVE" | "INACTIVE";

export interface CharacterSummary {
  id: string;
  slug: string;
  name: string;
  imageUrl: string | null;
  imageAlt: string | null;
  status: CharacterStatus;
}

export interface FirstAppearance {
  year: number | null;
  work: string | null;
}

export interface CharacterDetail extends CharacterSummary {
  fullName: string | null;
  bio: string | null;
  imageCredit: string | null;
  sourceUrl: string | null;
  firstAppearance: FirstAppearance | null;
  aliases: string[];
  currentRank: number | null;
}

export interface PageResponse<T> {
  items: T[];
  limit: number;
  offset: number;
  total: number;
}

export interface SnapshotSummary {
  year: number;
  month: number;
  cutoffAt: string;
  generatedAt: string;
  totalBallots: number;
  totalCharacters: number;
}

export interface RankEntry {
  rank: number;
  character: CharacterSummary;
  points: number;
  firstPlaceVotes: number;
  ballotCount: number;
}

export interface RankResponse {
  generatedAt: string;
  nextCutoffAt: string;
  totalBallots: number;
  latestSnapshot: SnapshotSummary | null;
  items: RankEntry[];
  limit: number;
  offset: number;
  total: number;
}

export interface BallotEntry {
  position: number;
  character: CharacterSummary;
}

export interface Ballot {
  id: string;
  updatedAt: string;
  entries: BallotEntry[];
}

export interface BallotEnvelope {
  ballot: Ballot | null;
}
