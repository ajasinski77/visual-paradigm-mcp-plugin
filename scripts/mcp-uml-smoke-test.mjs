#!/usr/bin/env node

/*
  UML smoke test for Visual Paradigm MCP plugin.
  Covers: Use Case, Class, Sequence, Activity
*/

const endpoint = process.env.MCP_URL || "http://localhost:18080/mcp/messages";
const suffix = new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14);

const names = {
  useCase: `UC_Smoke_${suffix}`,
  class: `Class_Smoke_${suffix}`,
  sequence: `Seq_Smoke_${suffix}`,
  activity: `Act_Smoke_${suffix}`,
  actor: `Customer_${suffix}`,
  useCaseItem: `Place_Order_${suffix}`,
  classA: `Order_${suffix}`,
  classB: `Invoice_${suffix}`,
  lifelineA: `Client_${suffix}`,
  lifelineB: `Server_${suffix}`,
  actionA: `Start_${suffix}`,
  actionB: `Finish_${suffix}`,
};

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

  const client = new Client({ name: "uml-smoke-test", version: "1.0.0" }, { capabilities: {} });
  await client.connect(new StreamableHTTPClientTransport(new URL(endpoint)));

  const calls = [
    { name: "createUseCaseDiagram", arguments: { diagramName: names.useCase } },
    { name: "addActor", arguments: { actorName: names.actor, diagramName: names.useCase } },
    { name: "addUseCase", arguments: { useCaseName: names.useCaseItem, diagramName: names.useCase } },
    {
      name: "addRelationship",
      arguments: {
        diagramName: names.useCase,
        fromElement: names.actor,
        toElement: names.useCaseItem,
        relationshipType: "association",
      },
    },
    { name: "generateReport", arguments: { diagramName: names.useCase } },

    { name: "createClassDiagram", arguments: { diagramName: names.class } },
    { name: "addClass", arguments: { className: names.classA, diagramName: names.class } },
    { name: "addClass", arguments: { className: names.classB, diagramName: names.class } },
    {
      name: "addClassRelationship",
      arguments: {
        diagramName: names.class,
        fromElement: names.classA,
        toElement: names.classB,
        relationshipType: "association",
      },
    },

    { name: "createSequenceDiagram", arguments: { diagramName: names.sequence } },
    { name: "addLifeline", arguments: { lifelineName: names.lifelineA, diagramName: names.sequence } },
    { name: "addLifeline", arguments: { lifelineName: names.lifelineB, diagramName: names.sequence } },
    {
      name: "addMessage",
      arguments: {
        diagramName: names.sequence,
        fromLifeline: names.lifelineA,
        toLifeline: names.lifelineB,
        messageName: "login()",
      },
    },

    { name: "createActivityDiagram", arguments: { diagramName: names.activity } },
    { name: "addAction", arguments: { actionName: names.actionA, diagramName: names.activity } },
    { name: "addAction", arguments: { actionName: names.actionB, diagramName: names.activity } },
    {
      name: "addControlFlow",
      arguments: {
        diagramName: names.activity,
        fromElement: names.actionA,
        toElement: names.actionB,
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
    fail(`UML smoke test finished with ${failures} failure(s).`);
    process.exitCode = 1;
    return;
  }

  ok("UML smoke test passed.");
}

main().catch((err) => {
  fail(err?.stack || err?.message || String(err));
  process.exit(1);
});
