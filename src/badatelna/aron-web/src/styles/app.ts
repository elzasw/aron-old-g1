import makeStyles from '@material-ui/core/styles/makeStyles';

export const useAppStyles = makeStyles({
  app: {
    '& img': {
      'user-drag': 'none',
      'user-select': 'none',
      '-moz-user-select': 'none',
      '-webkit-user-drag': 'none',
      '-webkit-user-select': 'none',
      '-ms-user-select': 'none',
    },

    '& .MuiSnackbar-root': {
      zIndex: 100000,
    },
    position: "relative",
    height: "0",
    flex: 1,
  },
  appLoadingFailed: {
    display: 'flex',
    justifyContent: 'center',
    padding: '2em',
    width: 'calc(100% - 4em)',
    fontSize: '1.4rem',
  },
  screenReaderHidden: {
    position: "absolute",
    display: "inline-block",
    left: "-10000px",
    top: 0,
    overflow: "hidden",
    "&:focus-within": {
      left: 0
    }
  }
});
