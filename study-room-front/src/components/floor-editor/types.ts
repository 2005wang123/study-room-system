// src/components/floor-editor/types.ts
import type {
  LayoutAreaShape,
  LayoutPathShape,
  LayoutSeatShape,
  LayoutTextShape,
  LayoutWallShape
} from '@/types/layout'

export type EditorKind = 'area' | 'seat' | 'wall' | 'path' | 'text'
export type EditorTool = 'select' | 'pan' | 'area' | 'seat' | 'wall' | 'path' | 'text'

export type EditableNode = LayoutAreaShape | LayoutSeatShape | LayoutWallShape | LayoutPathShape | LayoutTextShape

export interface SelectedNode {
  kind: EditorKind
  obj: EditableNode
}
