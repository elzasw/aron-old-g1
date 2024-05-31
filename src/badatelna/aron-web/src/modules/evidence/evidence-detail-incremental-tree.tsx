import React, { useContext, useEffect, useState } from 'react';
import classNames from 'classnames';
import { uniq } from 'lodash';
import { Resizable } from 're-resizable';
import MoreHorizIcon from '@material-ui/icons/MoreHoriz';
import UnfoldLessIcon from '@material-ui/icons/UnfoldLess';

import { NavigationContext } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailTreeProps } from './types';
import { useAppState } from '../../common-utils';
import { useConfiguration, IncrementalTree } from '../../components';
import { ModulePath } from '../../enums';
import { ApuEntity } from '../../types';
// import { TreeLevel } from '../../components/tree/useIncrementalTree';

const WRAPPER_ID = 'evidence-detail-tree-wrapper';

const getParentId = (item: ApuEntity): string => {
  return `${item.parent ? `${getParentId(item.parent)}__` : ''}${item.id}`;
};

export function EvidenceDetailIncrementalTree({ item, id, verticalResize = true }: DetailTreeProps) {
  const { treeHorizontalScroll } = useConfiguration();
  const classes = useStyles({ treeHorizontalScroll });
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  const { appState, updateAppState } = useAppState();

  const [selected, setSelected] = useState<string[]>([]);
  const initialItems = getInitialItems(item);

  useEffect(() => {
    setSelected([getParentId(item)]);
  }, [item]);

  useEffect(() => {
    const initialExpandedIds = initialItems.map((item) => getParentId(item));
    updateAppState({ evidenceDetailTreeExpandedItems: uniq(initialExpandedIds) });
  }, [])

  function getInitialItems(item: ApuEntity) {
    const parents = [item];
    let parent = item.parent;
    while (parent) {
      parents.push(parent);
      parent = parent.parent;
    }
    return parents.reverse();
  }

  return (
    <Resizable
      size={{
        width: '100%',
        height: verticalResize ? appState.evidenceDetailTreeHeight : '100%',
      }}
      minHeight={50}
      enable={{
        top: false,
        right: false,
        bottom: verticalResize,
        left: false,
        topRight: false,
        bottomRight: false,
        bottomLeft: false,
        topLeft: false,
      }}
      bounds="parent"
      onResizeStop={(_, __, ___, { height }) =>
        updateAppState({
          evidenceDetailTreeHeight: appState.evidenceDetailTreeHeight + height,
        })
      }
      handleComponent={{
        bottom: (
          <div
            className={classNames(
              classes.treeResizeHandle,
              layoutClasses.flexCentered
            )}
          >
            <MoreHorizIcon className={classes.treeResizeHandleIcon} />
          </div>
        ),
      }}
      className={verticalResize ? spacingClasses.marginBottomBig : undefined}
    >
      <div
        id={WRAPPER_ID}
        className={classNames(
          classes.treeWrapper,
          spacingClasses.paddingBottomSmall
        )}
      >
        <div className={layoutClasses.flex}>
          <UnfoldLessIcon
            className={classes.treeCollapseIcon}
            onClick={() =>
              updateAppState({ evidenceDetailTreeExpandedItems: [] })
            }
          />
          <IncrementalTree
            {...{
              apuId: id,
              initialItems,
              selected,
              expanded: appState.evidenceDetailTreeExpandedItems,
              disableClick: item,
              labelMapper: (item) => item.description || item.name || 'Neznámé',
              onLabelClick: (newItem) => {
                if (item.id !== newItem.id) {
                  navigate(`${ModulePath.APU}/${newItem.id}`)
                }
              },
              onNodeToggle: (evidenceDetailTreeExpandedItems) =>
                updateAppState({ evidenceDetailTreeExpandedItems }),
            }}
          />
        </div>
      </div>
    </Resizable>
  )
}
