import React from 'react';
import classNames from 'classnames';

import { Body } from './body';
import { Footer } from './footer';
import { useStyles } from './styles';
import { useLayoutStyles } from '../../styles';
import { useConfiguration, BookIcon } from '../../components';
import { Props } from './types';

export const Main = (props: Props) => {
  const { compactAppHeader } = useConfiguration();
  const classes = useStyles({ compactAppHeader });
  const layoutClasses = useLayoutStyles();

  return (
    <div
      className={classNames(classes.main, layoutClasses.flexColumnSpaceBetween)}
    >
      <BookIcon className={classes.mainBackgroundIcon} />
      <Body />
      <Footer {...props} />
    </div>
  );
};
