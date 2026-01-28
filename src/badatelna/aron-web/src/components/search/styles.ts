import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
  return {
    search: {
      width: '100%',

      '& input.MuiInputBase-input': {
        '&::placeholder': {
          color: theme.palette.grey[600],
          opacity: 1,
        },
        '&::-ms-input-placeholder': {
          color: theme.palette.grey[600],
        },
      },
    },
    searchInner: {
      position: 'relative',
      width: '100%',
      height: 40,
      borderRadius: 5,
      '& $searchTextField .MuiInputBase-root': {
        height: 40,
      },
      '&:hover': {
        boxShadow: 'rgba(0, 0, 0, 0.35) 0px 5px 15px',
      },
    },
    searchBigInner: {
      height: 60,
      '& $searchTextField .MuiInputBase-root': {
        height: 60,
      },
    },
    searchAdvancedContainer: {
      display: 'flex',
      [theme.breakpoints.down('md')]: {
        flexDirection: 'column-reverse',
      },
    },
    searchAdvanced: {
      fontSize: '0.7rem',
      fontWeight: 600,
      color: theme.palette.primary.main,
      cursor: 'pointer',
      zIndex: 1,
      textDecoration: 'none',
      textTransform: 'uppercase',
    },
    searchIcon: {
      fontSize: '1.8rem',
      marginRight: 8,
    },
    searchButton: {
      height: '100%',
      borderRadius: '0 5px 5px 0 !important',

      '&:hover': {
        background: theme.palette.primary.light,
      },
    },
    searchResetButton: {
      position: 'absolute',
      right: '5px',
      borderRadius: '20px',
      width: '25px',
      height: '25px',
    },
    searchTextFieldWrapper: {
      position: 'relative',
      flexGrow: 1,
    },
    searchTextInserted: {
      '& .MuiInputBase-input': {
        paddingRight: '35px',
      },
    },
    searchTextField: {
      flexGrow: 1,
      borderRadius: 5,
      '& .MuiInputBase-root': {
        borderRadius: '5px 0 0 5px',
      },
    },
  };
});
