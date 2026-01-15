// import { Button } from '../button';
import MUIButton from '@material-ui/core/Button';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import { useOrders } from './orders-provider';
import { messages } from './messages';
import { Link } from 'react-router-dom';
import { ModulePath } from '../../enums';
import makeStyles from '@material-ui/core/styles/makeStyles';

export interface Props {
  apuId: string;
}

export const useStyles = makeStyles(() => {
  return {
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
  };
});

export function OrderList() {
  const { orderItems: _orderItems, institutions, removeItem } = useOrders();
  const classes = useStyles();

  if (_orderItems.length === 0) {
    return <></>
  }

  const groupsByInstitution = institutions.map((institution) => {
    return {
      institution,
      orderItems: _orderItems.filter(({institution: _institution}) => _institution.number === institution.number),
    }
  })

  return <div style={{
    position: "absolute",
    top: "105%",
    right: 0,
    zIndex: 2000,
    width: "500px",
    border: "1px solid #ddd",
    borderRadius: "8px",
    background: "white",
  }}>
    <div style={{
      maxHeight: "600px",
      overflow: "auto",
    }}>
      {groupsByInstitution.map(({institution, orderItems}) => {
        return <div style={{
          padding: "8px",
          margin: "8px",
          background: "#f7f7f7",
          borderRadius: "4px",
          border: "1px solid #ddd",
        }}>
          <div style={{marginBottom: "4px"}}><b>{institution.name}</b></div>
          <div style={{margin: "8px 0"}}>
            {orderItems.map((item, index) => {
              const isLast = index === orderItems.length - 1

              return <div style={{
                display: "flex",
                padding: "4px",
                borderBottom: isLast ? "none" : "1px solid #ddd",
                alignItems: "center",
              }}>
                <div style={{
                  flexShrink: 1,
                  flexGrow: 1,
                  textOverflow: "ellipsis",
                  overflow: "hidden",
                  whiteSpace: "nowrap"
                }}>
                  <Link
                    to={{ pathname: `${ModulePath.APU}/${item.apuId}` }}
                    className={classes.link}
                    title={item.archivalie.popis}
                  >
                    {item.archivalie.popis}
                  </Link>
                </div>
                <div style={{margin: "0 4px", whiteSpace: "nowrap"}}>{messages[item.type]}</div>
                <div style={{ flexShrink: 0 }}>
                  <MUIButton size="small" onClick={() => removeItem(item.apuId)} ><DeleteOutline/></MUIButton>
                </div>
              </div>
            })}
          </div>
          <div style={{display: "flex", justifyContent: "flex-end"}}>
            <MUIButton variant="contained" size="medium" color="primary" style={{borderRadius: "4px"}}>Objednat</MUIButton>
          </div>
        </div>
      })}
    </div>
  </div>;
}
