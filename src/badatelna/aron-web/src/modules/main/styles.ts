import makeStyles from '@material-ui/core/styles/makeStyles';
import { colorBlueVeryLight, appHeaderHeight as _appHeaderHeight, compactAppHeaderHeight } from '../../styles';

export interface StyleConfiguration {
  compactAppHeader?: boolean;
}

export const useStyles = (configuration?: StyleConfiguration) => makeStyles((theme) => {
  const appHeaderHeight = configuration?.compactAppHeader ? compactAppHeaderHeight : _appHeaderHeight;
  return {
    main: {
      position: 'relative',
      background: colorBlueVeryLight,
      minHeight: `calc(100% - ${appHeaderHeight})`,
    },
    mainBackgroundIcon: {
      transform: 'rotate(15deg)',
      position: 'absolute',
      top: '1rem',
      right: '-10vmin',
      opacity: 0.04,
      width: '35vmax !important',
      height: '35vmax !important',
    },
    mainBody: {
      width: '100%',

      '& h1': {
        textAlign: 'center',
        color: theme.palette.primary.main,
      },
    },
    mainBodyInner: {
      width: '100%',
      maxWidth: 'calc(800px - 1rem)',
      padding: '0 0.5rem',
    },
    mainFavourite: {
      width: '100%',

      '& > div, & > a': {
        width: '100%',
        cursor: 'pointer',

        [theme.breakpoints.up('md')]: {
          width: '50%',
        },

        '&:hover': {
          color: theme.palette.primary.main,
        },
      },
    },
    mainFooter: {
      marginTop: 'auto',
      width: '100%',
      background: theme.palette.primary.dark,
      color: '#fff',
    },
    mainFooterInner: {
      display: 'flex',
      flexDirection: 'column',

      ['@media (min-width:1400px)']: {
        flexDirection: 'row',
      },
    },
    mainFooterLeft: {
      minWidth: '100%',

      ['@media (min-width:1400px)']: {
        minWidth: 'calc((100vw - 800px - 64px) / 2)',
      },
    },
    mainFooterSection: {
      ['@media (min-width:1400px)']: {
        maxWidth: 1000,
      },
    },
    mainFooterSections: {
      display: 'flex',
      flexDirection: 'column',
      flex: 1,

      [theme.breakpoints.up('sm')]: {
        flexDirection: 'row',
      },

      '& a': {
        color: '#fff',
      },
    },
    mainFooterTitle: {
      fontSize: '0.8rem',
      textTransform: 'uppercase',
      marginLeft: 0,
    },
    mainFooterText: {
      color: '#fff',
      fontSize: '0.75rem',
      lineHeight: '1.4rem',
      '& a': {
        color: '#fff',
      },
    },
    mainSearchOptions: {
      display: 'flex',
      alignItems: 'center',
      cursor: 'pointer',
      width: 'fit-content',
      marginBottom: theme.spacing(0.5),
      '& :first-child': {
        marginRight: theme.spacing(0.5),
      },
    },
    otherSourceLink: {
      textDecoration: "none",
      color: "currentColor",
      display: "block",
    },
    mainFavouriteIcon: {
      color: theme.palette.primary.main,
      fontSize: "1.4em",
      lineHeight: "1em",
      width: "1.5em",
      height: "1em",
      display: "inline-block"
    },
  };
})();
