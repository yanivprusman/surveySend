import type { NextConfig } from "next";
import path from "node:path";
import fs from "node:fs";

// feedback-lib + launcher live as workspace packages at /opt/automateLinux/ and
// are linked in as `file:` dependencies, so node_modules holds symlinks OUT of
// this project. Turbopack's `root` must contain the project AND those symlink
// targets, or it cannot resolve them and the build dies with "Module not found"
// on routes the scaffold itself generated. /opt/ is the common ancestor.
//
// Fail loud if the workspace is missing — no silent fallback. A build that
// quietly drops feedback-lib is one where the in-app issue reporter is gone and
// nothing said so.
const turbopackRoot = path.resolve(process.cwd(), "../../");
const workspaceRoot = path.resolve(turbopackRoot, "automateLinux");
if (!fs.existsSync(path.join(workspaceRoot, "packages/feedback-lib/package.json"))) {
  throw new Error(
    `feedback-lib workspace guard: expected workspace at ${workspaceRoot} ` +
      `(derived from Turbopack root ${turbopackRoot}). ` +
      `Update the relative path or check the checkout layout.`,
  );
}

const nextConfig: NextConfig = {
  // Next 16 blocks cross-origin /_next/* dev resources unless the host is
  // allowed, which silently breaks client hydration (buttons stop firing).
  allowedDevOrigins: [
    '*.dev.ya-niv.com',
    ...(process.env.ALLOWED_DEV_ORIGINS?.split(',').filter(Boolean) ?? []),
  ],
  turbopack: { root: turbopackRoot },
  // Both packages ship TypeScript source (their package.json `exports` point at
  // .ts files), so they must be compiled by this app rather than consumed as
  // built JS.
  transpilePackages: ['@claudecontrol/feedback-lib', '@addnewfeature/feedback-lib-launcher'],
};

export default nextConfig;
