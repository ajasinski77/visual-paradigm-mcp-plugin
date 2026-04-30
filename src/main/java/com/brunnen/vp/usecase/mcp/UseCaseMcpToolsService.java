package com.brunnen.vp.usecase.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IActivityDiagramUIModel;
import com.vp.plugin.diagram.IBusinessProcessDiagramUIModel;
import com.vp.plugin.diagram.IClassDiagramUIModel;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IInteractionDiagramUIModel;
import com.vp.plugin.diagram.IUseCaseDiagramUIModel;
import com.vp.plugin.model.IBPEndEvent;
import com.vp.plugin.model.IBPIntermediateEvent;
import com.vp.plugin.model.IBPPool;
import com.vp.plugin.model.IBPSequenceFlow;
import com.vp.plugin.model.IBPStartEvent;
import com.vp.plugin.model.IBPTask;
import com.vp.plugin.model.IBPLane;
import com.vp.plugin.model.IBPMessageFlow;
import com.vp.plugin.model.IActionState;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IClass;
import com.vp.plugin.model.IControlFlow;
import com.vp.plugin.model.IDecisionNode;
import com.vp.plugin.model.IDependency;
import com.vp.plugin.model.IExtend;
import com.vp.plugin.model.IInclude;
import com.vp.plugin.model.IInteractionLifeLine;
import com.vp.plugin.model.IInterface;
import com.vp.plugin.model.IMessage;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IUseCase;
import com.vp.plugin.model.factory.IModelElementFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/** MCP tools service providing Visual Paradigm Use Case operations. */
public class UseCaseMcpToolsService {

