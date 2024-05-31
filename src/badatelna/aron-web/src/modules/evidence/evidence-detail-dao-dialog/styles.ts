import makeStyles from '@material-ui/core/styles/makeStyles';
import { colorGrey } from '../../../styles';

export interface StyleConfiguration {
  alternativeItemLabel?: boolean;
}

export const useStyles = makeStyles((theme) => {
  const md = theme.breakpoints.up('md');
  return {
    icon: {
      cursor: 'pointer',
    },
    bold: {
      fontWeight: 600,
    },
    daoDialog: {
      position: 'relative',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0)',
      height: '100%',
      zIndex: 0,
    },
    daoDialogFixed: {
      position: 'fixed',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      background: 'rgba(0, 0, 0, 0.85)',
      height: '100vh',
      width: '100vw',
      zIndex: 100,
    },
    daoDialogSection: {
      position: 'relative',
    },
    daoDialogSide: {
      width: '100%',
      display: 'none',

      [md]: {
        width: '160px',
        display: 'block',
      },
    },
    daoDialogLeft: {
      zIndex: 10,
    },
    daoDialogCenter: {
      left: 0,
      width: '100%',
      top: 0,
      height: '100%',

      [md]: {
        width: '60%',
        left: '20%',
        display: 'block',
      },
    },
    daoDialogCenterNoSidebar: {
      [md]: {
        height: '100%',
        top: 0,
        left: '160px',
        display: 'block',
      },
    },
    daoDialogCenterNoThumbnails: {
      left: 0,
      width: '100%',
      top: 0,
      height: '100%',
    },
    daoDialogRight: {
      right: 0,
      padding: '30px 16px 8px 8px',
    },
    daoDialogSideOpen: {
      display: 'block',
      zIndex: 10,
    },
    daoDialogNoFiles: {
      color: '#fff',
      width: '100%',
      height: '100%',
    },
    daoDialogSectionPart: {
      background: '#0009',
      height: '100%',
      overflowY: 'auto',
    },
    daoDialogSectionPartLabel: {
      color: '#fff',
      height: 30,
    },
    daoDialogSectionPartContent: {
      padding: '10px 25%',
      overflow: 'auto',
      height: '100%',
      [md]: {
        padding: '0px',
      }
    },
    daoThumbnailContainer: {
      width: '100%',
      height: '100%',
      borderRadius: '5px',
      overflow: 'hidden',
      cursor: 'pointer',
      position: 'relative',
      marginBottom: '4px',
      minHeight: '4em'
    },
    daoThumbnailTitle: {
      color: 'white',
      position: 'absolute',
      zIndex: 10,
      padding: '5px 10px',
      textShadow: '0px 0px 8px black',
      bottom: 0,
      lineHeight: '1em',
      width: '100%',
      background: '#0007',
      maxHeight: '100%',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    },
    daoThumbnail: {
      width: '100%',
      height: '100%',
      filter: 'brightness(0.6)',
      alignItems: 'center',
      display: 'flex',

      '$daoThumbnailContainer:hover &': {
        color: '#fff',
        background: theme.palette.primary.main,
        filter: 'brightness(1)',
      },

      '& > img': {
        width: '100%',
        height: '100%',
        objectFit: 'cover',
      }
    },
    daoDialogSectionPartActive: {
      outline: `2px solid ${theme.palette.primary.main}`,

      '& $daoThumbnail': {
        cursor: 'auto',
        filter: 'brightness(1)',
        color: '#fff',
      }
    },
    daoDialogMetadataContainer: {
      right: 0,
      width: 'auto',
      height: 'auto'
    },
    daoDialogMetadata: {
      overflowY: 'auto',
      background: '#222',
      color: 'white',
      margin: '10px',
      padding: '20px',
      borderRadius: '10px',
      boxShadow: '4px 4px 10px #0008',
      width: 'auto',
      right: 0,
    },
    daoDialogMetadataLabel: {
      width: 'auto',
      paddingRight: 8,
      textAlign: 'right',
    },
    daoDialogMetadataButton: {
      padding: '5px',
      borderRadius: '5px',
      background: '#444',
      marginBottom: '10px',
      display: 'inline-flex',
      alignItems: 'center',
      cursor: 'pointer',
    },
    daoDialogFloatingOverlay: {
      position: 'absolute',
      color: 'white',
      background: '#0008',
      padding: '8px 5px',
    },
    daoDialogMenu: {
      [md]: {
        display: 'none',
      },
    },
    daoDialogMenuPlaceholder: {
      display: 'none',
      width: 30,

      [md]: {
        display: 'block',
      },
    },
    daoDialogIcon: {
      color: '#fff',
      cursor: 'pointer',
      fontSize: 24,

      [md]: {
        fontSize: 30,
      },
    },
    daoDialogIconDisabled: {
      color: colorGrey,
      cursor: 'auto',
    },
    daoDialogToolbar: {
      width: '100%',
      background: '#0009',
      zIndex: 1,
      // overflowX: 'auto',
      // overflowY: 'visible',
      [md]: {
        // overflowX: 'hidden',
      }
    },
    daoDialogToolbarInner: {
      padding: '10px',
    },
    toolbarPageForm: {
      color: '#fff',
      whiteSpace: 'nowrap',
    },
    toolbarInput: {
      background: '#fff3',
      color: '#fff',
      width: '6ch',
      border: 'none',
      padding: '3px',
      borderRadius: '3px',
      textAlign: 'right',
      outline: 'none',
    },
    imageSettingsWrapper: {
      width: '100%',
      height: '100%',
    },
    imageSettingsButton: {
      cursor: 'pointer',
    }
  };
});
