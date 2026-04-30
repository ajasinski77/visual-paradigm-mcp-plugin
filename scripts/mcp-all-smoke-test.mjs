#!/usr/bin/env node

/*
  All smoke test runner for Visual Paradigm MCP plugin.
  Runs UML smoke test and BPMN smoke test sequentially.
*/

import { spawn } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function runNodeScript(scriptPath) {
  return new Promise((resolvePromise, rejectPromise) => {
    const child = spawn(process.execPath, [scriptPath], {
      cwd: process.cwd(),
      env: process.env,
      stdio: "inherit",
    });

    child.on("error", (err) => rejectPromise(err));
    child.on("exit", (code) => {
      if (code === 0) {
        resolvePromise();
      } else {
        rejectPromise(new Error(`${scriptPath} exited with code ${code}`));
      }
    });
  });
}

async function main() {
  const uml = resolve(__dirname, "mcp-uml-smoke-test.mjs");
  const bpmn = resolve(__dirname, "mcp-bpmn-smoke-test.mjs");

  console.log("[RUN] UML smoke test");
  await runNodeScript(uml);
  console.log("[RUN] BPMN smoke test");
  await runNodeScript(bpmn);

  console.log("[OK] All smoke tests passed.");
}

main().catch((err) => {
  console.error(`[FAIL] ${err?.message || String(err)}`);
  process.exit(1);
});
