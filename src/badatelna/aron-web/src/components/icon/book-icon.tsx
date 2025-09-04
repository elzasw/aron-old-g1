import React from 'react';

import { Props } from './types';
import BookIconSvg from '../../assets/icons/book.svg';

export function BookIcon({ size = 24, color, ...props }: Props) {
  return (
    <img
      {...props}
      src={BookIconSvg}
      style={{ width: size, height: size, color }}
    />
  );
}