  public String createClassDiagram(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              com.vp.plugin.model.IProject project =
                  ApplicationManager.instance().getProjectManager().getProject();
              if (project == null) {
                result.set("Error: no project is open in Visual Paradigm");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IDiagramUIModel diagram = dm.createDiagram("ClassDiagram");
              diagram.setName(diagramName);
              dm.openDiagram(diagram);
              result.set("Created class diagram: " + diagramName);
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addClass(String className, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IClassDiagramUIModel diagram = findClassDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IClass clazz = IModelElementFactory.instance().createClass();
              clazz.setName(className);
              IDiagramElement el = dm.createDiagramElement(diagram, clazz);
              int count = countElements(diagram);
              el.setBounds(120 + (count % 3) * 220, 80 + (count / 3) * 140, 180, 100);
              result.set("Added class '" + className + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addInterface(String interfaceName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IClassDiagramUIModel diagram = findClassDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              int count = countElements(diagram);
              try {
                IInterface iface = IModelElementFactory.instance().createInterface();
                iface.setName(interfaceName);
                IDiagramElement el = dm.createDiagramElement(diagram, iface);
                el.setBounds(120 + (count % 3) * 220, 80 + (count / 3) * 140, 180, 100);
              } catch (Exception interfaceEx) {
                // Fallback: some VP versions fail creating interface shape directly on class diagram.
                IClass fallback = IModelElementFactory.instance().createClass();
                fallback.setName("<<interface>> " + interfaceName);
                IDiagramElement el = dm.createDiagramElement(diagram, fallback);
                el.setBounds(120 + (count % 3) * 220, 80 + (count / 3) * 140, 180, 100);
              }
              result.set("Added interface '" + interfaceName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addClassRelationship(
      String diagramName, String fromName, String toName, String relationshipType) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IClassDiagramUIModel diagram = findClassDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              IDiagramElement fromElement = findDiagramElementByName(diagram, fromName);
              IDiagramElement toElement = findDiagramElementByName(diagram, toName);

              DiagramManager dm = ApplicationManager.instance().getDiagramManager();

              IModelElement from = fromElement != null ? fromElement.getModelElement() : null;
              IModelElement to = toElement != null ? toElement.getModelElement() : null;

              if (from == null) {
                from = findModelElementByName(fromName);
                if (from != null) {
                  fromElement = dm.createDiagramElement(diagram, from);
                }
              }
              if (to == null) {
                to = findModelElementByName(toName);
                if (to != null) {
                  toElement = dm.createDiagramElement(diagram, to);
                }
              }

              if (fromElement == null || toElement == null || from == null || to == null) {
                result.set("Error: source/target element not found on diagram '" + diagramName + "'");
                return;
              }

              String relType =
                  relationshipType == null ? "association" : relationshipType.trim().toLowerCase();
              if ("dependency".equals(relType)) {
                IDependency dependency = IModelElementFactory.instance().createDependency();
                dependency.setFrom(from);
                dependency.setTo(to);
                dm.createConnector(diagram, dependency, fromElement, toElement, null);
              } else {
                IAssociation association = IModelElementFactory.instance().createAssociation();
                association.setFrom(from);
                association.setTo(to);
                dm.createConnector(diagram, association, fromElement, toElement, null);
              }

              result.set(
                  "Added "
                      + relType
                      + " between '"
                      + fromName
                      + "' and '"
                      + toName
                      + "' on diagram '"
                      + diagramName
                      + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String createSequenceDiagram(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              com.vp.plugin.model.IProject project =
                  ApplicationManager.instance().getProjectManager().getProject();
              if (project == null) {
                result.set("Error: no project is open in Visual Paradigm");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IDiagramUIModel diagram = dm.createDiagram("InteractionDiagram");
              diagram.setName(diagramName);
              dm.openDiagram(diagram);
              result.set("Created sequence diagram: " + diagramName);
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addLifeline(String lifelineName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IInteractionDiagramUIModel diagram = findInteractionDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IInteractionLifeLine lifeline = IModelElementFactory.instance().createInteractionLifeLine();
              lifeline.setName(lifelineName);
              IDiagramElement el = dm.createDiagramElement(diagram, lifeline);
              int count = countElements(diagram);
              el.setBounds(100 + (count - 1) * 200, 80, 130, 400);
              result.set("Added lifeline '" + lifelineName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addMessage(String diagramName, String fromLifeline, String toLifeline, String messageName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IInteractionDiagramUIModel diagram = findInteractionDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              IDiagramElement fromElement = findDiagramElementByName(diagram, fromLifeline);
              IDiagramElement toElement = findDiagramElementByName(diagram, toLifeline);
              if (fromElement == null || toElement == null) {
                result.set("Error: source/target lifeline not found on diagram '" + diagramName + "'");
                return;
              }

              IMessage message = IModelElementFactory.instance().createMessage();
              message.setName(messageName);
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              dm.createConnector(diagram, message, fromElement, toElement, null);

              result.set(
                  "Added message '"
                      + messageName
                      + "' from '"
                      + fromLifeline
                      + "' to '"
                      + toLifeline
                      + "' on diagram '"
                      + diagramName
                      + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String createActivityDiagram(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              com.vp.plugin.model.IProject project =
                  ApplicationManager.instance().getProjectManager().getProject();
              if (project == null) {
                result.set("Error: no project is open in Visual Paradigm");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IDiagramUIModel diagram = dm.createDiagram("ActivityDiagram");
              diagram.setName(diagramName);
              dm.openDiagram(diagram);
              result.set("Created activity diagram: " + diagramName);
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String createBusinessProcessDiagram(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              com.vp.plugin.model.IProject project =
                  ApplicationManager.instance().getProjectManager().getProject();
              if (project == null) {
                result.set("Error: no project is open in Visual Paradigm");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IDiagramUIModel diagram = dm.createDiagram("BusinessProcessDiagram");
              diagram.setName(diagramName);
              dm.openDiagram(diagram);
              result.set("Created business process diagram: " + diagramName);
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPTask(String taskName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IBPTask task = IModelElementFactory.instance().createBPTask();
              task.setName(taskName);
              IDiagramElement el = dm.createDiagramElement(diagram, task);
              int count = countElements(diagram);
              el.setBounds(140, 90 + (count - 1) * 90, 180, 70);
              result.set("Added BPMN task '" + taskName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPEvent(String eventName, String diagramName, String eventType) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              String type = eventType == null ? "start" : eventType.trim().toLowerCase();
              IModelElement event;
              if ("end".equals(type)) {
                IBPEndEvent end = IModelElementFactory.instance().createBPEndEvent();
                end.setName(eventName);
                event = end;
              } else if ("intermediate".equals(type)) {
                IBPIntermediateEvent intermediate =
                    IModelElementFactory.instance().createBPIntermediateEvent();
                intermediate.setName(eventName);
                event = intermediate;
              } else {
                IBPStartEvent start = IModelElementFactory.instance().createBPStartEvent();
                start.setName(eventName);
                event = start;
                type = "start";
              }
              IDiagramElement el = dm.createDiagramElement(diagram, event);
              int count = countElements(diagram);
              el.setBounds(60, 90 + (count - 1) * 90, 50, 50);
              result.set(
                  "Added BPMN "
                      + type
                      + " event '"
                      + eventName
                      + "' to diagram '"
                      + diagramName
                      + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPGateway(String gatewayName, String diagramName, String gatewayType) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              String type = gatewayType == null ? "xor" : gatewayType.trim().toLowerCase();
              IModelElement gateway;
              try {
                if ("and".equals(type)) {
                  gateway = IModelElementFactory.instance().createBPGatewayAND();
                } else if ("or".equals(type)) {
                  gateway = IModelElementFactory.instance().createBPGatewayOR();
                } else if ("event".equals(type) || "event-based".equals(type)) {
                  gateway = IModelElementFactory.instance().createBPGatewayEventBasedXOR();
                  type = "event";
                } else if ("complex".equals(type)) {
                  gateway = IModelElementFactory.instance().createBPGatewayComplex();
                } else {
                  gateway = IModelElementFactory.instance().createBPGatewayDataBasedXOR();
                  type = "xor";
                }
                gateway.setName(gatewayName);
                IDiagramElement el = dm.createDiagramElement(diagram, gateway);
                int count = countElements(diagram);
                el.setBounds(380, 90 + (count - 1) * 90, 60, 60);
                result.set(
                    "Added BPMN "
                        + type
                        + " gateway '"
                        + gatewayName
                        + "' to diagram '"
                        + diagramName
                        + "'");
              } catch (Exception gatewayEx) {
                IModelElement fallbackGateway = IModelElementFactory.instance().createBPGateway();
                fallbackGateway.setName(gatewayName);
                IDiagramElement el = dm.createDiagramElement(diagram, fallbackGateway);
                int count = countElements(diagram);
                el.setBounds(380, 90 + (count - 1) * 90, 60, 60);
                result.set(
                    "Added BPMN gateway '"
                        + gatewayName
                        + "' to diagram '"
                        + diagramName
                        + "' using fallback type because '"
                        + type
                        + "' creation failed");
              }
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPPool(String poolName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IBPPool pool = IModelElementFactory.instance().createBPPool();
              pool.setName(poolName);
              IDiagramElement el = dm.createDiagramElement(diagram, pool);
              int count = countElements(diagram);
              el.setBounds(40, 60 + (count - 1) * 120, 680, 100);
              result.set("Added BPMN pool '" + poolName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPSequenceFlow(String diagramName, String fromName, String toName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              IDiagramElement fromElement = findDiagramElementByName(diagram, fromName);
              IDiagramElement toElement = findDiagramElementByName(diagram, toName);
              if (fromElement == null || toElement == null) {
                result.set("Error: source/target element not found on diagram '" + diagramName + "'");
                return;
              }

              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IBPSequenceFlow flow = IModelElementFactory.instance().createBPSequenceFlow();
              dm.createConnector(diagram, flow, fromElement, toElement, null);

              result.set(
                  "Added BPMN sequence flow between '"
                      + fromName
                      + "' and '"
                      + toName
                      + "' on diagram '"
                      + diagramName
                      + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPLane(String laneName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IBPLane lane = IModelElementFactory.instance().createBPLane();
              lane.setName(laneName);
              IDiagramElement el = dm.createDiagramElement(diagram, lane);
              int count = countElements(diagram);
              el.setBounds(40, 160 + (count - 1) * 120, 680, 100);
              result.set("Added BPMN lane '" + laneName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addBPMessageFlow(String messageFlowName, String diagramName,
      String fromName, String toName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IBusinessProcessDiagramUIModel diagram = findBusinessProcessDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              IDiagramElement fromElement = findDiagramElementByName(diagram, fromName);
              IDiagramElement toElement = findDiagramElementByName(diagram, toName);
              if (fromElement == null || toElement == null) {
                result.set("Error: source/target element not found on diagram '" + diagramName + "'");
                return;
              }

              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IBPMessageFlow msgFlow = IModelElementFactory.instance().createBPMessageFlow();
              msgFlow.setName(messageFlowName);
              dm.createConnector(diagram, msgFlow, fromElement, toElement, null);

              result.set(
                  "Added BPMN message flow '"
                      + messageFlowName
                      + "' from '"
                      + fromName
                      + "' to '"
                      + toName
                      + "' on diagram '"
                      + diagramName
                      + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addAction(String actionName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IActivityDiagramUIModel diagram = findActivityDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IActionState action = IModelElementFactory.instance().createActionState();
              action.setName(actionName);
              IDiagramElement el = dm.createDiagramElement(diagram, action);
              int count = countElements(diagram);
              el.setBounds(140, 80 + (count - 1) * 100, 180, 60);
              result.set("Added action '" + actionName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addDecisionNode(String nodeName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IActivityDiagramUIModel diagram = findActivityDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              IDecisionNode node = IModelElementFactory.instance().createDecisionNode();
              node.setName(nodeName);
              IDiagramElement el = dm.createDiagramElement(diagram, node);
              int count = countElements(diagram);
              el.setBounds(420, 80 + (count - 1) * 100, 60, 60);
              result.set("Added decision node '" + nodeName + "' to diagram '" + diagramName + "'");
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addControlFlow(String diagramName, String fromName, String toName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              IActivityDiagramUIModel diagram = findActivityDiagram(diagramName);
              if (diagram == null) {
                result.set("Error: diagram '" + diagramName + "' not found");
                return;
              }
              IDiagramElement fromElement = findDiagramElementByName(diagram, fromName);
              IDiagramElement toElement = findDiagramElementByName(diagram, toName);
              if (fromElement == null || toElement == null) {
                result.set("Error: source/target element not found on diagram '" + diagramName + "'");
                return;
              }

              IModelElement from = fromElement.getModelElement();
              IModelElement to = toElement.getModelElement();
              if (from == null || to == null) {
                result.set("Error: cannot resolve model elements for control flow");
                return;
              }

              DiagramManager dm = ApplicationManager.instance().getDiagramManager();
              try {
                IControlFlow flow = IModelElementFactory.instance().createControlFlow();
                flow.setFrom(from);
                flow.setTo(to);
                dm.createConnector(diagram, flow, fromElement, toElement, null);
                result.set(
                    "Added control flow between '"
                        + fromName
                        + "' and '"
                        + toName
                        + "' on diagram '"
                        + diagramName
                        + "'");
              } catch (IllegalArgumentException invalidFlow) {
                // Fallback for node-type restrictions in some VP builds.
                IAssociation fallback = IModelElementFactory.instance().createAssociation();
                fallback.setFrom(from);
                fallback.setTo(to);
                dm.createConnector(diagram, fallback, fromElement, toElement, null);
                result.set(
                    "Added fallback flow (association) between '"
                        + fromName
                        + "' and '"
                        + toName
                        + "' on diagram '"
                        + diagramName
                        + "' because control flow was rejected");
              }
            } catch (Exception ex) {
              result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
          });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String createUseCaseDiagram(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          com.vp.plugin.model.IProject project =
              ApplicationManager.instance().getProjectManager().getProject();
          if (project == null) {
            result.set("Error: no project is open in Visual Paradigm");
            return;
          }
          DiagramManager dm = ApplicationManager.instance().getDiagramManager();
          // Use the string constant directly as the type identifier
          IDiagramUIModel diagram = dm.createDiagram("UseCaseDiagram");
          diagram.setName(diagramName);
          dm.openDiagram(diagram);
          result.set("Created use case diagram: " + diagramName + " (project: " + project.getName() + ")");
        } catch (Exception ex) {
          result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage()
              + " | project=" + (ApplicationManager.instance().getProjectManager().getProject() != null
                  ? ApplicationManager.instance().getProjectManager().getProject().getName() : "null"));
        }
      });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addActor(String actorName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          IUseCaseDiagramUIModel diagram = findUseCaseDiagram(diagramName);
          if (diagram == null) { result.set("Error: diagram '" + diagramName + "' not found"); return; }
          DiagramManager dm = ApplicationManager.instance().getDiagramManager();
          IActor actor = IModelElementFactory.instance().createActor();
          actor.setName(actorName);
          IDiagramElement el = dm.createDiagramElement(diagram, actor);
          int count = countElements(diagram);
          el.setBounds(30, 80 + (count - 1) * 120, 80, 100);
          result.set("Added actor '" + actorName + "' to diagram '" + diagramName + "'");
        } catch (Exception ex) {
          result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
      });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addUseCase(String useCaseName, String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          IUseCaseDiagramUIModel diagram = findUseCaseDiagram(diagramName);
          if (diagram == null) { result.set("Error: diagram '" + diagramName + "' not found"); return; }
          DiagramManager dm = ApplicationManager.instance().getDiagramManager();
          IUseCase useCase = IModelElementFactory.instance().createUseCase();
          useCase.setName(useCaseName);
          IDiagramElement el = dm.createDiagramElement(diagram, useCase);
          int count = countElements(diagram);
          el.setBounds(200, 50 + (count - 1) * 80, 150, 50);
          result.set("Added use case '" + useCaseName + "' to diagram '" + diagramName + "'");
        } catch (Exception ex) {
          result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
      });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String addRelationship(
      String diagramName, String fromName, String toName, String relationshipType) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          IUseCaseDiagramUIModel diagram = findUseCaseDiagram(diagramName);
          if (diagram == null) {
            result.set("Error: diagram '" + diagramName + "' not found");
            return;
          }

          IDiagramElement fromDiagramElement = findDiagramElementByName(diagram, fromName);
          IDiagramElement toDiagramElement = findDiagramElementByName(diagram, toName);
          if (fromDiagramElement == null) {
            result.set("Error: element '" + fromName + "' not found on diagram '" + diagramName + "'");
            return;
          }
          if (toDiagramElement == null) {
            result.set("Error: element '" + toName + "' not found on diagram '" + diagramName + "'");
            return;
          }

          IModelElement from = fromDiagramElement.getModelElement();
          IModelElement to = toDiagramElement.getModelElement();
          if (from == null || to == null) {
            result.set("Error: cannot resolve model elements for relationship");
            return;
          }

          DiagramManager dm = ApplicationManager.instance().getDiagramManager();
          String relType =
              relationshipType == null ? "association" : relationshipType.trim().toLowerCase();

          if ("include".equals(relType)) {
            if (!(from instanceof IUseCase) || !(to instanceof IUseCase)) {
              result.set("Error: include requires both elements to be use cases");
              return;
            }
            IInclude include = IModelElementFactory.instance().createInclude();
            include.setFrom((IUseCase) from);
            include.setTo((IUseCase) to);
            dm.createConnector(diagram, include, fromDiagramElement, toDiagramElement, null);
          } else if ("extend".equals(relType)) {
            if (!(from instanceof IUseCase) || !(to instanceof IUseCase)) {
              result.set("Error: extend requires both elements to be use cases");
              return;
            }
            IExtend extend = IModelElementFactory.instance().createExtend();
            extend.setFrom((IUseCase) from);
            extend.setTo((IUseCase) to);
            dm.createConnector(diagram, extend, fromDiagramElement, toDiagramElement, null);
          } else {
            IAssociation assoc = IModelElementFactory.instance().createAssociation();
            assoc.setFrom(from);
            assoc.setTo(to);
            dm.createConnector(diagram, assoc, fromDiagramElement, toDiagramElement, null);
          }

          result.set(
              "Added "
                  + relType
                  + " between '"
                  + fromName
                  + "' and '"
                  + toName
                  + "' on diagram '"
                  + diagramName
                  + "'");
        } catch (Exception ex) {
          result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
      });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  public String generateReport(String diagramName) {
    AtomicReference<String> result = new AtomicReference<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          IUseCaseDiagramUIModel diagram = findUseCaseDiagram(diagramName);
          if (diagram == null) { result.set("Error: diagram '" + diagramName + "' not found"); return; }
          StringBuilder sb = new StringBuilder();
          sb.append("Use Case Report: ").append(diagramName).append("\n\n");
          Iterator<?> it = diagram.diagramElementIterator();
          List<String> actors = new ArrayList<>();
          List<String> useCases = new ArrayList<>();
          while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof IDiagramElement) {
              IModelElement me = ((IDiagramElement) obj).getModelElement();
              if (me instanceof IActor) actors.add(me.getName());
              else if (me instanceof IUseCase) useCases.add(me.getName());
            }
          }
          sb.append("Actors: ").append(actors).append("\n");
          sb.append("Use Cases: ").append(useCases).append("\n");
          result.set(sb.toString());
        } catch (Exception ex) {
          result.set("Error (EDT): " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
      });
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return "Error: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
    return result.get();
  }

  private IUseCaseDiagramUIModel findUseCaseDiagram(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) return null;
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) return null;
    for (IDiagramUIModel d : diagrams) {
      if (name.equals(d.getName()) && d instanceof IUseCaseDiagramUIModel) {
        return (IUseCaseDiagramUIModel) d;
      }
    }
    return null;
  }

  private IClassDiagramUIModel findClassDiagram(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) {
      return null;
    }
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) {
      return null;
    }
    for (IDiagramUIModel d : diagrams) {
      if (name.equals(d.getName()) && d instanceof IClassDiagramUIModel) {
        return (IClassDiagramUIModel) d;
      }
    }
    return null;
  }

  private IInteractionDiagramUIModel findInteractionDiagram(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) {
      return null;
    }
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) {
      return null;
    }
    for (IDiagramUIModel d : diagrams) {
      if (name.equals(d.getName()) && d instanceof IInteractionDiagramUIModel) {
        return (IInteractionDiagramUIModel) d;
      }
    }
    return null;
  }

  private IActivityDiagramUIModel findActivityDiagram(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) {
      return null;
    }
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) {
      return null;
    }
    for (IDiagramUIModel d : diagrams) {
      if (name.equals(d.getName()) && d instanceof IActivityDiagramUIModel) {
        return (IActivityDiagramUIModel) d;
      }
    }
    return null;
  }

  private IBusinessProcessDiagramUIModel findBusinessProcessDiagram(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) {
      return null;
    }
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) {
      return null;
    }
    for (IDiagramUIModel d : diagrams) {
      if (name.equals(d.getName()) && d instanceof IBusinessProcessDiagramUIModel) {
        return (IBusinessProcessDiagramUIModel) d;
      }
    }
    return null;
  }

  private IModelElement findModelElementByName(String name) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) return null;
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) return null;
    for (IDiagramUIModel d : diagrams) {
      Iterator<?> elIt = d.diagramElementIterator();
      while (elIt.hasNext()) {
        Object obj = elIt.next();
        if (obj instanceof IDiagramElement) {
          IModelElement me = ((IDiagramElement) obj).getModelElement();
          if (me != null && name.equals(me.getName())) {
            return me;
          }
        }
      }
    }
    return null;
  }

  private IDiagramElement findDiagramElementByName(IUseCaseDiagramUIModel diagram, String name) {
    Iterator<?> elIt = diagram.diagramElementIterator();
    while (elIt.hasNext()) {
      Object obj = elIt.next();
      if (obj instanceof IDiagramElement) {
        IDiagramElement el = (IDiagramElement) obj;
        IModelElement modelElement = el.getModelElement();
        if (modelElement != null && name.equals(modelElement.getName())) {
          return el;
        }
      }
    }
    return null;
  }

  private IDiagramElement findDiagramElementByName(IDiagramUIModel diagram, String name) {
    Iterator<?> elIt = diagram.diagramElementIterator();
    while (elIt.hasNext()) {
      Object obj = elIt.next();
      if (obj instanceof IDiagramElement) {
        IDiagramElement el = (IDiagramElement) obj;
        IModelElement modelElement = el.getModelElement();
        if (modelElement != null) {
          String elementName = modelElement.getName();
          if (name.equals(elementName)
              || ("<<interface>> " + name).equals(elementName)
              || (elementName != null && elementName.endsWith(" " + name))) {
            return el;
          }
        }
      }
    }
    return null;
  }

  private IDiagramUIModel findDiagramContaining(IModelElement element) {
    com.vp.plugin.model.IProject project =
        ApplicationManager.instance().getProjectManager().getProject();
    if (project == null) return null;
    IDiagramUIModel[] diagrams = project.toDiagramArray();
    if (diagrams == null) return null;
    for (IDiagramUIModel d : diagrams) {
      Iterator<?> elIt = d.diagramElementIterator();
      while (elIt.hasNext()) {
        Object obj = elIt.next();
        if (obj instanceof IDiagramElement) {
          IModelElement me = ((IDiagramElement) obj).getModelElement();
          if (me != null && me.equals(element)) {
            return d;
          }
        }
      }
    }
    return null;
  }

  private int countElements(IDiagramUIModel diagram) {
    int count = 0;
    Iterator<?> it = diagram.diagramElementIterator();
    while (it.hasNext()) {
      it.next();
      count++;
    }
    return count;
  }

  public List<JsonObject> getToolDefinitions() {
    List<JsonObject> tools = new ArrayList<>();
    tools.add(toolDef("createClassDiagram", "Create a new class diagram", "diagramName", "string"));
    tools.add(toolDef2("addClass", "Add a class to a class diagram", "className", "diagramName"));
    tools.add(
      toolDef2(
        "addInterface", "Add an interface to a class diagram", "interfaceName", "diagramName"));

    JsonObject classRel = new JsonObject();
    classRel.addProperty("name", "addClassRelationship");
    classRel.addProperty("description", "Add class association/dependency relationship");
    JsonObject classRelSchema = new JsonObject();
    classRelSchema.addProperty("type", "object");
    JsonObject classRelProps = new JsonObject();
    classRelProps.add("diagramName", strProp("Diagram name"));
    classRelProps.add("fromElement", strProp("Source class/interface"));
    classRelProps.add("toElement", strProp("Target class/interface"));
    classRelProps.add("relationshipType", strProp("Relationship type: association|dependency"));
    classRelSchema.add("properties", classRelProps);
    classRelSchema.add(
      "required", required("diagramName", "fromElement", "toElement", "relationshipType"));
    classRel.add("inputSchema", classRelSchema);
    tools.add(classRel);

    tools.add(
      toolDef(
        "createSequenceDiagram",
        "Create a new sequence diagram",
        "diagramName",
        "string"));
    tools.add(toolDef2("addLifeline", "Add a lifeline to a sequence diagram", "lifelineName", "diagramName"));

    JsonObject msg = new JsonObject();
    msg.addProperty("name", "addMessage");
    msg.addProperty("description", "Add message between lifelines on a sequence diagram");
    JsonObject msgSchema = new JsonObject();
    msgSchema.addProperty("type", "object");
    JsonObject msgProps = new JsonObject();
    msgProps.add("diagramName", strProp("Diagram name"));
    msgProps.add("fromLifeline", strProp("Source lifeline name"));
    msgProps.add("toLifeline", strProp("Target lifeline name"));
    msgProps.add("messageName", strProp("Message name"));
    msgSchema.add("properties", msgProps);
    msgSchema.add("required", required("diagramName", "fromLifeline", "toLifeline", "messageName"));
    msg.add("inputSchema", msgSchema);
    tools.add(msg);

    tools.add(
      toolDef(
        "createActivityDiagram",
        "Create a new activity diagram",
        "diagramName",
        "string"));
    tools.add(
      toolDef2("addAction", "Add an action to an activity diagram", "actionName", "diagramName"));
    tools.add(
      toolDef2(
        "addDecisionNode",
        "Add a decision node to an activity diagram",
        "nodeName",
        "diagramName"));
    JsonObject flow = new JsonObject();
    flow.addProperty("name", "addControlFlow");
    flow.addProperty("description", "Add control flow between nodes on an activity diagram");
    JsonObject flowSchema = new JsonObject();
    flowSchema.addProperty("type", "object");
    JsonObject flowProps = new JsonObject();
    flowProps.add("diagramName", strProp("Diagram name"));
    flowProps.add("fromElement", strProp("Source element name"));
    flowProps.add("toElement", strProp("Target element name"));
    flowSchema.add("properties", flowProps);
    flowSchema.add("required", required("diagramName", "fromElement", "toElement"));
    flow.add("inputSchema", flowSchema);
    tools.add(flow);

    tools.add(
      toolDef(
        "createBusinessProcessDiagram",
        "Create a new BPMN business process diagram",
        "diagramName",
        "string"));
    tools.add(
      toolDef2("addBPTask", "Add a BPMN task to a business process diagram", "taskName", "diagramName"));

    JsonObject bpEvent = new JsonObject();
    bpEvent.addProperty("name", "addBPEvent");
    bpEvent.addProperty("description", "Add BPMN event to a business process diagram");
    JsonObject bpEventSchema = new JsonObject();
    bpEventSchema.addProperty("type", "object");
    JsonObject bpEventProps = new JsonObject();
    bpEventProps.add("eventName", strProp("Event name"));
    bpEventProps.add("diagramName", strProp("Diagram name"));
    bpEventProps.add("eventType", strProp("Event type: start|intermediate|end"));
    bpEventSchema.add("properties", bpEventProps);
    bpEventSchema.add("required", required("eventName", "diagramName", "eventType"));
    bpEvent.add("inputSchema", bpEventSchema);
    tools.add(bpEvent);

    JsonObject bpGateway = new JsonObject();
    bpGateway.addProperty("name", "addBPGateway");
    bpGateway.addProperty("description", "Add BPMN gateway to a business process diagram");
    JsonObject bpGatewaySchema = new JsonObject();
    bpGatewaySchema.addProperty("type", "object");
    JsonObject bpGatewayProps = new JsonObject();
    bpGatewayProps.add("gatewayName", strProp("Gateway name"));
    bpGatewayProps.add("diagramName", strProp("Diagram name"));
    bpGatewayProps.add("gatewayType", strProp("Gateway type: xor|and|or|event|complex"));
    bpGatewaySchema.add("properties", bpGatewayProps);
    bpGatewaySchema.add("required", required("gatewayName", "diagramName", "gatewayType"));
    bpGateway.add("inputSchema", bpGatewaySchema);
    tools.add(bpGateway);

    tools.add(
      toolDef2("addBPPool", "Add a BPMN pool to a business process diagram", "poolName", "diagramName"));

    JsonObject bpFlow = new JsonObject();
    bpFlow.addProperty("name", "addBPSequenceFlow");
    bpFlow.addProperty("description", "Add BPMN sequence flow between elements");
    JsonObject bpFlowSchema = new JsonObject();
    bpFlowSchema.addProperty("type", "object");
    JsonObject bpFlowProps = new JsonObject();
    bpFlowProps.add("diagramName", strProp("Diagram name"));
    bpFlowProps.add("fromElement", strProp("Source element name"));
    bpFlowProps.add("toElement", strProp("Target element name"));
    bpFlowSchema.add("properties", bpFlowProps);
    bpFlowSchema.add("required", required("diagramName", "fromElement", "toElement"));
    bpFlow.add("inputSchema", bpFlowSchema);
    tools.add(bpFlow);

    tools.add(
      toolDef2("addBPLane", "Add a BPMN lane to a business process diagram", "laneName", "diagramName"));

    JsonObject bpMsgFlow = new JsonObject();
    bpMsgFlow.addProperty("name", "addBPMessageFlow");
    bpMsgFlow.addProperty("description", "Add BPMN message flow between elements on separate pools");
    JsonObject bpMsgFlowSchema = new JsonObject();
    bpMsgFlowSchema.addProperty("type", "object");
    JsonObject bpMsgFlowProps = new JsonObject();
    bpMsgFlowProps.add("messageFlowName", strProp("Message flow name"));
    bpMsgFlowProps.add("diagramName", strProp("Diagram name"));
    bpMsgFlowProps.add("fromElement", strProp("Source element name"));
    bpMsgFlowProps.add("toElement", strProp("Target element name"));
    bpMsgFlowSchema.add("properties", bpMsgFlowProps);
    bpMsgFlowSchema.add("required", required("messageFlowName", "diagramName", "fromElement", "toElement"));
    bpMsgFlow.add("inputSchema", bpMsgFlowSchema);
    tools.add(bpMsgFlow);

    tools.add(
        toolDef("createUseCaseDiagram", "Create a new use case diagram", "diagramName", "string"));
    tools.add(
        toolDef2("addActor", "Add an actor to the use case diagram", "actorName", "diagramName"));
    tools.add(
        toolDef2("addUseCase", "Add a use case to the diagram", "useCaseName", "diagramName"));

    JsonObject rel = new JsonObject();
    rel.addProperty("name", "addRelationship");
    rel.addProperty("description", "Add association/include/extend relationship on a diagram");
    JsonObject relSchema = new JsonObject();
    relSchema.addProperty("type", "object");
    JsonObject relProps = new JsonObject();
    relProps.add("diagramName", strProp("Diagram name"));
    relProps.add("fromElement", strProp("Source element name (actor or use case)"));
    relProps.add("toElement", strProp("Target element name (actor or use case)"));
    relProps.add("relationshipType", strProp("Relationship type: association|include|extend"));
    relSchema.add("properties", relProps);
    relSchema.add("required", required("diagramName", "fromElement", "toElement", "relationshipType"));
    rel.add("inputSchema", relSchema);
    tools.add(rel);

    tools.add(toolDef("generateReport", "Generate a use case report", "diagramName", "string"));
    return tools;
  }

  public String callTool(String toolName, JsonObject arguments) {
    switch (toolName) {
      case "createClassDiagram":
        return createClassDiagram(arg(arguments, "diagramName"));
      case "addClass":
        return addClass(arg(arguments, "className"), arg(arguments, "diagramName"));
      case "addInterface":
        return addInterface(arg(arguments, "interfaceName"), arg(arguments, "diagramName"));
      case "addClassRelationship":
        return addClassRelationship(
            arg(arguments, "diagramName"),
            arg(arguments, "fromElement"),
            arg(arguments, "toElement"),
            arg(arguments, "relationshipType"));
      case "createSequenceDiagram":
        return createSequenceDiagram(arg(arguments, "diagramName"));
      case "addLifeline":
        return addLifeline(arg(arguments, "lifelineName"), arg(arguments, "diagramName"));
      case "addMessage":
        return addMessage(
            arg(arguments, "diagramName"),
            arg(arguments, "fromLifeline"),
            arg(arguments, "toLifeline"),
            arg(arguments, "messageName"));
      case "createActivityDiagram":
        return createActivityDiagram(arg(arguments, "diagramName"));
      case "createBusinessProcessDiagram":
        return createBusinessProcessDiagram(arg(arguments, "diagramName"));
      case "addBPTask":
        return addBPTask(arg(arguments, "taskName"), arg(arguments, "diagramName"));
      case "addBPEvent":
        return addBPEvent(
            arg(arguments, "eventName"),
            arg(arguments, "diagramName"),
            arg(arguments, "eventType"));
      case "addBPGateway":
        return addBPGateway(
            arg(arguments, "gatewayName"),
            arg(arguments, "diagramName"),
            arg(arguments, "gatewayType"));
      case "addBPPool":
        return addBPPool(arg(arguments, "poolName"), arg(arguments, "diagramName"));
      case "addBPSequenceFlow":
        return addBPSequenceFlow(
            arg(arguments, "diagramName"),
            arg(arguments, "fromElement"),
            arg(arguments, "toElement"));
      case "addBPLane":
        return addBPLane(arg(arguments, "laneName"), arg(arguments, "diagramName"));
      case "addBPMessageFlow":
        return addBPMessageFlow(
            arg(arguments, "messageFlowName"),
            arg(arguments, "diagramName"),
            arg(arguments, "fromElement"),
            arg(arguments, "toElement"));
      case "addAction":
        return addAction(arg(arguments, "actionName"), arg(arguments, "diagramName"));
      case "addDecisionNode":
        return addDecisionNode(arg(arguments, "nodeName"), arg(arguments, "diagramName"));
      case "addControlFlow":
        return addControlFlow(
            arg(arguments, "diagramName"),
            arg(arguments, "fromElement"),
            arg(arguments, "toElement"));
      case "createUseCaseDiagram":
        return createUseCaseDiagram(arg(arguments, "diagramName"));
      case "addActor":
        return addActor(arg(arguments, "actorName"), arg(arguments, "diagramName"));
      case "addUseCase":
        return addUseCase(arg(arguments, "useCaseName"), arg(arguments, "diagramName"));
      case "addRelationship":
        return addRelationship(
            arg(arguments, "diagramName"),
            arg(arguments, "fromElement"),
            arg(arguments, "toElement"),
            arg(arguments, "relationshipType"));
      case "generateReport":
        return generateReport(arg(arguments, "diagramName"));
      default:
        throw new IllegalArgumentException("Unknown tool: " + toolName);
    }
  }

  private static String arg(JsonObject args, String key) {
    return args.has(key) && !args.get(key).isJsonNull() ? args.get(key).getAsString() : "";
  }

  private static JsonObject toolDef(
      String name, String description, String argName, String argType) {
    JsonObject tool = new JsonObject();
    tool.addProperty("name", name);
    tool.addProperty("description", description);
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    JsonObject props = new JsonObject();
    JsonObject param = new JsonObject();
    param.addProperty("type", argType);
    param.addProperty("description", argName);
    props.add(argName, param);
    schema.add("properties", props);
    schema.add("required", required(argName));
    tool.add("inputSchema", schema);
    return tool;
  }

  private static JsonObject toolDef2(String name, String description, String a, String b) {
    JsonObject tool = new JsonObject();
    tool.addProperty("name", name);
    tool.addProperty("description", description);
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    JsonObject props = new JsonObject();
    props.add(a, strProp(a));
    props.add(b, strProp(b));
    schema.add("properties", props);
    schema.add("required", required(a, b));
    tool.add("inputSchema", schema);
    return tool;
  }

  private static JsonObject strProp(String description) {
    JsonObject prop = new JsonObject();
    prop.addProperty("type", "string");
    prop.addProperty("description", description);
    return prop;
  }

  private static JsonArray required(String... names) {
    JsonArray arr = new JsonArray();
    for (String n : names) {
      arr.add(n);
    }
    return arr;
  }
}
