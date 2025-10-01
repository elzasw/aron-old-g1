import makeStyles from '@material-ui/core/styles/makeStyles';

import { appHeaderHeight as _appHeaderHeight, compactAppHeaderHeight } from '../../styles';

export interface StyleConfiguration {
    compactAppHeader?: boolean;
}

export const useStyles = (configuration?: StyleConfiguration ) => makeStyles((theme) => {
  const appHeaderHeight = configuration?.compactAppHeader ? compactAppHeaderHeight : _appHeaderHeight;
  return {
  appTitle: {
    fontSize: '1.2rem',
    color: '#fff',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
  },
  appTitleFull: {
    flex: 1,
  },
  appTitleInFooter: {
    width: '100%',
    display: 'flex',
    justifyContent: 'center',

    ['@media (min-width:1400px)']: {
      justifyContent: 'flex-start',
    },
  },
  appTitleFirst: {
    fontWeight: 600,
  },
  appTitleClickable: {
    cursor: 'pointer',
  },
  invertColor: {
    filter: 'invert(1)',
  },
  appTitleLogo: {
    maxHeight: '100%',
    maxWidth: '100%',
    padding: theme.spacing(2),
    paddingLeft: 0,

    [theme.breakpoints.up('sm')]: {
      padding: theme.spacing(3),
      paddingLeft: 0,
    },
  },
  appTitleLogoClickable: {
    cursor: 'pointer',
  },
  appTitleTopImage: {
    maxHeight: appHeaderHeight,
    padding: '0.5rem',
    marginLeft: '0.5rem',
    display: 'none',

    [theme.breakpoints.up('sm')]: {
      maxWidth: '11em',
      display: 'block',
    },
  },
  appTitleDivider: {
    display: 'none',

    [theme.breakpoints.up('sm')]: {
      display: 'block',
    },
  },
}})();
