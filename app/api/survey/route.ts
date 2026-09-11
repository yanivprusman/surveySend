import { NextResponse } from "next/server";
import { daemonJson } from "@/lib/daemon";
import { authorize } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * Send one survey, on behalf of the phone.
 *
 * This route decides nothing. It checks who is asking, hands the facts to
 * `d survey`, and returns the daemon's own answer — so the phone, the terminal
 * and any future client are all looking at one implementation of what a survey
 * is. Validation (a real Israeli mobile, a name, what the job was) lives there
 * too, because a rule enforced in two places is a rule that will disagree with
 * itself.
 */
export async function POST(request: Request) {
  const denied = authorize(request);
  if (denied)
    return NextResponse.json({ ok: false, error: denied }, { status: 401 });

  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ ok: false, error: "invalid_json" }, { status: 400 });
  }

  const to = String(body.to ?? "").trim();
  const name = String(body.name ?? "").trim();
  const job = String(body.job ?? "").trim();
  const app = String(body.app ?? "").trim();

  let result: Record<string, unknown>;
  try {
    result = await daemonJson({
      command: "survey",
      to,
      name,
      job,
      ...(app ? { app } : {}),
      // The phone always sends. "Make the link but don't send it" is a terminal
      // affordance for when the bridge is down; on a phone, a link nobody sent
      // is just a to-do he will not come back to.
      noSend: 0,
    });
  } catch (e) {
    return NextResponse.json(
      { ok: false, error: e instanceof Error ? e.message : String(e) },
      { status: 502 },
    );
  }

  // A survey that exists but whose link did not go out is a REAL survey with a
  // problem, not a failure: 200 with sent:false, so the phone can show the link
  // and let him pass it on by hand instead of losing it behind an error screen.
  const created = result.ok === true;
  return NextResponse.json(result, { status: created ? 200 : 400 });
}
