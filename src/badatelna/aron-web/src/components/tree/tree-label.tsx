import React, { useRef, useEffect } from 'react';
import { useConfiguration } from '..';

interface Props {
  isSelected?: boolean;
  value: string;
  debugData?: string;
  firstLoad?: boolean;
}

export function TreeLabel({
  isSelected,
  value,
  debugData,
  firstLoad,
}: Props) {
  const { treeHorizontalScroll } = useConfiguration();
  const labelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isSelected && firstLoad) {
      setTimeout(() => {
        labelRef.current?.scrollIntoView({ block: "center", behavior: "smooth" })
      }, 400)
    }
  }, [isSelected, firstLoad])

  const optionalStyle = !treeHorizontalScroll ? {
    overflow: "hidden",
    textOverflow: "ellipsis",
    width: "calc( 100% + 20px )",
  } : {};

  return <div ref={labelRef} title={value} style={{
    whiteSpace: "nowrap",
    paddingRight: "20px",
    marginLeft: "-4px",
    paddingLeft: "4px",
    ...optionalStyle
  }}>{debugData}{value}</div>;
}
