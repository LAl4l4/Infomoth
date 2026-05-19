import instance from "./axios";

export async function pullCurrencies() {
  const res = await instance.get("/data/currencies");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid currencies response");
  }
  return res.data;
}

export async function pullExchangeRate(base, quote) {
  const res = await instance.get("/data/exchangerate", {
    params: { base, quote },
  });

  if (typeof res.data !== "number") {
    throw new Error("Invalid exchange rate response");
  }
  return res.data;
}

export async function pullPopularAISkills() {
  const res = await instance.get("/data/ai-skills");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid AI skills response");
  }
  return res.data;
}

export async function pullSentimentScore() {
  const res = await instance.get("/data/sentiment");
  if (typeof res.data !== "number") {
    throw new Error("Invalid sentiment response");
  }
  return res.data;
}
