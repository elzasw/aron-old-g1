import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  // colorBlueVeryLight,
  appHeaderHeight as _appHeaderHeight,
  compactAppHeaderHeight,
} from '../../styles';

export interface StyleConfiguration {
  compactAppHeader?: boolean;
}

export const useStyles = (configuration?: StyleConfiguration) =>
  makeStyles((theme) => {
    const appHeaderHeight = configuration?.compactAppHeader
      ? compactAppHeaderHeight
      : _appHeaderHeight;
    return {
      main: {
        position: 'relative',
        // background: colorBlueVeryLight,
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
        pointerEvents: 'none',
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
      mainBodyInnerWide: {
        width: '100%',
        maxWidth: 'calc(800px - 1rem)',
        padding: '0 1rem',
        [theme.breakpoints.up('sm')]: {
          maxWidth: 'calc(800px - 1rem)',
        },
        [theme.breakpoints.up('md')]: {
          maxWidth: 'calc(1000px - 1rem)',
        },
      },
      mainFavourite: {
        width: '100%',
      },
      mainFavouriteGrid: {
        width: '100%',
        display: "grid",
        gridAutoFlow: "dense",
        gridTemplateColumns: "repeat(2, 1fr)",
        gridAutoRows: "minmax(120px, auto)",
        [theme.breakpoints.up('sm')]: {
          gridTemplateColumns: "repeat(4, 1fr)",
        },
        [theme.breakpoints.up('md')]: {
          gridTemplateColumns: "repeat(6, 1fr)",
        },
        // gridTemplateColumns: "150px 150px 150px 150px 150px ",
        gap: `${theme.spacing(1)}px`,
      },
      mainFavouriteItem: {
        width: '100%',
        cursor: 'pointer',
        color: theme.palette.text.primary,
        display: 'flex',

        [theme.breakpoints.up('md')]: {
          width: '50%',
        },

        '&:hover': {
          color: theme.palette.primary.main,
        },
      },
      mainFavouriteItemAlt: {
        width: '100%',
        cursor: 'pointer',
        background: theme.palette.primary.main,
        borderRadius: theme.spacing(0.8),
        margin: theme.spacing(0.5),
        padding: theme.spacing(0.8),
        color: theme.palette.primary.contrastText,
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        textDecoration: 'none',

        [theme.breakpoints.up('md')]: {
          width: 'auto',
        },

        '&:hover': {
          background: theme.palette.primary.light,
        },
      },
      mainFavouriteItemGrid: {
        width: '100%',
        // height: "100px",
        cursor: 'pointer',
        background: theme.palette.primary.main,
        borderRadius: theme.spacing(0.8),
        margin: 0,
        // padding: theme.spacing(0.8),
        color: theme.palette.primary.contrastText,
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'flex-start',
        position: 'relative',
        padding: theme.spacing(2),
        transition: 'background 450ms cubic-bezier(.23,1,.32,1)',
        overflow: 'hidden',
        textDecoration: 'none',
        // boxShadow: '2px 2px 2px #0004',

        [theme.breakpoints.up('md')]: {
          // width: 'auto',
        },

        '&:hover': {
          background: theme.palette.primary.dark,
          zIndex: '10',
          // transform: 'scale(1.05)',
          // boxShadow: '8px 8px 16px -5px #0004',

          '& $mainFavouriteItemGridBackground': {
            // transform: 'scale(1.1)',
            transform: 'scale(1.05)',
            // opacity: 0.5,
            opacity: 0.8,
            filter: 'saturate(1) brightness(1)',
            // filter: 'saturate(1) brightness(1) contrast(0.6) blur(3px)',
          },
          '& $mainFavouriteIconGrid': {
            // transform: 'scale(1.1)',
            transform: 'scale(1.05)',
            opacity: 0.5
          }
        },
      },
      mainFavouriteItemGridBackground: {
        backgroundSize: 'cover',
        position: 'absolute',
        background: theme.palette.primary.main,
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        transition: 'transform 1050ms cubic-bezier(.23,1,.32,1), opacity 450ms cubic-bezier(.23,1,.32,1), filter 450ms cubic-bezier(.23,1,.32,1)',
        mask: 'linear-gradient(135deg, #fff6, white)',
        backgroundPosition: '50% 50%',
        // filter: 'saturate(0.7) brightness(0.9)',
        filter: 'saturate(1) brightness(1) contrast(0.6) blur(1px)',
        // filter: 'saturate(1) brightness(1) contrast(0.9)',
        opacity: 0.6,
        transform: 'scale(1.1)',
      },
      otherSourcesIcon: {
        fontSize: '1.4em',
        lineHeight: '1em',
        display: 'inline-block',
        paddingRight: '4px',
        marginRight: theme.spacing(1),
      },
      mainFavouriteLabelWrapperGrid: {
        position: "absolute",
        zIndex: 10,
        left: 0,
        paddingRight: theme.spacing(2),
      },
      mainFavouriteLabelGrid: {
        fontSize: '1.2em',
        // textShadow: '0 0 8px #0006',
        // textShadow: `0 0 6px ${theme.palette.primary.dark}`,
        textShadow: `0px 0px 2px ${theme.palette.primary.dark}, 0 0 6px ${theme.palette.primary.dark}`,
        lineHeight: '1.4em',
        zIndex: 100,
        // background: theme.palette.primary.dark,
        boxDecorationBreak: "clone",
        padding: "4px 8px",
        paddingLeft: "16px",
      },
      hiddenSettingsButton: {
        border:'none',
        background: theme.palette.primary.main,
        cursor: 'pointer',
        width: 20,
        height: 20,
        '&:hover': {
          background: theme.palette.primary.light,
        },
      },
      mainFavouriteIcon: {
        color: theme.palette.primary.main,
        fontSize: '1.4em',
        lineHeight: '1em',
        width: '1.5em',
        height: '1em',
        display: 'inline-block',
        marginRight: theme.spacing(0.5),
      },
      mainFavouriteIconAlt: {
        color: theme.palette.primary.contrastText,
        fontSize: '1.4em',
        lineHeight: '1em',
        width: '1.5em',
        height: '1em',
        display: 'inline-block',
        marginRight: theme.spacing(0.5),
      },
      mainFavouriteIconGrid: {
        color: theme.palette.primary.contrastText,
        fontSize: '6.4em',
        lineHeight: '1em',
        width: 'auto',
        height: '1em',
        display: 'inline-block',
        // marginRight: theme.spacing(0.5),
        position: 'absolute',
        opacity: '0.3',
        bottom: '-5px',
        right: '-10px',
        transition: 'transform 450ms cubic-bezier(.23,1,.32,1), opacity 450ms cubic-bezier(.23,1,.32,1)',
        mixBlendMode: 'overlay',
        transform: 'scale(1.1)',
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
        textDecoration: 'none',
      },
    };
  })();
