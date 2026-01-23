import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles(() => {
  return {
    listPageNumber: {
      display: 'flex',
      whiteSpace: 'nowrap',
      alignItems: 'center',
    },
    pageSizeSelect: {
      // '& > div': {
      //   minWidth: 40,
      // },
      // '& div.MuiSelect-select.MuiSelect-select': {
      //   paddingRight: 2,
      // },
      borderRadius: 5,
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
