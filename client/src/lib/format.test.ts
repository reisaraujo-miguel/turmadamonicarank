import { describe, expect, it } from "vitest";

import { formatDate, formatDay } from "./format";

const instant = "2026-11-01T15:34:00Z";

describe("formatDate", () => {
  it("formats the instant in pt-BR and São Paulo time", () => {
    const expected = new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "America/Sao_Paulo",
    }).format(new Date(instant));

    expect(formatDate(instant)).toBe(expected);
  });

  it("returns an empty string for missing values", () => {
    expect(formatDate(null)).toBe("");
    expect(formatDate(undefined)).toBe("");
  });
});

describe("formatDay", () => {
  it("formats only the local day", () => {
    const expected = new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "long",
      timeZone: "America/Sao_Paulo",
    }).format(new Date(instant));

    expect(formatDay(instant)).toBe(expected);
  });

  it("returns an empty string for missing values", () => {
    expect(formatDay(null)).toBe("");
  });
});
