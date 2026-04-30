# Visual Paradigm MCP Plugin

Visual Paradigm plugin z wbudowanym serwerem MCP (Model Context Protocol),
który udostępnia operacje modelowania diagramów przez HTTP/SSE.

## Aktualny stan

- Serwer MCP uruchamia się razem z pluginem Visual Paradigm
- Endpoint: `http://localhost:18080/mcp/messages`
- Transport: Streamable HTTP / SSE
- Obsługiwane diagramy: Use Case, Class, Sequence, Activity, BPMN

## Dostępne narzędzia MCP (24)

### Use Case (5)

- `createUseCaseDiagram(diagramName)`
- `addActor(actorName, diagramName)`
- `addUseCase(useCaseName, diagramName)`
- `addRelationship(diagramName, fromElement, toElement, relationshipType)`
- `generateReport(diagramName)`

### Class (4)

- `createClassDiagram(diagramName)`
- `addClass(className, diagramName)`
- `addInterface(interfaceName, diagramName)`
- `addClassRelationship(diagramName, fromElement, toElement, relationshipType)`

### Sequence (3)

- `createSequenceDiagram(diagramName)`
- `addLifeline(lifelineName, diagramName)`
- `addMessage(diagramName, fromLifeline, toLifeline, messageName)`

### Activity (4)

- `createActivityDiagram(diagramName)`
- `addAction(actionName, diagramName)`
- `addDecisionNode(nodeName, diagramName)`
- `addControlFlow(diagramName, fromElement, toElement)`

### BPMN (8)

- `createBusinessProcessDiagram(diagramName)`
- `addBPTask(taskName, diagramName)`
- `addBPEvent(eventName, diagramName, eventType)`
- `addBPGateway(gatewayName, diagramName, gatewayType)`
- `addBPPool(poolName, diagramName)`
- `addBPLane(laneName, diagramName)`
- `addBPSequenceFlow(diagramName, fromElement, toElement)`
- `addBPMessageFlow(messageFlowName, diagramName, fromElement, toElement)`

## Instalacja i uruchomienie

1. Zbuduj plugin:

```bash
mvn clean package -DskipTests
```

1. Skopiuj JAR do katalogu pluginów Visual Paradigm:

```powershell
$pluginDir = Join-Path $env:APPDATA 'VisualParadigm\plugins\com.brunnen.vp.visual-paradigm-mcp-plugin\lib'
New-Item -ItemType Directory -Force -Path $pluginDir | Out-Null
Copy-Item '.\target\visual-paradigm-mcp-plugin.jar' (Join-Path $pluginDir 'visual-paradigm-mcp-plugin.jar') -Force
```

1. Zrestartuj Visual Paradigm.

## Szybki test połączenia MCP

Sprawdź nasłuch:

```powershell
netstat -ano | findstr ':18080'
```

## Smoke test BPMN (1 komenda)

Uruchom gotowy test narzędzi BPMN:

```powershell
node .\scripts\mcp-bpmn-smoke-test.mjs
```

## Smoke test UML (1 komenda)

Uruchom gotowy test narzędzi UML (Use Case, Class, Sequence, Activity):

```powershell
node .\scripts\mcp-uml-smoke-test.mjs
```

## All-smoke (UML + BPMN)

Uruchom pełny test regresji MCP w jednym przebiegu:

```powershell
node .\scripts\mcp-all-smoke-test.mjs
```

Opcjonalnie możesz wskazać własny endpoint MCP:

```powershell
$env:MCP_URL = 'http://localhost:18080/mcp/messages'
node .\scripts\mcp-bpmn-smoke-test.mjs
```

## Uwaga BPMN

W niektórych buildach VP konkretne typy gateway (np. XOR) mogą zwracać błąd
NPE podczas tworzenia. Plugin ma fallback do ogólnego `createBPGateway()`,
żeby zachować działanie narzędzi i przepływów.

## Troubleshooting

- Port 18080 nie nasłuchuje:
  - sprawdź, czy Visual Paradigm działa i plugin jest załadowany,
  - zrestartuj Visual Paradigm po podmianie JAR.
- Błąd połączenia MCP z klienta Node:
  - upewnij się, że środowisko ma dostęp do `@modelcontextprotocol/sdk`,
  - potwierdź URL endpointu (`/mcp/messages`).
- Diagram tworzy się, ale nie ma części elementów BPMN:
  - dla gateway użyj fallbacku (plugin robi to automatycznie),
  - uruchom smoke test i sprawdź pierwszy krok, który zwraca `Error:`.
- Build Maven nie działa na Windows:
  - użyj ścieżki i builda przez WSL (sprawdzone w tym repo).

## Rozwój

- Build i packaging: Maven (`pom.xml`)
- Konfiguracja serwera MCP: `src/main/resources/application-mcp.properties`
- Główna logika narzędzi: `src/main/java/com/brunnen/vp/usecase/mcp/UseCaseMcpToolsService.java`
