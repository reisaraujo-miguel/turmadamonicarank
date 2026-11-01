import { describe, expect, it } from "vitest";

import { daysUntil, nextCountLabel } from "./periods";

const cutoff = "2026-12-01T02:59:59.999Z";
const now = new Date("2026-11-01T12:00:00Z");

describe("period countdown", () => {
  it("counts whole days until the cutoff", () => {
    expect(daysUntil(cutoff, now)).toBe(30);
  });

  it("never returns a negative countdown", () => {
    expect(daysUntil("2026-10-01T00:00:00Z", now)).toBe(0);
  });

  it("uses a readable pt-BR label", () => {
    expect(nextCountLabel(cutoff, now)).toBe("A contagem fecha em 30 dias");
    expect(nextCountLabel("2026-11-02T12:00:00Z", now)).toBe("A contagem fecha amanhã");
  });
});
