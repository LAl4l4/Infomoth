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
  token?: string;
}

/** A single tab descriptor used by the top tab bar. */
export interface TabItem {
  key: number;
  label: string;
}
