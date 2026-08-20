import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  // appHeaderHeight,
  // breadcrumbsHeight,
  // colorBlueDark,
  // colorBlueVeryLight,
  colorGrey,
  colorGreyLight,
  colorWhite,
  border,
  borderBold,
  // colorBlue,
} from '../../styles';

const sidebarWidth = 340;
const sidebarMinWidth = 30;

export interface StyleConfiguration {
  alternativeItemLabel?: boolean;
  treeHorizontalScroll?: boolean;
}

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  const sm = theme.breakpoints.up('sm');
  return {
    sidebar: {
      position: 'absolute',
      left: 0,
      // top: `calc(${appHeaderHeight} + ${breadcrumbsHeight})`,
      height: '100%',
      width: sidebarMinWidth,
      // background: colorBlueVeryLight,
      background: colorWhite,
      borderRight: border,
      transition: 'width 300ms',
      zIndex: 1,

      [md]: {
        width: sidebarWidth - 20,
        overflowY: 'auto',
        margin: theme.spacing(1),
        borderRadius: '10px',
        borderRight: 'none',
        height: 'calc(100% - 20px)',
        // boxShadow: '0 0 15px #0002',
        boxShadow: theme.shadows[8],
      },
    },
    sidebarContent: {
      width: '100%',
      opacity: 0,
      transition: 'opacity 300ms ease',
      display: 'none',
      [md]: {
        display: 'block',
        opacity: 1,
      },
    },
    sidebarVisible: {
      width: '100%',
      overflowY: 'auto',
      [sm]: {
        width: sidebarWidth,
      },
    },
    sidebarContentVisible: {
      display: 'block',
      opacity: 1,
    },
    visibleButton: {
      width: '100%',
      borderBottom: border,
      cursor: 'pointer',
      display: 'flex',
      justifyContent: 'flex-end',
      [md]: {
        display: 'none',
      },
    },
    sidebarItem: {
      '& h5': {
        color: theme.palette.primary.main,
        marginTop: '2rem',
        marginBottom: '0.75rem',
      },
      '& p': {
        margin: '0.8rem 4px 0',
        color: theme.palette.primary.dark,
        cursor: 'pointer',
        fontSize: 12,

        '&:hover': {
          textDecoration: 'underline',
        },
      },
      '& span': {
        color: colorGreyLight,
      },
    },
    sidebarButton: {
      width: '100%',
      background: theme.palette.primary.main,
      color: theme.palette.primary.contrastText,
      fontWeight: 'bold',

      '&:hover': {
        background: theme.palette.primary.light,
      },
    },
    listContainer: {
      overflowX: 'auto',
      height: '100%',
    },
    list: {
      height: '100%',
      minHeight: '100%',
      width: `calc(100% - ${sidebarMinWidth}px)`,
      marginLeft: sidebarMinWidth,

      [md]: {
        width: `calc(100% - ${sidebarWidth}px)`,
        marginLeft: sidebarWidth,
      },
    },
    listItem: {
      width: '100%',
      borderRadius: 8,
      // padding: `${theme.spacing(3)}px ${theme.spacing(3)}px`,
      cursor: 'pointer',
      display: 'block',
      textDecoration: 'none',
      color: 'currentColor',
      boxShadow: theme.shadows[2],
      background: theme.palette.background.paper,
      transition: 'box-shadow 150ms ease-out, margin 150ms ease-out',
      overflow: 'hidden',

      '&:hover': {
        // boxShadow: 'rgba(0, 0, 0, 0.35) 0px 5px 15px',
        boxShadow: theme.shadows[10],
        // marginTop: -1,
        // marginBottom: 9,
      },

      '& h4': {
        margin: '0 0 0.5rem',
        color: theme.palette.primary.main,
        fontSize: '1.2em',
      },

      '& p': {
        margin: 0,
        whiteSpace: 'pre-wrap',
      },
    },
    listItemIconBackground: {
      background: theme.palette.primary.main,
    },
    listItemTitleOnly: {
      '& h4': {
        margin: 0,
      },
    },
    listEmpty: {
      fontSize: 15,
    },
    listPageNumber: {
      display: 'flex',
      whiteSpace: 'nowrap',
      alignItems: 'center',
    },
    pageSizeSelect: {
      '& > div': {
        minWidth: 40,
      },
      '& div.MuiSelect-select.MuiSelect-select': {
        paddingRight: 2,
      },
    },
    evidenceDetail: {
      overflowX: 'auto',
      height: '100%',
      '& h3': {
        marginTop: 0,
      },
      '& h4': {
        marginTop: 0,
      },
    },
    evidenceDetailSectionTitle: {
      fontWeight: 'bold',
      fontSize: '1rem',
    },
    evidenceDetailDescription: {
      fontWeight: 400,
      whiteSpace: 'pre-wrap',
    },
    evidenceDetailNameDetail: {
      fontWeight: 'normal',
      whiteSpace: 'pre-wrap',
    },
    evidenceDetailItem: {
      fontSize: 13,
    },
    evidenceDetailItemNotFirst: {
      borderTop: border,
    },
    evidenceDetailItemLabel: ({
      alternativeItemLabel,
    }: StyleConfiguration) => ({
      width: 150,
      minWidth: 150,
      textTransform: alternativeItemLabel ? 'none' : 'uppercase',
      textAlign: 'right',
      fontWeight: alternativeItemLabel ? 'bold' : 'normal',
    }),
    evidenceDetailItemLabelBorder: {
      borderRight: border,
    },
    evidenceDetailItemText: {
      marginTop: 0,
      marginBottom: 0,
      whiteSpace: 'pre-wrap',
      display: 'flex',
      flexDirection: 'column',
    },
    evidenceDetailItemExpandButtonWrapper: {
      display: 'flex',
    },
    evidenceDetailItemExpandButton: {
      border: 'none',
      background: 'transparent',
      color: '#616161',
      cursor: 'pointer',
      padding: 0,
    },
    toggleSidebarButtonOpen: {
      transform: 'rotate(180deg)',
      transition: 'transform 500ms ease',
      //alignItem: 'center',
    },
    toggleSidebarButtonClose: {
      transition: 'transform 500ms ease',
      //justifyContent: 'center',
    },

    bold: {
      fontWeight: 600,
    },

    evidenceDetailTop: {
      borderBottom: borderBold,
    },

    evidenceDetailTopIcon: {
      transform: 'rotate(90deg)',
      cursor: 'pointer',
    },

    evidenceDetailTopIconOpen: {
      transform: 'rotate(270deg)',
    },

    evidenceDetailIcon: {
      fontSize: 60,
    },

    findRelatedButton: {
      padding: '4px 8px !important',
    },

    link: {
      cursor: 'pointer',
      textDecoration: 'underline',
      color: 'rgba(0, 0, 0, 0.87)',

      '&:hover': {
        color: 'rgba(0, 0, 0, 0.87)',
      },

      '&:visited': {
        color: 'rgba(0, 0, 0, 0.87)',
      },

      '&:active': {
        color: 'rgba(0, 0, 0, 0.87)',
      },
    },
    archdescRootLink: {
      margin: theme.spacing(0.5),
      padding: `${theme.spacing(0.5)}px ${theme.spacing(1)}px`,
      textDecoration: 'none',
      background: theme.palette.primary.main,
      color: colorWhite,
      fontSize: '0.8rem',
      fontWeight: 'bold',
      borderRadius: '5px',
      display: 'inline-flex',

      '&:hover': {
        background: theme.palette.primary.dark,
        color: theme.palette.primary.contrastText,
      },

      '&:visited': {
        color: theme.palette.primary.contrastText,
      },

      '&:active': {
        color: theme.palette.primary.contrastText,
      },
    },
    searchLink: {
      padding: '10px',
      textDecoration: 'none',
      background: 'transparent',
      fontSize: '1.1rem',
      borderRadius: '5px',
      display: 'inline-flex',
      border: `1px solid ${colorGreyLight}`,

      '&:hover': {
        background: colorGreyLight,
      },
    },
    icon: {
      cursor: 'pointer',
    },

    daoContainer: {
      padding: 10,
      borderBottom: border,
    },
    dao: {},
    daoPreview: {
      width: 150,
      height: 100,
      border,
    },
    daoPreviewImage: {
      width: '100%',
      height: '100%',
      axWidth: '100%',
      maxHeight: '100%',

      '& > img': {
        maxWidth: '100%',
        maxHeight: '100%',
        objectFit: 'contain',
      },
      '& > svg': {
        fontSize: 60,
      },
    },
    daoShowAll: {
      width: 80,
      height: 100,
      border,
      cursor: 'pointer',
      textAlign: 'center',

      '&:hover': {
        border: `1px solid ${colorGrey}`,
      },
    },
    treeWrapper: ({ treeHorizontalScroll }: StyleConfiguration) => ({
      width: '100%',
      // minWidth: 'fit-content',
      height: '100%',
      overflow: 'hidden',
      overflowY: 'auto',
      overflowX: treeHorizontalScroll ? 'auto' : 'hidden',
    }),
    treeResizeHandle: {
      width: '100%',
      background: '#fff',
      borderTop: border,
      borderBottom: border,
    },
    treeResizeHandleIcon: {
      fontSize: 14,
    },
    treeCollapseIcon: {
      cursor: 'pointer',
    },

    evidenceTextInfo: {
      height: '100%',
      maxWidth: '240px',
      whiteSpace: 'normal',
      display: 'flex',
      alignItems: 'center',
      paddingLeft: theme.spacing(1),
      paddingRight: theme.spacing(1),
      textAlign: 'right',

      [theme.breakpoints.up('md')]: {
        whiteSpace: 'nowrap',
        maxWidth: 'none',
      },
    },

    evidenceMainContainer: {
      [md]: {
        width: '1200px',
        position: 'relative',
      },
    },

    evidenceMainWrapper: {
      background: '#f5f5f5',
      display: 'flex',
      justifyContent: 'center',
      height: '100%',
    },

    itemsCount: {
      color: theme.palette.grey[500],
    },

    attachment: {
      border,
      borderRadius: 4,
      width: 'fit-content',
      background: theme.palette.background.paper,
    },

    attachmentLabel: {
      flex: 1,
    },

    attachmentDownload: {
      cursor: 'pointer',
    },

    tooManyresults: {
      '& h3': {
        marginTop: '0.2em',
        marginBottom: '0.4em',
      },
      '& p': {
        marginTop: 0,
        marginBottom: '1.1em',
      },
    },

    buttonsContainer: {
      display: 'flex',
    },

    shareButtonsContainer: {
      display: 'flex',
      // marginBottom: '8px',
    },

    shareIcon: {
      height: '100%',
      width: '100%',
    },

    shareButton: {
      height: '30px',
      width: '30px',
      display: 'flex',
      margin: theme.spacing(0.5),
      overflow: 'hidden',
      borderRadius: theme.shape.borderRadius,
    },

    citationButton: {
      margin: theme.spacing(0.5),
    },

    citationText: {
      padding: theme.spacing(2),
      flex: 1,
    },
    paginationButton: {
      width: '32px',
      height: '32px',
      minWidth: '32px',
      borderRadius: '32px !important',
    },
    paginationButton_small: {
      width: '26px',
      height: '26px',
      minWidth: '26px',
      borderRadius: '26px !important',
    },
  };
});
