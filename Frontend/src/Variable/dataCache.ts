import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
  pullCurrencies,
  pullExchangeRate,
  pullPopularAISkills,
  pullSentimentScore,
  pullUsStockIndices,
  pullMarketSignal,
} from "../API/data";
import type { RootState, AISkill, UsStockIndex, CurrencyCode, SentimentScore, MarketSignalData } from "../customTypes";

export const DATA_REFRESH_MS = 5 * 60 * 1000;

interface CacheEntry<T> {
  updatedAt?: number;
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface ExchangeRateCache {
  updatedAt?: Record<string, number>;
  data: Record<string, number>;
  loading: boolean;
  error: string | null;
}

interface DataCacheState {
  marketSignal: CacheEntry<MarketSignalData>;
  aiSkills: CacheEntry<AISkill[]>;
  sentimentScore: CacheEntry<SentimentScore>;
  currencies: CacheEntry<CurrencyCode[]>;
  exchangeRates: ExchangeRateCache;
  usStockIndices: CacheEntry<UsStockIndex[]>;
}

const createEntry = <T>(): CacheEntry<T> => ({
  data: null,
  loading: false,
  error: null,
});

const initialState: DataCacheState = {
  marketSignal: createEntry<MarketSignalData>(),
  aiSkills: createEntry<AISkill[]>(),
  sentimentScore: createEntry<SentimentScore>(),
  currencies: createEntry<CurrencyCode[]>(),
  exchangeRates: { data: {}, loading: false, error: null },
  usStockIndices: createEntry<UsStockIndex[]>(),
};

export const fetchAISkills = createAsyncThunk<
  AISkill[],
  { force?: boolean } | void,
  { state: RootState }
>("dataCache/fetchAISkills", async () => pullPopularAISkills(), {
  condition: (options, { getState }) => {
    const cached = getState().dataCache.aiSkills;
    return !cached.loading && (options?.force || !cached.updatedAt
      || Date.now() - cached.updatedAt >= DATA_REFRESH_MS);
  },
});

export const fetchSentimentScore = createAsyncThunk<
  SentimentScore,
  { force?: boolean } | void,
  { state: RootState }
>("dataCache/fetchSentimentScore", async () => pullSentimentScore(), {
  condition: (options, { getState }) => {
    const cached = getState().dataCache.sentimentScore;
    return !cached.loading && (options?.force || !cached.updatedAt
      || Date.now() - cached.updatedAt >= DATA_REFRESH_MS);
  },
});

export const fetchCurrencies = createAsyncThunk<
  CurrencyCode[],
  { force?: boolean } | void,
  { state: RootState }
>("dataCache/fetchCurrencies", async () => pullCurrencies(), {
  condition: (options, { getState }) => {
    const cached = getState().dataCache.currencies;
    return !cached.loading && (options?.force || !cached.updatedAt
      || Date.now() - cached.updatedAt >= DATA_REFRESH_MS);
  },
});

export const fetchExchangeRate = createAsyncThunk<
  { key: string; rate: number },
  { base: CurrencyCode; quote: CurrencyCode; force?: boolean },
  { state: RootState }
>("dataCache/fetchExchangeRate", async ({ base, quote }) => ({
  key: `${base}-${quote}`,
  rate: await pullExchangeRate(base, quote),
}), {
  condition: ({ base, quote, force }, { getState }) => {
    const updatedAt = getState().dataCache.exchangeRates.updatedAt?.[`${base}-${quote}`];
    return force || !updatedAt || Date.now() - updatedAt >= DATA_REFRESH_MS;
  },
});

export const fetchUsStockIndices = createAsyncThunk<
  UsStockIndex[],
  { force?: boolean } | void,
  { state: RootState }
>("dataCache/fetchUsStockIndices", async () => pullUsStockIndices(), {
  condition: (options, { getState }) => {
    const cached = getState().dataCache.usStockIndices;
    return !cached.loading && (options?.force || !cached.updatedAt
      || Date.now() - cached.updatedAt >= DATA_REFRESH_MS);
  },
});

export const fetchMarketSignal = createAsyncThunk<
  MarketSignalData,
  { force?: boolean } | void,
  { state: RootState }
>("dataCache/fetchMarketSignal", async () => pullMarketSignal(), {
  condition: (options, { getState }) => {
    const cached = getState().dataCache.marketSignal;
    return !cached.loading && (options?.force || !cached.updatedAt
      || Date.now() - cached.updatedAt >= DATA_REFRESH_MS);
  },
});

const dataCacheSlice = createSlice({
  name: "dataCache",
  initialState,
  reducers: {
    clearDataCache(state) {
      state.marketSignal = createEntry<MarketSignalData>();
      state.aiSkills = createEntry<AISkill[]>();
      state.sentimentScore = createEntry<SentimentScore>();
      state.currencies = createEntry<CurrencyCode[]>();
      state.exchangeRates = { data: {}, loading: false, error: null };
      state.usStockIndices = createEntry<UsStockIndex[]>();
    },
  },
  extraReducers: (builder) => {
    // Keep previous data and its timestamp when a background request fails.
    builder
      .addCase(fetchMarketSignal.pending, (state) => {
        state.marketSignal.loading = true;
      })
      .addCase(fetchMarketSignal.fulfilled, (state, action) => {
        state.marketSignal = { data: action.payload, loading: false, error: null, updatedAt: Date.now() };
      })
      .addCase(fetchMarketSignal.rejected, (state, action) => {
        state.marketSignal.loading = false;
        state.marketSignal.error = action.error.message || "加载失败";
      });
    // AI Skills
    builder
      .addCase(fetchAISkills.pending, (state) => {
        if (!state.aiSkills.data) state.aiSkills.loading = true;
      })
      .addCase(fetchAISkills.fulfilled, (state, action) => {
        state.aiSkills.data = action.payload;
        state.aiSkills.updatedAt = Date.now();
        state.aiSkills.loading = false;
        state.aiSkills.error = null;
      })
      .addCase(fetchAISkills.rejected, (state, action) => {
        state.aiSkills.loading = false;
        state.aiSkills.error = action.error.message || "加载失败";
      });

    // Sentiment Score
    builder
      .addCase(fetchSentimentScore.pending, (state) => {
        if (state.sentimentScore.data === null) state.sentimentScore.loading = true;
      })
      .addCase(fetchSentimentScore.fulfilled, (state, action) => {
        state.sentimentScore.data = action.payload;
        state.sentimentScore.updatedAt = Date.now();
        state.sentimentScore.loading = false;
        state.sentimentScore.error = null;
      })
      .addCase(fetchSentimentScore.rejected, (state, action) => {
        state.sentimentScore.loading = false;
        state.sentimentScore.error = action.error.message || "加载失败";
      });

    // Currencies
    builder
      .addCase(fetchCurrencies.pending, (state) => {
        if (!state.currencies.data) state.currencies.loading = true;
      })
      .addCase(fetchCurrencies.fulfilled, (state, action) => {
        state.currencies.data = action.payload;
        state.currencies.updatedAt = Date.now();
        state.currencies.loading = false;
        state.currencies.error = null;
      })
      .addCase(fetchCurrencies.rejected, (state, action) => {
        state.currencies.loading = false;
        state.currencies.error = action.error.message || "加载失败";
      });

    // Exchange Rate
    builder
      .addCase(fetchExchangeRate.pending, (state) => {
        state.exchangeRates.loading = true;
      })
      .addCase(fetchExchangeRate.fulfilled, (state, action) => {
        state.exchangeRates.data[action.payload.key] = action.payload.rate;
        state.exchangeRates.updatedAt ??= {};
        state.exchangeRates.updatedAt[action.payload.key] = Date.now();
        state.exchangeRates.loading = false;
        state.exchangeRates.error = null;
      })
      .addCase(fetchExchangeRate.rejected, (state, action) => {
        state.exchangeRates.loading = false;
        state.exchangeRates.error = action.error.message || "加载失败";
      });

    // US Stock Indices
    builder
      .addCase(fetchUsStockIndices.pending, (state) => {
        if (!state.usStockIndices.data) state.usStockIndices.loading = true;
      })
      .addCase(fetchUsStockIndices.fulfilled, (state, action) => {
        state.usStockIndices.data = action.payload;
        state.usStockIndices.updatedAt = Date.now();
        state.usStockIndices.loading = false;
        state.usStockIndices.error = null;
      })
      .addCase(fetchUsStockIndices.rejected, (state, action) => {
        state.usStockIndices.loading = false;
        state.usStockIndices.error = action.error.message || "加载失败";
      });
  },
});

export const { clearDataCache } = dataCacheSlice.actions;
export const dataCacheSlice_ = dataCacheSlice;

export const selectAISkills = (s: RootState) => s.dataCache.aiSkills;
export const selectSentimentScore = (s: RootState) => s.dataCache.sentimentScore;
export const selectCurrencies = (s: RootState) => s.dataCache.currencies;
export const selectExchangeRates = (s: RootState) => s.dataCache.exchangeRates;
export const selectUsStockIndices = (s: RootState) => s.dataCache.usStockIndices;
export const selectMarketSignal = (s: RootState) => s.dataCache.marketSignal;
