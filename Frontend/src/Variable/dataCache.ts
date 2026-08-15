import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
  pullCurrencies,
  pullExchangeRate,
  pullPopularAISkills,
  pullSentimentScore,
  pullUsStockIndices,
} from "../API/data";
import type { RootState, AISkill, UsStockIndex, CurrencyCode, SentimentScore } from "../customTypes";

interface CacheEntry<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface ExchangeRateCache {
  data: Record<string, number>;
  loading: boolean;
  error: string | null;
}

interface DataCacheState {
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
  aiSkills: createEntry<AISkill[]>(),
  sentimentScore: createEntry<SentimentScore>(),
  currencies: createEntry<CurrencyCode[]>(),
  exchangeRates: { data: {}, loading: false, error: null },
  usStockIndices: createEntry<UsStockIndex[]>(),
};

export const fetchAISkills = createAsyncThunk<
  AISkill[],
  void,
  { state: RootState }
>("dataCache/fetchAISkills", async (_, { getState }) => {
  const cached = getState().dataCache.aiSkills;
  if (cached.data) return cached.data;
  return await pullPopularAISkills();
});

export const fetchSentimentScore = createAsyncThunk<
  SentimentScore,
  void,
  { state: RootState }
>("dataCache/fetchSentimentScore", async (_, { getState }) => {
  const cached = getState().dataCache.sentimentScore;
  if (cached.data !== null) return cached.data;
  return await pullSentimentScore();
});

export const fetchCurrencies = createAsyncThunk<
  CurrencyCode[],
  void,
  { state: RootState }
>("dataCache/fetchCurrencies", async (_, { getState }) => {
  const cached = getState().dataCache.currencies;
  if (cached.data) return cached.data;
  return await pullCurrencies();
});

export const fetchExchangeRate = createAsyncThunk<
  { key: string; rate: number },
  { base: CurrencyCode; quote: CurrencyCode },
  { state: RootState }
>("dataCache/fetchExchangeRate", async ({ base, quote }, { getState }) => {
  const key = `${base}-${quote}`;
  const cached = getState().dataCache.exchangeRates;
  if (key in cached.data) return { key, rate: cached.data[key] };
  const rate = await pullExchangeRate(base, quote);
  return { key, rate };
});

export const fetchUsStockIndices = createAsyncThunk<
  UsStockIndex[],
  void,
  { state: RootState }
>("dataCache/fetchUsStockIndices", async (_, { getState }) => {
  const cached = getState().dataCache.usStockIndices;
  if (cached.data) return cached.data;
  return await pullUsStockIndices();
});

const dataCacheSlice = createSlice({
  name: "dataCache",
  initialState,
  reducers: {
    clearDataCache(state) {
      state.aiSkills = createEntry<AISkill[]>();
      state.sentimentScore = createEntry<SentimentScore>();
      state.currencies = createEntry<CurrencyCode[]>();
      state.exchangeRates = { data: {}, loading: false, error: null };
      state.usStockIndices = createEntry<UsStockIndex[]>();
    },
  },
  extraReducers: (builder) => {
    // AI Skills
    builder
      .addCase(fetchAISkills.pending, (state) => {
        if (!state.aiSkills.data) state.aiSkills.loading = true;
      })
      .addCase(fetchAISkills.fulfilled, (state, action) => {
        state.aiSkills.data = action.payload;
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
