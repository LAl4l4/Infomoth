import axios from "./axios";

export async function pullCurrencies() {
  const res = await axios.get("/data/currencies");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid currencies response");
  }
  return res.data;
}

export async function pullExchangeRate(base, quote) {
  const res = await axios.get("/data/exchangerate", {
    params: { base, quote },
  });

  if (typeof res.data !== "number") {
    throw new Error("Invalid exchange rate response");
  }
  return res.data;
}

export async function pullPopularAISkills() {
  const res = await axios.get("/data/ai-skills");
  if (!Array.isArray(res.data)) {
    throw new Error("Invalid AI skills response");
  }
  return res.data;
}
