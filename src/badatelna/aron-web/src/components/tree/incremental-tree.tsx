import React from 'react';
import classNames from 'classnames';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import RemoveIcon from '@material-ui/icons/Remove';
import { ArrowUpward, ArrowDownward } from '@material-ui/icons';

import { useStyles } from './styles';
import { useIncrementalTree, TreeLevel, NodeDirection } from './useIncrementalTree';
import { ApuEntity } from '../../types';
import { useConfiguration } from '../configuration';

export interface Props {
  apuId: string;
  className?: string;
  disableClick?: ApuEntity;
  expanded?: string[];
  selected?: string[];
  initialItems?: ApuEntity[];
  labelMapper?: (item: TreeLevel) => string;
  idMapper?: (item: TreeLevel) => string;
  onLabelClick?: (item: any) => void;
  onNodeToggle?: (expanded: string[]) => void;
}

export function IncrementalTree({
  apuId,
  className,
  labelMapper = (item) => item.name,
  idMapper = (item) => item.id,
  onNodeToggle = () => null,
  onLabelClick,
  disableClick,
  initialItems,
  ...props
}: Props) {
  const classes = useStyles();
  const { treeHorizontalScroll } = useConfiguration();
  const [treeItems, getRelatedNodes] = useIncrementalTree(apuId, initialItems?.map((item) => ({ ...item, parentId: item.parent?.id })));

  function renderItemsFromFlat(items: TreeLevel[], parent?: TreeLevel, parentId?: string) {
    return [...items].map((item, index, array) => {
      const childNodes: TreeLevel[] = [];
      for (let i = index + 1; array[i]?.depth > item.depth; i++) {
        childNodes.push(array[i]);
      }

      if (childNodes.length > 0) {
        array.splice(index + 1, childNodes.length);
      }

      const optionalStyle = !treeHorizontalScroll ? {
        overflow: "hidden",
        textOverflow: "ellipsis",
        width: "calc( 100% + 20px )",
      } : {};

      // const debugData = `${item.pos}/${parent?.childCnt} ${parent?.id.split("-")[0]}/${item.id.split("-")[0]} - `;
      const debugData = "";

      const label = <div title={labelMapper(item)} style={{
        whiteSpace: "nowrap",
        paddingRight: "20px",
        ...optionalStyle
      }}>{debugData}{labelMapper(item)}</div>;

      const hasChildren = item.childCnt > 0;

      const nodeId = `${parentId ? `${parentId}__` : ''}${idMapper(item)}`;

      const getLastDirectChild = (item: TreeLevel, items: TreeLevel[]) => {
        const directChildren = getDirectChildren(item, items);
        return directChildren[directChildren.length - 1];
      }

      const getDirectChildren = (item: TreeLevel, items: TreeLevel[]) => {
        const directChildren = items.filter((_item) => _item.depth === item.depth + 1);
        return directChildren;
      }

      return <TreeItem
        {...{
          key: nodeId,
          id: item.id,
          nodeId,
          label,
          icon: hasChildren ? undefined : (
            <RemoveIcon className={classes.endItem} />
          ),
        }}
      >
        {childNodes.length > 0
          && getDirectChildren(item, childNodes).length < item.childCnt
          && childNodes[0].pos > 1
          && <TreeItem
            nodeId={`${childNodes[0].id}_loadBefore`}
            label={<div style={{ display: "inline-flex" }}><ArrowUpward /> <div style={{ marginLeft: 5 }}>Načíst předchozí</div></div>}
          ></TreeItem>
        }

        {childNodes.length > 0
          && renderItemsFromFlat(childNodes, item, nodeId)}

        {childNodes.length === 0
          && item.childCnt > 0
          && <TreeItem
            nodeId={`${nodeId}_loading`}
            label="Načítání..."
            onLabelClick={(e: React.MouseEvent) => { e.preventDefault(); e.stopPropagation(); }}
          ></TreeItem>
        }

        {childNodes.length > 0
          && getDirectChildren(item, childNodes).length < item.childCnt
          && getLastDirectChild(item, childNodes).pos < item.childCnt
          && <TreeItem
            nodeId={`${getLastDirectChild(item, childNodes)?.id}_loadAfter`}
            label={<div style={{ display: "inline-flex" }}><ArrowDownward /> <div style={{ marginLeft: 5 }}>Načíst další</div></div>}
          ></TreeItem>
        }
      </TreeItem>
    }).filter((item) => item);
  }

  function _onNodeToggle(_event: any, expanded: string[]) {
    const lastExpandedId = expanded[0].split("__").reverse()[0];

    getRelatedNodes(lastExpandedId, NodeDirection.UNDER);
    onNodeToggle(expanded);
  }

  return (
    <TreeView
      {...{
        ...props,
        className: classNames(classes.tree, className),
        defaultCollapseIcon: <ExpandMoreIcon />,
        defaultExpandIcon: <ChevronRightIcon />,
        onNodeToggle: _onNodeToggle,
        onNodeSelect: (_event: any, nodePath: any) => {
          if (nodePath.indexOf("loading") >= 0) {
            return;
          }

          if (nodePath.indexOf("loadBefore") >= 0) {
            const [id] = nodePath.split("_");
            getRelatedNodes(id, NodeDirection.BEFORE);
            return;
          } else if (nodePath.indexOf("loadAfter") >= 0) {
            const [id] = nodePath.split("_");
            getRelatedNodes(id, NodeDirection.AFTER);
            return;
          }

          const nodeId = nodePath.split("__").pop();
          if (!disableClick || disableClick.id !== nodeId) {
            onLabelClick?.({
              id: nodeId
            });
          }
        },
      }}
    >
      {renderItemsFromFlat(treeItems)}
    </TreeView>
  );
}
