export interface Chat {
  chatId: string;
  title: string;
}

export interface Message {
  id: number;
  role: 'USER' | 'AGENT';
  text: string;
}

export interface DocumentSummary {
  documentId: string;
  chatId: string | null;
  title: string;
  summary: string;
  createdAt: string;
}

export interface DocumentDetail {
  documentId: string;
  chatId: string | null;
  title: string;
  summary: string;
  content: string;
  keywords: string[];
  createdAt: string;
}

export interface GraphNode {
  id: string;
  type: 'DOCUMENT' | 'KEYWORD';
  title?: string;
  name?: string;
}

export interface GraphEdge {
  documentId: string;
  keywordName: string;
}

export type SidebarTab = 'chats' | 'documents' | 'graph';

export type MainView = 'chat' | 'documentList' | 'documentDetail' | 'graph';
