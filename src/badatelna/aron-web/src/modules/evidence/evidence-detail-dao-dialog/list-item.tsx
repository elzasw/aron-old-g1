import React from 'react';
import { ListChildComponentProps } from "react-window";
import { Thumbnail } from './thumbnail';

export function ListItem({ index, style, data }: ListChildComponentProps) {
  const item = data.files[index];
  const isActive = data.activeFile && data.activeFile.id === item.id;
  return <div
    key={index}
    style={{ ...style, overflow: "hidden", padding: "3px 8px" }}
  >
    <Thumbnail
      key={index}
      isActive={isActive}
      index={index}
      file={item}
      onClick={data.onClick}
    />
  </div>
}
