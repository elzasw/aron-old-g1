import React from 'react';
import classNames from 'classnames';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import RemoveIcon from '@material-ui/icons/Remove';

import { Props } from './types';
import { useStyles } from './styles';
import { useConfiguration } from '../configuration';

export function Tree({
  items,
  className,
  labelMapper = (item) => item.label,
  idMapper = (item) => item.id,
  onNodeToggle = () => null,
  onLabelClick,
  disableClick,
  ...props
}: Props) {
  const classes = useStyles();
  const { treeHorizontalScroll } = useConfiguration();

  const renderItems = (items: any[], parentId?: string) =>
    items.map((item) => {

      const optionalStyle = !treeHorizontalScroll ? {
        overflow: "hidden",
        textOverflow: "ellipsis",
        width: "calc( 100% - 20px )",
      } : {};

      const label = <div title={labelMapper(item)} style={{
        whiteSpace: "nowrap",
        paddingRight: "10px",
        ...optionalStyle
      }}>{labelMapper(item)}</div>;

      const nodeId = `${parentId ? `${parentId}__` : ''}${idMapper(item)}`;
      const { children } = item;

      const hasChildren = children && children.length;

      return (
        <TreeItem
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
          {hasChildren ? renderItems(children, nodeId) : <></>}
        </TreeItem>
      );
    });

  return (
    <TreeView
      {...{
        ...props,
        className: classNames(classes.tree, className),
        defaultCollapseIcon: <ExpandMoreIcon />,
        defaultExpandIcon: <ChevronRightIcon />,
        onNodeToggle: (_, expanded) => onNodeToggle(expanded),
        onNodeSelect: (event: any, nodePath: any) => {
          const nodeId = nodePath.split("__").pop();
          if (!disableClick || disableClick.id !== nodeId) {
            onLabelClick?.({
              id: nodeId
            });
          }
        },
      }}
    >
      {renderItems(items)}
    </TreeView>
  );
}
