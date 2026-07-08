import instance from "./axios";
import type { AISkill, UsStockIndex, CurrencyCode } from "../customTypes";

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

export async function pullSentimentScore(): Promise<number> {
  const res = await instance.get("/data/sentiment");
  if (typeof res.data !== "number") {
    throw new Error("Invalid sentiment response");
  }
  return res.data;
}

export async function pullUsStockIndices(): Promise<UsStockIndex[]> {
  const res = await instance.get("/data/us-stock-indices");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid US stock indices response");
  }
  return res.data as UsStockIndex[];
}
