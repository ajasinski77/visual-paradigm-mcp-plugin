#!/usr/bin/env node

/*
  BPMN smoke test for Visual Paradigm MCP plugin.
  Requires: @modelcontextprotocol/sdk available in Node resolution path.
*/

const endpoint = process.env.MCP_URL || "http://localhost:18080/mcp/messages";
const suffix = new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14);
const diagramName = `BPMN_Smoke_${suffix}`;

function ok(msg) {
  console.log(`[OK] ${msg}`);
}

function fail(msg) {
  console.error(`[FAIL] ${msg}`);
}

async function main() {
  const { Client } = await import("@modelcontextprotocol/sdk/client/index.js");
  const { StreamableHTTPClientTransport } = await import(
    "@modelcontextprotocol/sdk/client/streamableHttp.js"
  );

  const client = new Client({ name: "bpmn-smoke-test", version: "1.0.0" }, { capabilities: {} });
  await client.connect(new StreamableHTTPClientTransport(new URL(endpoint)));

  const calls = [
    { name: "createBusinessProcessDiagram", arguments: { diagramName } },
    { name: "addBPPool", arguments: { poolName: "Supplier", diagramName } },
    { name: "addBPPool", arguments: { poolName: "Buyer", diagramName } },
    { name: "addBPLane", arguments: { laneName: "Operations", diagramName } },
    { name: "addBPEvent", arguments: { eventName: "Start", diagramName, eventType: "start" } },
    { name: "addBPTask", arguments: { taskName: "Send Order", diagramName } },
    { name: "addBPTask", arguments: { taskName: "Process Order", diagramName } },
    { name: "addBPGateway", arguments: { gatewayName: "Approved?", diagramName, gatewayType: "xor" } },
    { name: "addBPEvent", arguments: { eventName: "End", diagramName, eventType: "end" } },
    { name: "addBPSequenceFlow", arguments: { diagramName, fromElement: "Start", toElement: "Send Order" } },
    { name: "addBPSequenceFlow", arguments: { diagramName, fromElement: "Send Order", toElement: "Approved?" } },
    { name: "addBPSequenceFlow", arguments: { diagramName, fromElement: "Approved?", toElement: "End" } },
    {
      name: "addBPMessageFlow",
      arguments: {
        messageFlowName: "Order Message",
        diagramName,
        fromElement: "Send Order",
        toElement: "Process Order",
      },
    },
  ];

  let failures = 0;
  for (const call of calls) {
    try {
      const response = await client.callTool(call);
      const text = response?.content?.[0]?.text || "(no text response)";
      if (text.toLowerCase().startsWith("error")) {
        failures += 1;
        fail(`${call.name}: ${text}`);
      } else {
        ok(`${call.name}: ${text}`);
      }
    } catch (err) {
      failures += 1;
      fail(`${call.name}: ${err?.message || String(err)}`);
    }
  }

  await client.close();

  if (failures > 0) {
    fail(`Smoke test finished with ${failures} failure(s). Diagram: ${diagramName}`);
    process.exitCode = 1;
    return;
  }

  ok(`Smoke test passed. Diagram created: ${diagramName}`);
}

main().catch((err) => {
  fail(err?.stack || err?.message || String(err));
  process.exit(1);
});
