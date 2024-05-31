import React, { useState, useContext, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import classNames from 'classnames';
import IconButton from '@material-ui/core/IconButton';
import MenuIcon from '@material-ui/icons/Menu';

import { LocaleContext, LocaleName } from '@eas/common-web';

import { getAppHeaderItems } from '../../enums';
import { AppTitle } from '../app-title';
import { useStyles } from './styles';
import { useLayoutStyles, useSpacingStyles } from '../../styles';
import { Language } from '../language';
import { Props } from './types';
import { useConfiguration } from '../configuration'

export function AppHeader({ pageTemplate, ...props }: Props) {
  const configuration = useConfiguration();
  const classes = useStyles({compactAppHeader: configuration.compactAppHeader});
  const layoutClasses = useLayoutStyles();
  const spacingClasses = useSpacingStyles();
  const location = useLocation();
  const [open, setOpen] = useState(false);

  const { switchLocale } = useContext(LocaleContext);

  const localizations = pageTemplate?.localizations;

  useEffect(() => {
    if (
      localizations &&
      localizations.length === 1 &&
      typeof localizations[0] === 'string'
    ) {
      switchLocale(
        localizations[0] === 'cs_CZ' ? LocaleName.cs : LocaleName.en
      );
    }
  }, [localizations, switchLocale]);

  const showLanguages = localizations && localizations.length > 1 && !configuration.localeCookieName;

  return (
    <header className={classes.appHeader}>
      <div
        className={classNames(
          classes.appHeaderInner,
          layoutClasses.flexSpaceBetween
        )}
      >
        <AppTitle appName={pageTemplate?.name} {...props} />
        <div className={classes.appHeaderItems}>
          {getAppHeaderItems(configuration).map(({ path, label, url }, i) =>
            !path ? (
              <a
                key={i}
                href={url}
                target="blank"
                className={classNames(
                  classes.appHeaderItem,
                  layoutClasses.flexCentered,
                  spacingClasses.paddingHorizontal
                )}
              >
                {label}
              </a>
            ) : (
              <Link
                key={path}
                to={{
                  pathname: path,
                }}
                className={classNames(
                  classes.appHeaderItem,
                  layoutClasses.flexCentered,
                  spacingClasses.paddingHorizontal,
                  location.pathname === path && classes.appHeaderItemActive
                )}
              >
                {label}
              </Link>
            )
          )}
          {showLanguages && 
             <Language 
               localizations={pageTemplate?.localizations} 
               className={spacingClasses.marginLeft} 
             />}
        </div>
        <>
          <div className={classNames(classes.mobileMenu, layoutClasses.flex)}>
            <IconButton
              className={classes.toggleMenuButton}
              onClick={() => setOpen((prev) => !prev)}
            >
              <MenuIcon />
            </IconButton>
            {showLanguages && 
              <Language 
                localizations={pageTemplate?.localizations} 
                className={spacingClasses.marginLeft} 
              />}
          </div>
          {open ? (
            <div
              className={classes.appHeaderItemsMobile}
              onClick={() => setOpen(false)}
            >
              {getAppHeaderItems(configuration).map(({ path, label, url }, i) =>
                !path ? (
                  <a
                    key={i}
                    href={url}
                    target="blank"
                    className={classes.appHeaderItemMobile}
                  >
                    {label}
                  </a>
                ) : (
                  <Link
                    key={path}
                    to={{
                      pathname: path,
                    }}
                    className={classes.appHeaderItemMobile}
                  >
                    {label}
                  </Link>
                )
              )}
            </div>
          ) : (
            <></>
          )}
        </>
      </div>
    </header>
  );
}
