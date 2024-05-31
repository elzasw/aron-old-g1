import makeStyles from '@material-ui/core/styles/makeStyles';
import {
  // appHeaderHeight,
  breadcrumbsHeight,
  colorText,
  border,
  colorGreyLight,
} from '../../styles';

export const useStyles = makeStyles((theme) => {
  return {
    module: {
      // minHeight: `calc(100% - ${appHeaderHeight})`,
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
      flex: 1,
    },
    moduleWrapper: {
      position: 'relative',
      flex: 1,
      height: '50%',
      // overflowX: 'auto',
    },
    breadcrumbsWrapper: {
      position: 'sticky',
      // top: appHeaderHeight,
      zIndex: 20,
      height: breadcrumbsHeight,
      borderBottom: border,
      background: '#fff',
      flexShrink: 0,
      flexGrow: 0,
    },
    breadcrumbs: {
      height: '100%',
      width: 'fit-content',
      fontSize: 13,
    },
    breadcrumb: {
      height: '100%',
      position: 'relative',
      whiteSpace: 'nowrap',
    },
    breadcrumbLink: {
      color: colorText,
      textDecoration: 'none',

      '&:hover': {
        textDecoration: 'underline',
      },
    },
    breadcrumbIcon: {
      cursor: 'pointer',
    },
    breadcrumbMenu: {
      position: 'absolute',
      top: `calc(${breadcrumbsHeight} - 1px)`,
      left: 8,
      background: '#fff',
      border: `1px solid ${colorGreyLight}`,
      borderTopWidth: 0,
      borderBottomLeftRadius: 4,
      borderBottomRightRadius: 4,
      display: 'flex',
      flexDirection: 'column',
      zIndex: 10,
      width: 260,
      overflow: 'hidden',
    },
    breadcrumbLinkResponsive: {
      padding: theme.spacing(1, 2),
      whiteSpace: 'normal',
    },
    toolbar: {
      height: '100%',
      position: 'absolute',
      right: 0,
    },
  };
});
