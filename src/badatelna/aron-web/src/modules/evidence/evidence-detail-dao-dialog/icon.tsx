import Tooltip from '@material-ui/core/Tooltip';
import classNames from 'classnames';
import React from 'react';
import { useSpacingStyles } from '../../../styles';
import { useStyles } from './styles';
import { IconProps } from './types';

export function ToolbarButton({
  title,
  Component,
  className,
  disabled,
  onClick,
  ...props
}: IconProps) {
  const classes = useStyles();
  const spacingClasses = useSpacingStyles();

  return (
    <Tooltip {...{ key: title, title }}>
      <div>
        <Component
          {...{
            ...props,
            onClick: (e: React.MouseEvent) => !disabled && onClick(e),
            className: classNames(
              classes.daoDialogIcon,
              disabled && classes.daoDialogIconDisabled,
              spacingClasses.marginHorizontalExtraSmall,
              className
            ),
          }}
        />
      </div>
    </Tooltip>
  );
}
