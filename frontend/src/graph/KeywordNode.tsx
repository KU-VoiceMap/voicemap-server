import { Handle, Position, type Node, type NodeProps } from '@xyflow/react';
import { useGraphInteraction } from './GraphContext';

type KeywordNodeData = {
  label: string;
  connectedCount: number;
};

type KeywordNodeType = Node<KeywordNodeData, 'keyword'>;

export default function KeywordNode({ id, data }: NodeProps<KeywordNodeType>) {
  const { hoveredNodeId, connectedNodeIds, onNodeHover } = useGraphInteraction();

  const isHovered = hoveredNodeId === id;
  const isDimmed = hoveredNodeId !== null && !isHovered && !connectedNodeIds.has(id);

  return (
    <div
      className={`rf-kw-node${isHovered ? ' is-hovered' : ''}${isDimmed ? ' is-dimmed' : ''}`}
      onMouseEnter={() => onNodeHover(id)}
      onMouseLeave={() => onNodeHover(null)}
    >
      <Handle type="target" position={Position.Top} style={{ opacity: 0, top: '50%', left: '50%', transform: 'translate(-50%, -50%)', pointerEvents: 'none' }} />
      <span className="rf-kw-label">{data.label}</span>
      {data.connectedCount > 0 && (
        <span className="rf-kw-badge">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M13.19 8.688a4.5 4.5 0 0 1 1.242 7.244l-4.5 4.5a4.5 4.5 0 0 1-6.364-6.364l1.757-1.757m13.35-.622 1.757-1.757a4.5 4.5 0 0 0-6.364-6.364l-4.5 4.5a4.5 4.5 0 0 0 1.242 7.244" />
          </svg>
          {data.connectedCount}
        </span>
      )}
    </div>
  );
}
