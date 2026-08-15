import instance from "./axios";
import type {
  AISkill,
  UsStockIndex,
  CurrencyCode,
  MarketTrendData,
  SentimentScore,
} from "../customTypes";

export async function pullCurrencies(): Promise<CurrencyCode[]> {
  const res = await instance.get("/data/currencies");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid currencies response");
  }
  return res.data as CurrencyCode[];
}

export async function pullExchangeRate(base: CurrencyCode, quote: CurrencyCode): Promise<number> {
  const res = await instance.get("/data/exchangerate", {
    params: { base, quote },
  });

  if (typeof res.data !== "number") {
    throw new Error("Invalid exchange rate response");
  }
  return res.data;
}

export async function pullPopularAISkills(): Promise<AISkill[]> {
  const res = await instance.get("/data/ai-skills");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid AI skills response");
  }
  return res.data as AISkill[];
}

export async function pullSentimentScore(): Promise<SentimentScore> {
  const res = await instance.get("/data/sentiment");
  const validScore = (value: unknown) => value === null
    || (typeof value === "number" && Number.isFinite(value));
  if (!res.data || !validScore(res.data.instant) || !validScore(res.data.dailyAverage)) {
    throw new Error("Invalid sentiment response");
  }
  return res.data as SentimentScore;
}

export async function pullUsStockIndices(): Promise<UsStockIndex[]> {
  const res = await instance.get("/data/us-stock-indices");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid US stock indices response");
  }
  return res.data as UsStockIndex[];
}

export async function pullMarketTrends(): Promise<MarketTrendData> {
  const res = await instance.get("/data/market-trends");
  if (!res.data || !Array.isArray(res.data.points) || !Array.isArray(res.data.correlations)) {
    throw new Error("Invalid market trends response");
  }
  return res.data as MarketTrendData;
}
