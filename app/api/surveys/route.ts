import { NextResponse } from "next/server";
import { daemonJson } from "@/lib/daemon";
import { authorize } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * What has been asked, and what came back.
 *
 * The other half of the app: a send screen with no sign of what happened next
 * is a form, not a tool. This is what makes it worth opening a second time —
 * he sees whether the customer ever answered, and what he said.
 */
export async function GET(request: Request) {
  const denied = authorize(request);
  if (denied)
    return NextResponse.json({ ok: false, error: denied }, { status: 401 });

  const url = new URL(request.url);
  const status = (url.searchParams.get("status") ?? "all").trim();
  const app = (url.searchParams.get("app") ?? "").trim();

  try {
    const result = await daemonJson({
      command: "surveys",
      status,
      ...(app ? { app } : {}),
      limit: "40",
    });
    return NextResponse.json(result, { status: result.ok === true ? 200 : 400 });
  } catch (e) {
    return NextResponse.json(
      { ok: false, error: e instanceof Error ? e.message : String(e) },
      { status: 502 },
    );
  }
}
