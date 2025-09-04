import React from 'react';

import { BASE_URL } from '../../enums';

interface Props {
  size?: number;
  color?: string;
  className?: string;
  icon?: string;
  background?: string;
  radius?: number;
  padding?: number;
}

export function Icon({
  size,
  color, // supported only by className icons
  icon,
  background,
  radius,
  padding = 0,
  className,
}: Props) {
  if (!icon) {
    return <></>;
  }

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
