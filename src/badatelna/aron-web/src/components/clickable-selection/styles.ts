import makeStyles from '@material-ui/core/styles/makeStyles';

export const useStyles = makeStyles((theme) => {
    return {
        option: {
            display: 'flex',
            alignItems: 'center',
            cursor: 'pointer',
            width: 'fit-content',
            marginBottom: theme.spacing(0.5),
            '& :first-child': {
                marginRight: theme.spacing(0.5),
            },
        },
        containerAlt: {
          display: "flex",
          flexWrap: "wrap-reverse",
          margin: `0 ${theme.spacing(0.5)}`,
        },
        optionAlt: {
          cursor: 'pointer',
          border: `1px dashed ${theme.palette.divider}`,
          borderStyle: "dashed",
          borderColor: "#ccc",
          background: "transparent",
          borderRadius: theme.shape.borderRadius,
          margin: `${theme.spacing(0.5)}px !important`,
          padding: `${theme.spacing(0.5)}px ${theme.spacing(1)}px`,
        },
        selected: {
          borderStyle: "solid",
          borderColor: "transparent",
          background: theme.palette.primary.main,
          color: theme.palette.primary.contrastText,
          fontWeight: "bold",
        },
        radioOption: {
            margin: 0,
            '& :first-child': {
                margin: 0,
                '&:hover': { background: 'none' },
            },
        },
    };
});
