import React, { useEffect, useRef } from 'react';
import { useStyles } from './styles';
import { FileObject } from './types';
import { FixedSizeList } from "react-window";
import AutoSizer from "react-virtualized-auto-sizer"
import { ListItem } from './list-item';

export function ImageList({
  activeFile,
  files,
  label,
  onClick = () => console.error('"onClick" not defined in ImageList'),
}: {
  activeFile: FileObject;
  files: FileObject[];
  label?: string;
  onClick?: (file: FileObject) => void;
}) {
  const listRef = useRef<FixedSizeList>(null);
  const classes = useStyles();

  useEffect(() => {
    const activeFileIndex = files.findIndex(({ id }) => id === activeFile.id);
    listRef?.current?.scrollToItem(activeFileIndex, "auto");
  }, [activeFile])

  return <div
    key={`${label}`}
    className={classes.daoDialogSectionPart}
  >
    <div
      className={classes.daoDialogSectionPartContent}
    >
      <AutoSizer>
        {({ width, height }) => (
          <FixedSizeList
            ref={listRef}
            width={width}
            height={height}
            itemCount={files.length}
            itemSize={140}
            overscanCount={2}
            itemData={{
              files, activeFile, onClick
            }}
          >
            {ListItem}
          </FixedSizeList>
        )}
      </AutoSizer>
    </div>
  </div>
}
