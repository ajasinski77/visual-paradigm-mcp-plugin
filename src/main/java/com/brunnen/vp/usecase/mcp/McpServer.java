package com.brunnen.vp.usecase.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** MCP Server for Visual Paradigm Use Case Plugin (Java 11 compatible). */
public class McpServer {

  private static final int PORT = 18080;
  private static final String PATH = "/mcp/messages";
  private static final String SERVER_NAME = "visual-paradigm-use-case-mcp-server";
  private static final String SERVER_VERSION = "1.0.0";

  private final Gson gson = new Gson();
  private final Map<String, OutputStream> clients = new ConcurrentHashMap<>();
  private final UseCaseMcpToolsService toolsService = new UseCaseMcpToolsService();

  private volatile boolean running;
  private NanoHTTPD httpServer;

  /** Start the MCP server. */
  public synchronized void start() {
    if (running) {
      return;
    }

    httpServer =
        new NanoHTTPD("127.0.0.1", PORT) {
          @Override
          public Response serve(IHTTPSession session) {
            try {
              return handleSession(session);
            } catch (Exception ex) {
              return newFixedLengthResponse(
                  Response.Status.INTERNAL_ERROR, "text/plain", ex.getMessage());
            }
          }
        };

    try {
      httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
      running = true;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to start MCP server", e);
    }
  }

  /** Stop the MCP server. */
  public synchronized void stop() {
    running = false;

    for (Map.Entry<String, OutputStream> entry : clients.entrySet()) {
      try {
        entry.getValue().close();
      } catch (IOException ignored) {
      }
    }
    clients.clear();

    if (httpServer != null) {
      httpServer.stop();
      httpServer = null;
    }
  }

  /** Check if the server is running. */
  public boolean isRunning() {
    return running;
  }

  private Response handleSession(NanoHTTPD.IHTTPSession session) throws Exception {
    if (!PATH.equals(session.getUri())) {
      return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }

    if (Method.GET.equals(session.getMethod())) {
      return handleSseConnect();
    }
    if (Method.POST.equals(session.getMethod())) {
      return handleJsonRpcPost(session);
    }

    return NanoHTTPD.newFixedLengthResponse(
        Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed");
  }

  private Response handleSseConnect() throws IOException {
    String clientId = UUID.randomUUID().toString();
    PipedInputStream in = new PipedInputStream(16 * 1024);
    PipedOutputStream out = new PipedOutputStream(in);
    clients.put(clientId, out);

    // Send priming event with ID for resumable streams (MCP streamable-http spec)
    writeSseEventWithId(out, "message", gson.toJson(new JsonObject()), clientId);

    Thread heartbeat =
        new Thread(
            () -> {
              try {
                while (running && clients.containsKey(clientId)) {
                  writeSseComment(out, "keepalive");
                  Thread.sleep(15000);
                }
              } catch (Exception ignored) {
              } finally {
                clients.remove(clientId);
                try {
                  out.close();
                } catch (IOException ignored) {
                }
              }
            },
            "mcp-sse-heartbeat");
    heartbeat.setDaemon(true);
    heartbeat.start();

    Response resp = NanoHTTPD.newChunkedResponse(Response.Status.OK, "text/event-stream", in);
    resp.addHeader("Cache-Control", "no-cache");
    resp.addHeader("Connection", "keep-alive");
    return resp;
  }

  private Response handleJsonRpcPost(NanoHTTPD.IHTTPSession session) throws Exception {
    Map<String, String> files = new ConcurrentHashMap<>();
    session.parseBody(files);
    String body = files.getOrDefault("postData", "{}");

    JsonObject request = gson.fromJson(body, JsonObject.class);
    JsonElement id = request.has("id") ? request.get("id") : JsonNull.INSTANCE;
    String method = request.has("method") ? request.get("method").getAsString() : "";
    JsonObject params =
        request.has("params") && request.get("params").isJsonObject()
            ? request.getAsJsonObject("params")
            : new JsonObject();

    if ("notifications/initialized".equals(method)) {
      return NanoHTTPD.newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain", "");
    }

    JsonObject response = new JsonObject();
    response.addProperty("jsonrpc", "2.0");
    response.add("id", id);

    try {
      JsonObject result = dispatch(method, params);
      response.add("result", result);
    } catch (Exception ex) {
      JsonObject error = new JsonObject();
      error.addProperty("code", -32603);
      error.addProperty("message", ex.getMessage());
      response.add("error", error);
    }

    // Return JSON-RPC directly for streamable HTTP clients, and also broadcast for SSE clients.
    broadcastJsonRpc(response);
    return NanoHTTPD.newFixedLengthResponse(
        Response.Status.OK, "application/json", gson.toJson(response));
  }

  private JsonObject dispatch(String method, JsonObject params) {
    switch (method) {
      case "initialize":
        return initializeResult();
      case "tools/list":
        return toolsListResult();
      case "tools/call":
        return toolCallResult(params);
      default:
        throw new IllegalArgumentException("Method not found: " + method);
    }
  }

  private JsonObject initializeResult() {
    JsonObject result = new JsonObject();
    result.addProperty("protocolVersion", "2024-11-05");

    JsonObject capabilities = new JsonObject();
    JsonObject tools = new JsonObject();
    tools.addProperty("listChanged", false);
    capabilities.add("tools", tools);
    result.add("capabilities", capabilities);

    JsonObject serverInfo = new JsonObject();
    serverInfo.addProperty("name", SERVER_NAME);
    serverInfo.addProperty("version", SERVER_VERSION);
    result.add("serverInfo", serverInfo);
    return result;
  }

  private JsonObject toolsListResult() {
    JsonObject result = new JsonObject();
    JsonArray tools = new JsonArray();
    for (JsonObject tool : toolsService.getToolDefinitions()) {
      tools.add(tool);
    }
    result.add("tools", tools);
    return result;
  }

  private JsonObject toolCallResult(JsonObject params) {
    String toolName = params.get("name").getAsString();
    JsonObject arguments =
        params.has("arguments") && params.get("arguments").isJsonObject()
            ? params.getAsJsonObject("arguments")
            : new JsonObject();

    String outputText = toolsService.callTool(toolName, arguments);

    JsonObject result = new JsonObject();
    JsonArray content = new JsonArray();
    JsonObject text = new JsonObject();
    text.addProperty("type", "text");
    text.addProperty("text", outputText);
    content.add(text);
    result.add("content", content);
    return result;
  }

  private void broadcastJsonRpc(JsonObject message) {
    String payload = gson.toJson(message);
    for (Map.Entry<String, OutputStream> entry : clients.entrySet()) {
      try {
        writeSseData(entry.getValue(), payload);
      } catch (IOException ex) {
        try {
          entry.getValue().close();
        } catch (IOException ignored) {
        }
        clients.remove(entry.getKey());
      }
    }
  }

  private static void writeSseEvent(OutputStream out, String event, String data)
      throws IOException {
    String payload = "event: " + event + "\n" + "data: " + data + "\n\n";
    out.write(payload.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private static void writeSseData(OutputStream out, String json) throws IOException {
    String payload = "data: " + json + "\n\n";
    out.write(payload.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private static void writeSseComment(OutputStream out, String comment) throws IOException {
    String payload = ": " + comment + "\n\n";
    out.write(payload.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }

  private static void writeSseEventWithId(OutputStream out, String event, String data, String id)
      throws IOException {
    String payload = "id: " + id + "\nevent: " + event + "\ndata: " + data + "\n\n";
    out.write(payload.getBytes(StandardCharsets.UTF_8));
    out.flush();
  }
}
