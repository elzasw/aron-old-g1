import makeStyles from '@material-ui/core/styles/makeStyles';

export const useLayoutStyles = makeStyles((theme) => ({
  flex: {
    display: 'flex',
  },
  flexCentered: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  flexSpaceBetween: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  flexBottom: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'flex-end',
  },
  flexSpaceBetweenBottom: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
  },
  flexEnd: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  flexAlignCenter: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center',
  },
  flexAlignTop: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'flex-start',
  },
  flexColumn: {
    display: 'flex',
    flexDirection: 'column',
  },
  flexColumnCenter: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  flexColumnSpaceBetween: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  flexWrap: {
    flexWrap: 'wrap',
  },
  flexColumnPhone: {
    display: 'flex',
    flexDirection: 'column',
    [theme.breakpoints.up('md')]: {
      flexDirection: 'row',
    },
  },
  flexGrow0: {
    flexGrow: 0
  },
  flexGrow1: {
    flexGrow: 1
  },
  flexGrow2: {
    flexGrow: 2
  },
  flexGrow3: {
    flexGrow: 3
  },
  flexGrow4: {
    flexGrow: 4
  },
}));
