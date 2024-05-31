import { useState, useEffect } from 'react';
import { createUrl } from '../../common-utils';

export interface TreeLevel {
  id: string;
  parentId?: string;
  pos: number;
  depth: number;
  childCnt: number;
  description: string;
  name: string;
}

export enum NodeDirection {
  AFTER = 'after',
  BEFORE = 'before',
  UNDER = 'under',
}

export async function fetchNodes(apuId: string, direction: NodeDirection): Promise<TreeLevel[]> {
  const nodesResponse = await fetch(createUrl(`/apu/${apuId}/related/${direction}`));
  return await nodesResponse.json();
}

export interface BatchNodeResult {
  levels: TreeLevel[];
  id: string;
  direction: NodeDirection;
}


export function useIncrementalTree(apuId: string, initialItems: TreeLevel[] = []): [TreeLevel[], (apuId: string, direction: NodeDirection) => Promise<void>, boolean] {
  const [isLoading, setIsLoading] = useState(false);
  const [treeLevels, setTreeLevels] = useState<TreeLevel[]>(initialItems);

  const initialDataLoad = async () => {
    const promiseQueue: (() => Promise<BatchNodeResult>)[] = [];

    initialItems.forEach(({ id, depth, pos, childCnt, parentId }, index) => {
      const previousItemInLevel = initialItems[index - 1]?.depth === depth ? initialItems[index - 1] : undefined;
      const parentItem = initialItems.find(({ id }) => parentId === id);
      const nextItem = initialItems[index + 1];

      // Disable request for items,
      // that are first in their level/depth
      if (!previousItemInLevel && pos > 1) {
        promiseQueue.push(async () => {
          const direction = NodeDirection.BEFORE;
          const levels = await fetchNodes(id, direction);
          return { levels, id, direction }
        });
      }
      // Request direction under only on items,
      // that have children
      if ((!nextItem || nextItem.depth <= depth) && childCnt > 0) {
        promiseQueue.push(async () => {
          const direction = NodeDirection.UNDER;
          const levels = await fetchNodes(id, direction);
          return { levels, id, direction }
        });
      }
      // Disable request for items,
      // that are last in their level/depth
      if (pos !== parentItem?.childCnt) {
        promiseQueue.push(async () => {
          const direction = NodeDirection.AFTER;
          const levels = await fetchNodes(id, direction);
          return { levels, id, direction }
        });
      }
    })

    const results = await Promise.all(promiseQueue.map((promise) => promise()));

    const newTreeLevels = [...initialItems];
    results.forEach(({ id, levels, direction }) => {
      const index = newTreeLevels.findIndex((level) => level.id === id);
      let indexToPlace: number | undefined = undefined;

      if (direction === NodeDirection.BEFORE) { indexToPlace = index; }
      else if (direction === NodeDirection.AFTER) {
        const { depth } = newTreeLevels[index];

        for (let i = index + 1; i <= newTreeLevels.length; i++) {
          const nextLevel = newTreeLevels[i];

          if (!nextLevel || nextLevel.depth < depth) {
            indexToPlace = i;
            break;
          }

          if (nextLevel.depth > depth) {
            continue;
          }
        }
      }
      else if (direction === NodeDirection.UNDER) {
        indexToPlace = index + 1;
      }

      if (indexToPlace == undefined) { return; }

      newTreeLevels.splice(indexToPlace, 0, ...levels);
    })
    setTreeLevels(newTreeLevels);
  }

  const getRelatedNodes = async (nodeId: string, direction: NodeDirection) => {
    const nodeIndex = treeLevels.findIndex((item) => item.id === nodeId);
    const node = treeLevels[nodeIndex];

    // skip fetch when children are already loaded
    if (direction === NodeDirection.UNDER && node) {
      const potentialChildNode = treeLevels[nodeIndex + 1];
      if (potentialChildNode?.depth > node.depth) {
        return;
      }
    }

    setIsLoading(true);

    const nodes = await fetchNodes(nodeId, direction);

    if (direction === NodeDirection.UNDER) {
      const newTreeLevels = [...treeLevels];
      newTreeLevels.splice(nodeIndex + 1, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    }
    else if (direction === NodeDirection.AFTER) {
      const newTreeLevels = [...treeLevels];
      let nextPossibleIndex = nodeIndex + 1;

      // find the index of the next item, that is in the same or lower depth
      while (newTreeLevels[nextPossibleIndex]?.depth > node.depth) {
        nextPossibleIndex++;
      }

      newTreeLevels.splice(nextPossibleIndex, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    } else if (direction === NodeDirection.BEFORE) {
      const newTreeLevels = [...treeLevels];
      const previousPossibleIndex = nodeIndex;

      newTreeLevels.splice(previousPossibleIndex, 0, ...nodes)
      setTreeLevels(newTreeLevels);
    }
    else {
      setTreeLevels(nodes);
    }

    setIsLoading(false);

    return;
  }

  useEffect(() => {
    initialDataLoad();
  }, [apuId])

  return [treeLevels, getRelatedNodes, isLoading]
}
