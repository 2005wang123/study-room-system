// src/components/floor-editor/types.ts
import type {
  LayoutAreaShape,
  LayoutGraphicShape,
  LayoutPathShape,
  LayoutSeatShape,
  LayoutTextShape,
  LayoutWallShape
} from '@/types/layout'

export type EditorKind = 'area' | 'seat' | 'wall' | 'path' | 'text' | 'shape'
export type EditorTool = 'select' | 'pan' | 'area' | 'seat' | 'wall' | 'path' | 'text' | 'shape'

export type EditableNode =
  LayoutAreaShape | LayoutSeatShape | LayoutWallShape | LayoutPathShape | LayoutTextShape | LayoutGraphicShape

export interface SelectedNode {
  kind: EditorKind
  obj: EditableNode
}
