import { useMemo, useCallback, useEffect, useState } from 'react';
import {
  ReactFlow,
  Controls,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import {
  forceSimulation,
  forceLink,
  forceManyBody,
  forceCenter,
  forceCollide,
  type SimulationNodeDatum,
  type SimulationLinkDatum,
} from 'd3-force';
import type { GraphNode, GraphEdge, DocumentDetail } from '../types';
import { GraphInteractionContext } from './GraphContext';
import DocumentNode from './DocumentNode';
import KeywordNode from './KeywordNode';

const nodeTypes = {
  document: DocumentNode,
  keyword: KeywordNode,
};

interface GraphViewProps {
  nodes: GraphNode[];
  edges: GraphEdge[];
  isLoading: boolean;
  error: string | null;
  documentDetails: Map<string, DocumentDetail>;
  onSelectDocument: (documentId: string) => void;
}

interface SimNode extends SimulationNodeDatum {
  id: string;
}

/* ── z-index strategy ──────────────────────────────────────────────
 * zIndexMode="manual" on <ReactFlow> disables the automatic edge
 * elevation that adds node.zIndex to connected edges.
 * We keep nodes at NODE_Z (10) and edges always at EDGE_Z (0)
 * so edges always render below every node.
 * The hovered node is elevated to NODE_Z_HOVERED so its tooltip
 * renders above neighbouring nodes.
 * ─────────────────────────────────────────────────────────────── */
const NODE_Z = 10;
const NODE_Z_HOVERED = 50;
const EDGE_Z = 0;

const EDGE_DEFAULT_STYLE = { stroke: '#dbe2ea', strokeWidth: 1.2 };
const EDGE_HIGHLIGHT_STYLE = { stroke: '#0d8e7b', strokeWidth: 2 };
const EDGE_DIMMED_STYLE = { stroke: '#dbe2ea', strokeWidth: 1, opacity: 0.15 };

function computeLayout(
  graphNodes: GraphNode[],
  graphEdges: GraphEdge[],
  documentDetails: Map<string, DocumentDetail>,
): { nodes: Node[]; edges: Edge[] } {
  const nodeById = new Map(graphNodes.map((n) => [n.id, n]));

  const simNodes: SimNode[] = graphNodes.map((n) => ({ id: n.id }));
  const nodeIdSet = new Set(simNodes.map((n) => n.id));

  const validEdges = graphEdges.filter(
    (e) => nodeIdSet.has(String(e.documentId)) && nodeIdSet.has(String(e.keywordName)),
  );

  const simLinks: SimulationLinkDatum<SimNode>[] = validEdges.map((e) => ({
    source: String(e.documentId),
    target: String(e.keywordName),
  }));

  const simulation = forceSimulation(simNodes)
    .force('link', forceLink<SimNode, SimulationLinkDatum<SimNode>>(simLinks).id((d) => d.id).distance(160))
    .force('charge', forceManyBody().strength(-350))
    .force('center', forceCenter(0, 0))
    .force('collide', forceCollide<SimNode>().radius((d) => {
      const original = nodeById.get(d.id);
      return original?.type === 'DOCUMENT' ? 90 : 45;
    }))
    .stop();

  for (let i = 0; i < 200; i++) {
    simulation.tick();
  }

  const keywordConnectionCounts = new Map<string, number>();
  for (const e of validEdges) {
    const kid = String(e.keywordName);
    keywordConnectionCounts.set(kid, (keywordConnectionCounts.get(kid) ?? 0) + 1);
  }

  const rfNodes: Node[] = simNodes.map((simNode) => {
    const original = nodeById.get(simNode.id)!;
    const isDocument = original.type === 'DOCUMENT';

    if (isDocument) {
      const detail = documentDetails.get(original.id);
      return {
        id: simNode.id,
        type: 'document',
        position: { x: simNode.x ?? 0, y: simNode.y ?? 0 },
        zIndex: NODE_Z,
        data: {
          label: original.title || '문서',
          summary: detail?.summary ?? '',
          createdAt: detail?.createdAt ?? '',
          documentId: original.id,
        },
        draggable: true,
      };
    }

    return {
      id: simNode.id,
      type: 'keyword',
      position: { x: simNode.x ?? 0, y: simNode.y ?? 0 },
      zIndex: NODE_Z,
      data: {
        label: original.name || '키워드',
        connectedCount: keywordConnectionCounts.get(original.id) ?? 0,
      },
      draggable: true,
    };
  });

  const rfEdges: Edge[] = validEdges.map((e, i) => ({
    id: `edge-${i}`,
    source: String(e.documentId),
    target: String(e.keywordName),
    type: 'simplebezier',
    zIndex: EDGE_Z,
    style: EDGE_DEFAULT_STYLE,
    data: { sourceId: String(e.documentId), targetId: String(e.keywordName) },
  }));

  return { nodes: rfNodes, edges: rfEdges };
}

function buildAdjacency(graphEdges: GraphEdge[]): Map<string, Set<string>> {
  const adj = new Map<string, Set<string>>();
  for (const e of graphEdges) {
    const docId = String(e.documentId);
    const kwName = String(e.keywordName);
    if (!adj.has(docId)) adj.set(docId, new Set());
    if (!adj.has(kwName)) adj.set(kwName, new Set());
    adj.get(docId)!.add(kwName);
    adj.get(kwName)!.add(docId);
  }
  return adj;
}

export default function GraphView({
  nodes: graphNodes,
  edges: graphEdges,
  isLoading,
  error,
  documentDetails,
  onSelectDocument,
}: GraphViewProps) {
  const layout = useMemo(() => {
    if (graphNodes.length === 0) return { nodes: [], edges: [] };
    return computeLayout(graphNodes, graphEdges, documentDetails);
  }, [graphNodes, graphEdges, documentDetails]);

  const adjacency = useMemo(() => buildAdjacency(graphEdges), [graphEdges]);

  const [nodes, setNodes, onNodesChange] = useNodesState(layout.nodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(layout.edges);
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);

  useEffect(() => {
    setNodes(layout.nodes);
    setEdges(layout.edges);
  }, [layout, setNodes, setEdges]);

  const connectedNodeIds = useMemo(() => {
    if (!hoveredNodeId) return new Set<string>();
    const neighbors = adjacency.get(hoveredNodeId) ?? new Set<string>();
    return new Set([hoveredNodeId, ...neighbors]);
  }, [hoveredNodeId, adjacency]);

  useEffect(() => {
    setNodes((prev) =>
      prev.map((n) => ({
        ...n,
        zIndex: n.id === hoveredNodeId ? NODE_Z_HOVERED : NODE_Z,
      })),
    );

    setEdges((prev) =>
      prev.map((e) => {
        if (!hoveredNodeId) {
          return { ...e, style: EDGE_DEFAULT_STYLE, animated: false, zIndex: EDGE_Z };
        }

        const edgeData = e.data as { sourceId: string; targetId: string } | undefined;
        const isConnected =
          edgeData != null &&
          (edgeData.sourceId === hoveredNodeId || edgeData.targetId === hoveredNodeId);

        return {
          ...e,
          style: isConnected ? EDGE_HIGHLIGHT_STYLE : EDGE_DIMMED_STYLE,
          animated: isConnected,
          zIndex: EDGE_Z,
        };
      }),
    );
  }, [hoveredNodeId, setNodes, setEdges]);

  const handleNodeHover = useCallback((nodeId: string | null) => {
    setHoveredNodeId(nodeId);
  }, []);

  const handleDocumentClick = useCallback((documentId: string) => {
    onSelectDocument(documentId);
  }, [onSelectDocument]);

  const contextValue = useMemo(() => ({
    hoveredNodeId,
    connectedNodeIds,
    onNodeHover: handleNodeHover,
    onDocumentClick: handleDocumentClick,
  }), [hoveredNodeId, connectedNodeIds, handleNodeHover, handleDocumentClick]);

  if (isLoading) {
    return (
      <div className="graph-view">
        <div className="graph-container">
          <div className="empty-state graph-state is-loading">
            <p>문서 그래프를 불러오는 중입니다.</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="graph-view">
        <div className="graph-container">
          <div className="empty-state graph-state">
            <p>{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (graphNodes.length === 0) {
    return (
      <div className="graph-view">
        <div className="graph-container">
          <div className="empty-state graph-state">
            <p>표시할 문서 그래프가 없습니다.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="graph-view">
      <div className="graph-container">
        <GraphInteractionContext.Provider value={contextValue}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodeTypes={nodeTypes}
            zIndexMode="manual"
            fitView
            fitViewOptions={{ padding: 1.5 }}
            minZoom={0.15}
            maxZoom={2.5}
            proOptions={{ hideAttribution: true }}
            nodesDraggable
            nodesConnectable={false}
            elementsSelectable={false}
          >
            <Controls showInteractive={false} />
          </ReactFlow>
        </GraphInteractionContext.Provider>
      </div>
    </div>
  );
}
