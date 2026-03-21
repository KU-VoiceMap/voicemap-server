import { createContext, useContext } from 'react';

export interface GraphInteraction {
  hoveredNodeId: string | null;
  connectedNodeIds: Set<string>;
  onNodeHover: (nodeId: string | null) => void;
  onDocumentClick: (documentId: string) => void;
}

export const GraphInteractionContext = createContext<GraphInteraction>({
  hoveredNodeId: null,
  connectedNodeIds: new Set(),
  onNodeHover: () => { },
  onDocumentClick: () => { },
});

export function useGraphInteraction() {
  return useContext(GraphInteractionContext);
}
