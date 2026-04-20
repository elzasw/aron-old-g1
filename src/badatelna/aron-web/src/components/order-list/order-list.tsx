// import { Button } from '../button';
import { useState } from 'react';
import MUIButton from '@material-ui/core/Button';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import { useOrders, getGroupName } from './orders-provider';
import { Link } from 'react-router-dom';
import { ModulePath } from '../../enums';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { ZpusobZpristupneni, OrderItemInfoEx, InstitutionInfo, BadatelnaInfo } from './types';
import { AccessTypeSelect } from './access-type-select';
import { OrderConfirmDialog } from './order-confirm-dialog';

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

interface DialogState {
  institution: InstitutionInfo;
  badatelna?: BadatelnaInfo;
  orderItems: OrderItemInfoEx[];
}

export function OrderList() {
  const { orderItems: _orderItems, groups, removeItem, changeItemType, clearAll } = useOrders();
  const classes = useStyles();
  const [dialogState, setDialogState] = useState<DialogState | null>(null);

  if (_orderItems.length === 0) {
    return <></>
  }

  const groupsByInstitution = groups.map((groupName) => {
    const filteredItems = _orderItems.filter(({
      institution: _institution,
      badatelna: _badatelna
    }) => getGroupName(_institution, _badatelna) === groupName);
    const {institution: _institution, badatelna: _badatelna} = filteredItems[0]
    return {
      institution: _institution,
      badatelna: _badatelna,
      // orderItems: _orderItems.filter(({institution: _institution, }) => _institution.code === institution.code),
      orderItems: filteredItems,
    }
  })

  function handleChangeType(item:OrderItemInfoEx, type: ZpusobZpristupneni) {
    changeItemType(item.apuId, type);
  }

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
      {groupsByInstitution.map(({institution, badatelna, orderItems}) => {
        return <div style={{
          padding: "8px",
          margin: "8px",
          background: "#f7f7f7",
          borderRadius: "4px",
          border: "1px solid #ddd",
        }}>
          <div style={{marginBottom: "4px"}}><b>{institution.name}</b></div>
          {badatelna && <div style={{ marginBottom: "4px" }}><i>{badatelna.name}</i></div>}
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
                <div>
                  <AccessTypeSelect value={item.type} onChange={(type) => handleChangeType(item, type)}/>
                </div>
                <div style={{ flexShrink: 0 }}>
                  <MUIButton size="small" onClick={() => removeItem(item.apuId)} ><DeleteOutline/></MUIButton>
                </div>
              </div>
            })}
          </div>
          <div style={{display: "flex", justifyContent: "flex-end"}}>
            <MUIButton
              variant="contained"
              size="medium"
              color="primary"
              style={{borderRadius: "4px"}}
              onClick={() => setDialogState({ institution, badatelna, orderItems })}
            >Objednat</MUIButton>
          </div>
        </div>
      })}
    </div>
    <div style={{ display: "flex", justifyContent: "flex-start", padding: "4px 8px 8px" }}>
      <MUIButton size="small" onClick={clearAll}>Vyprázdnit košík</MUIButton>
    </div>
    {dialogState && (
      <OrderConfirmDialog
        open={true}
        onClose={() => setDialogState(null)}
        institution={dialogState.institution}
        badatelna={dialogState.badatelna}
        orderItems={dialogState.orderItems}
      />
    )}
  </div>;
}
