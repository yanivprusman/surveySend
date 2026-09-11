import "server-only";

/**
 * The one guard on this API.
 *
 * These routes send WhatsApp messages FROM the owner's own number, so an
 * unauthenticated caller is a spam vector wearing his identity. The service
 * listens on 0.0.0.0 — the WireGuard overlay AND the home LAN — so "only my
 * devices can reach it" is not true enough to rely on.
 *
 * A missing token in the environment REFUSES every request. An API that guards
 * nothing because its secret was never set is worse than one that is down: the
 * first looks like it is working.
 */
export function authorize(request: Request): string | null {
  const expected = process.env.SURVEY_SEND_API_TOKEN ?? "";
  if (!expected) return "SURVEY_SEND_API_TOKEN is not set on the server";

  const header = request.headers.get("authorization") ?? "";
  const given = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!given) return "missing bearer token";

  // Constant-time-ish: compare full length always, so a wrong token cannot be
  // narrowed down by how fast it is rejected.
  if (given.length !== expected.length) return "bad token";
  let diff = 0;
  for (let i = 0; i < expected.length; i++)
    diff |= given.charCodeAt(i) ^ expected.charCodeAt(i);
  return diff === 0 ? null : "bad token";
}
