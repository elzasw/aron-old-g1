import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => ({
  root: {
    // height: 22,
    verticalAlign: 'top',
    borderRadius: 5,
    '&:before': {
      border: 0,
    },
  },

  input: {
    // padding: '2pt',
    pointerEvents: 'none',
  },
  disabled: {
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    overflow: 'hidden',
  },
}));
