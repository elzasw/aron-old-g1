import makeStyles from '@material-ui/core/styles/makeStyles';
import { appHeaderHeight as _appHeaderHeight, compactAppHeaderHeight } from '../../styles';

export interface StyleConfiguration {
    compactAppHeader?: boolean;
}

export const useStyles = (configuration?: StyleConfiguration ) => makeStyles((theme) => {
  const lg = theme.breakpoints.up('lg');
  const appHeaderHeight = configuration?.compactAppHeader ? compactAppHeaderHeight : _appHeaderHeight;
  return {
    appHeader: {
      width: '100%',
      minWidth: '100%',
      height: appHeaderHeight,
      background: theme.palette.primary.dark,
      position: 'sticky',
      top: 0,
      zIndex: 30,
      flexShrink: 0,
      flexGrow: 0,
    },
    appHeaderInner: {
      height: '100%',
      padding: theme.spacing(0, 3),
    },
    appHeaderItems: {
      display: 'none',
      height: '100%',

      [lg]: {
        display: 'flex',
      },
    },
    appHeaderItemsMobile: {
      position: 'absolute',
      top: appHeaderHeight,
      right: 0,
      background: theme.palette.primary.dark,
      borderBottomLeftRadius: 4,
      display: 'flex',
      flexDirection: 'column',
      zIndex: 20,
      [lg]: {
        display: 'none',
      },
    },
    appHeaderItem: {
      color: '#fff',
      textDecoration: 'none',
      cursor: 'pointer',
      fontSize: 14,
      height: '100%',

      '&:hover': {
        background: theme.palette.primary.main,
      },
    },
    appHeaderItemActive: {
      background: theme.palette.primary.main,
    },
    appHeaderItemMobile: {
      padding: '0.8rem 1.2rem',
      textDecoration: 'none',
      color: theme.palette.common.white,

      '&:last-child': {
        borderBottomLeftRadius: 4,
      },

      '&:hover': {
        background: theme.palette.primary.main,
      },
    },
    toggleMenuButton: {
      color: theme.palette.common.white,
    },
    mobileMenu: {
      [lg]: {
        display: 'none',
      },
    },
  };
})();
