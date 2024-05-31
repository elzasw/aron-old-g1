import React, { useContext, useEffect, useState } from 'react';
import classNames from 'classnames';
import { uniq } from 'lodash';
import { Resizable } from 're-resizable';
import CircularProgress from '@material-ui/core/CircularProgress';
import MoreHorizIcon from '@material-ui/icons/MoreHoriz';
import UnfoldLessIcon from '@material-ui/icons/UnfoldLess';

import { NavigationContext } from '@eas/common-web';

import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { DetailTreeProps } from './types';
import { useAppState, useGet } from '../../common-utils';
import { Tree, useConfiguration } from '../../components';
import { ModulePath, ApiUrl } from '../../enums';
import { ApuEntity, ApuTree } from '../../types';

const WRAPPER_ID = 'evidence-detail-tree-wrapper';

const getParentId = (item: ApuEntity): string => {
  return `${item.parent ? `${getParentId(item.parent)}__` : ''}${item.id}`;
};

export function EvidenceDetailTree({ item, id, verticalResize = true }: DetailTreeProps) {
  const { treeHorizontalScroll } = useConfiguration();
  const classes = useStyles({ treeHorizontalScroll });
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();

  const { navigate } = useContext(NavigationContext);

  const { appState, updateAppState } = useAppState();

  const [treeItem, loading] = useGet<ApuTree>(`${ApiUrl.APU}/${id}/tree`);

  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    setSelected([getParentId(item)]);
  }, [item]);

  useEffect(() => {
    if (treeItem && !loading) {
      const init = () => {
        const itemExists = (
          item: ApuTree | undefined,
          id: string,
          parentId?: string
        ): boolean => {
          if (item) {
            const itemId = `${parentId ? `${parentId}__` : ''}${item.id}`;

            return !!(
              itemId === id ||
              (id.match(itemId) &&
                item.children &&
                item.children.some((item) => itemExists(item, id, itemId)))
            );
          }

          return false;
        };

        const items = (
          appState.evidenceDetailTreeExpandedItems || []
        ).filter((id) => itemExists(treeItem, id));

        let current: ApuEntity | undefined = item;

        while (current) {
          items.push(getParentId(current));
          current = current.parent;
        }

        updateAppState({ evidenceDetailTreeExpandedItems: uniq(items) });

        setTimeout(() => {
          const itemElement = document.getElementById(item.id);
          const wrapper = document.getElementById(WRAPPER_ID);

          if (itemElement && wrapper) {
            wrapper.scrollTop =
              itemElement.getBoundingClientRect().top -
              wrapper.getBoundingClientRect().top;
          }
        }, 500);
      };

      setTimeout(init);
    }

    // eslint-disable-next-line
  }, [treeItem, loading]);

  return treeItem ? (
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
          <Tree
            {...{
              items: [treeItem],
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
  ) : loading ? (
    <div
      className={classNames(layoutClasses.flexCentered, spacingClasses.padding)}
    >
      <CircularProgress />
    </div>
  ) : (
        <></>
      );
}
