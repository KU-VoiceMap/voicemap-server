import { Handle, Position, type Node, type NodeProps } from '@xyflow/react';
import { useGraphInteraction } from './GraphContext';
import { formatDateTime } from '../utils/formatDate';

type DocumentNodeData = {
  label: string;
  summary?: string;
  createdAt?: string;
  documentId: string;
};

type DocumentNodeType = Node<DocumentNodeData, 'document'>;

export default function DocumentNode({ id, data }: NodeProps<DocumentNodeType>) {
  const { hoveredNodeId, connectedNodeIds, onNodeHover, onDocumentClick } = useGraphInteraction();

  const isHovered = hoveredNodeId === id;
  const isDimmed = hoveredNodeId !== null && !isHovered && !connectedNodeIds.has(id);
  const formattedDate = data.createdAt ? formatDateTime(data.createdAt) : '';

  return (
    <div
      className={`rf-doc-node${isHovered ? ' is-hovered' : ''}${isDimmed ? ' is-dimmed' : ''}`}
      onMouseEnter={() => onNodeHover(id)}
      onMouseLeave={() => onNodeHover(null)}
      onClick={() => onDocumentClick(data.documentId)}
    >
      <Handle type="source" position={Position.Top} style={{ opacity: 0, top: '50%', left: '50%', transform: 'translate(-50%, -50%)', pointerEvents: 'none' }} />
      <span className="rf-doc-label">{data.label}</span>
      <div className="rf-tooltip">
        <strong className="rf-tooltip-title">{data.label}</strong>
        {data.summary && <p className="rf-tooltip-summary">{data.summary}</p>}
        {formattedDate && <span className="rf-tooltip-date">{formattedDate}</span>}
      </div>
    </div>
  );
}
