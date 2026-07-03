import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
  pullCurrencies,
  pullExchangeRate,
  pullPopularAISkills,
  pullSentimentScore,
} from "../API/data";

export const fetchAISkills = createAsyncThunk(
  "dataCache/fetchAISkills",
  async (_, { getState }) => {
    const cached = getState().dataCache.aiSkills;
    if (cached.data) return cached.data;
    return await pullPopularAISkills();
  }
);

export const fetchSentimentScore = createAsyncThunk(
  "dataCache/fetchSentimentScore",
  async (_, { getState }) => {
    const cached = getState().dataCache.sentimentScore;
    if (cached.data !== null) return cached.data;
    return await pullSentimentScore();
  }
);

export const fetchCurrencies = createAsyncThunk(
  "dataCache/fetchCurrencies",
  async (_, { getState }) => {
    const cached = getState().dataCache.currencies;
    if (cached.data) return cached.data;
    return await pullCurrencies();
  }
);

export const fetchExchangeRate = createAsyncThunk(
  "dataCache/fetchExchangeRate",
  async ({ base, quote }, { getState }) => {
    const key = `${base}-${quote}`;
    const cached = getState().dataCache.exchangeRates;
    if (key in cached.data) return { key, rate: cached.data[key] };
    const rate = await pullExchangeRate(base, quote);
    return { key, rate };
  }
);

const dataCacheSlice = createSlice({
  name: "dataCache",
  initialState: {
    aiSkills: { data: null, loading: false, error: null },
    sentimentScore: { data: null, loading: false, error: null },
    currencies: { data: null, loading: false, error: null },
    exchangeRates: { data: {}, loading: false, error: null },
  },
  reducers: {
    clearDataCache(state) {
      state.aiSkills = { data: null, loading: false, error: null };
      state.sentimentScore = { data: null, loading: false, error: null };
      state.currencies = { data: null, loading: false, error: null };
      state.exchangeRates = { data: {}, loading: false, error: null };
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
  },
});

export const { clearDataCache } = dataCacheSlice.actions;
export const dataCacheSlice_ = dataCacheSlice;

export const selectAISkills = (s) => s.dataCache.aiSkills;
export const selectSentimentScore = (s) => s.dataCache.sentimentScore;
export const selectCurrencies = (s) => s.dataCache.currencies;
export const selectExchangeRates = (s) => s.dataCache.exchangeRates;
