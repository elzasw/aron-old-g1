import React from 'react';

import { BASE_URL } from '../../enums';
import { IconDef } from '..';

interface Props {
  className?: string;
  icon?: IconDef | string;
}

function isIconDef(icon: IconDef | string): icon is IconDef {
  return typeof icon !== "string";
}

export function Icon({
  icon: _icon,
  className,
}: Props) {
  if (!_icon) { return <></>; }

  const {
    icon,
    size,
    color,
    background,
    radius,
    padding = 0
  } = isIconDef(_icon) ? _icon : ({ icon: _icon } as IconDef);

  if (!icon) { return <></>; }

  const isAbsoluteUrl = /(http|https):\/\//g.test(icon || '');
  const isFileName = /.\.[\w\d,\-,_]+$/g.test(icon || '');
  const iconUrl = isAbsoluteUrl
    ? icon
    : isFileName
      ? `${BASE_URL}/app/${icon}`
      : undefined;

  return (
    <span
      className={className}
      style={{
        backgroundColor: background,
        borderRadius: radius ? `${radius}px` : 0,
        display: 'inline-flex',
        padding: `${padding}px`,
        // height: '1em',
        // height: size ? `${size}px` : '1em',
        color,
      }}
    >
      {iconUrl ? (
        <img
          src={iconUrl}
          style={{
            // height: "100px",
            height: size ? `${size}px` : '1em',
          }}
        />
      ) : (
        <span
          style={{
            fontSize: size ? `${size}px` : '1em',
          }}
          className={icon}
        />
      )}
    </span>
  );
}
