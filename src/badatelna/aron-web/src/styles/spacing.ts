import makeStyles from '@material-ui/core/styles/makeStyles';

const BIG = 3;
const NORMAL = 2;
const SMALL = 1;
const EXTRA_SMALL = 0.5;

export const useSpacingStyles = makeStyles(({ spacing }) => {
  const big = spacing(BIG);
  const normal = spacing(NORMAL);
  const small = spacing(SMALL);

  return {
    padding: {
      padding: normal,
    },
    paddingBig: {
      padding: big,
    },
    paddingSmall: {
      padding: small,
    },
    paddingNone: {
      padding: '0 !important',
    },

    paddingTop: {
      paddingTop: normal,
    },
    paddingBottom: {
      paddingBottom: normal,
    },
    paddingLeft: {
      paddingLeft: normal,
    },
    paddingRight: {
      paddingRight: normal,
    },

    paddingTopBig: {
      paddingTop: big,
    },
    paddingBottomBig: {
      paddingBottom: big,
    },
    paddingLeftBig: {
      paddingLeft: big,
    },
    paddingRightBig: {
      paddingRight: big,
    },

    paddingTopSmall: {
      paddingTop: small,
    },
    paddingBottomSmall: {
      paddingBottom: small,
    },
    paddingLeftSmall: {
      paddingLeft: small,
    },
    paddingRightSmall: {
      paddingRight: small,
    },

    paddingVertical: {
      padding: spacing(NORMAL, 0),
    },
    paddingHorizontal: {
      padding: spacing(0, NORMAL),
    },
    paddingVerticalBig: {
      padding: spacing(BIG, 0),
    },
    paddingHorizontalBig: {
      padding: spacing(0, BIG),
    },
    paddingVerticalSmall: {
      padding: spacing(SMALL, 0),
    },
    paddingHorizontalSmall: {
      padding: spacing(0, SMALL),
    },

    margin: {
      margin: normal,
    },
    marginBig: {
      margin: big,
    },
    marginSmall: {
      margin: small,
    },
    marginNone: {
      margin: '0 !important',
    },

    marginTop: {
      marginTop: normal,
    },
    marginBottom: {
      marginBottom: normal,
    },
    marginLeft: {
      marginLeft: normal,
    },
    marginRight: {
      marginRight: normal,
    },

    marginTopBig: {
      marginTop: big,
    },
    marginBottomBig: {
      marginBottom: big,
    },
    marginLeftBig: {
      marginLeft: big,
    },
    marginRightBig: {
      marginRight: big,
    },

    marginTopSmall: {
      marginTop: small,
    },
    marginBottomSmall: {
      marginBottom: small,
    },
    marginLeftSmall: {
      marginLeft: small,
    },
    marginRightSmall: {
      marginRight: small,
    },

    marginVertical: {
      margin: spacing(NORMAL, 0),
    },
    marginHorizontal: {
      margin: spacing(0, NORMAL),
    },
    marginVerticalBig: {
      margin: spacing(BIG, 0),
    },
    marginHorizontalBig: {
      margin: spacing(0, BIG),
    },
    marginVerticalSmall: {
      margin: spacing(SMALL, 0),
    },
    marginHorizontalSmall: {
      margin: spacing(0, SMALL),
    },
    marginHorizontalExtraSmall: {
      margin: spacing(0, EXTRA_SMALL),
    },
  };
});
