import { useCallback, useEffect, useLayoutEffect, useRef } from 'react';

interface KeyDefinition {
  key: string;
  ctrlKey?: boolean;
  shiftKey?: boolean;
  altKey?: boolean;
  metaKey?: boolean;
}

export const useKeyPress = (keys: KeyDefinition[], callback: (event: KeyboardEvent) => void, node: HTMLElement | Document | null = document) => {
  // implement the callback ref pattern
  const callbackRef = useRef(callback);
  useLayoutEffect(() => {
    callbackRef.current = callback;
  });

  // handle what happens on key press
  const handleKeyPress: any = useCallback(
    (event: KeyboardEvent) => {
      // check if one of the key is part of the ones we want
      if (keys.some((key) => {
        return event.key === key.key
          && event.altKey === (key.altKey || false)
          && event.ctrlKey === (key.ctrlKey || false)
          && event.metaKey === (key.metaKey || false)
          && event.shiftKey === (key.shiftKey || false)
      })) {
        callbackRef.current(event);
      }
    },
    [keys]
  );

  useEffect(() => {
    // attach the event listener
    if (node) {
      node.addEventListener("keydown", handleKeyPress);
    }

    // remove the event listener
    return () => {
      if (node) {
        node.removeEventListener("keydown", handleKeyPress);
      }
    }
  }, [handleKeyPress, node]);
};
