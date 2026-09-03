import store from './Variable/store';

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

// ---- Domain types ----

/** A single AI skill ranked by daily mention volume. */
export interface AISkill {
  rank: number;
  skill: string;
  mentions: number;
}

/** A tracked US stock index snapshot. */
export interface UsStockIndex {
  symbol: string;
  name: string;
  price: number;
  change: number;
  changePercent: number;
  date: string;
}

/** ISO-ish currency code, e.g. "USD" / "CNY". */
export type CurrencyCode = string;

/** Locally persisted (and editable) user profile data. */
export interface UserData {
  bio: string;
  avatar: string;
  birthday: string;
  gender: string;
}

/** Shape of the profile payload returned by the backend. */
export interface ProfileData {
  bio: string;
  avatarUrl: string;
  birthday: string;
  gender: string;
}

/** Auth endpoint response contract. */
export interface AuthResponse {
  result: string;
}

/** General preferences persisted for the authenticated user. */
export interface GeneralSettingsData {
  defaultPage: number;
  defaultBaseCurrency: CurrencyCode;
  defaultQuoteCurrency: CurrencyCode;
}

/** Per-user colors for the main shell and interactive globe. */
export interface DisplaySettingsData {
  backgroundColor: string;
  globeGlowColor: string;
  globePointColor: string;
  globeMarkerColor: string;
}

/** Current z-score sentiment and today's mean of normalized observations. */
export interface SentimentScore {
  /** Current FinBERT score standardized by its persisted rolling mean and standard deviation. */
  normalizedScore: number | null;
  /** Mean of all normalized sentiment observations persisted today. */
  dailyAverage: number | null;
}

/** One stock index's captured price and percentage change on a trading date. */
export interface MarketTrendStockPoint {
  symbol: string;
  name: string;
  price: number;
  changePercent: number;
}

/** Daily market sentiment and US stock observations. */
export interface MarketTrendPoint {
  date: string;
  sentiment: number | null;
  stocks: MarketTrendStockPoint[];
}

/** Pearson correlation between sentiment and one stock index's captured daily change. */
export interface MarketCorrelation {
  symbol: string;
  name: string;
  corr: number | null;
  sampleSize: number;
}

/** Seven calendar days of trend data plus full-history persisted correlations. */
export interface MarketTrendData {
  points: MarketTrendPoint[];
  correlations: MarketCorrelation[];
}

/** A single tab descriptor used by the top tab bar. */
export interface TabItem {
  key: number;
  label: string;
}
