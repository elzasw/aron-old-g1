import { createUrl } from '../../common-utils';
import { Button } from '../../components';
import { Props as ButtonProps } from '../../components/button/types';
import { useOrders } from './orders-provider';
import { OrderItemInfo, ZpusobZpristupneni } from './types';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import { useState } from 'react';
import { messages } from './messages';

export interface Props extends ButtonProps {
  apuId: string;
}

async function apiGet<T>(url: string):Promise<T> {
  try {
    const response = await fetch(createUrl(`${url}`));

    return response.ok ? await response.json() : null;
  } catch (error) {
    console.log(error);
    throw (error);
  }
}

const types = [
  ZpusobZpristupneni.NAHLIZENI,
  ZpusobZpristupneni.REPLIKA,
  ZpusobZpristupneni.OVERENI_SHODY,
];

export function AddToOrderButton({ apuId, ...otherProps }: Props) {
  const { orderItems, addItem, removeItem } = useOrders();
  const isOrdered = orderItems.findIndex(({ apuId: _apuId }) => _apuId === apuId) >= 0;
  const [anchorElement, setAnchorElement] = useState<HTMLButtonElement | null>(null);

  async function handleClick(type: ZpusobZpristupneni) {
    if (!isOrdered) {
      const orderItemInfo = await apiGet<OrderItemInfo>(`/script/order-item-info/${apuId}`)
      if (orderItemInfo) {
        addItem({ ...orderItemInfo, apuId, type });
        setAnchorElement(null);
      }
    }
  }

  function handleRemove() {
    removeItem(apuId);
  }

  if (isOrdered) {
    return <Button
    {...otherProps}
    onClick={handleRemove}
    label={"Odebrat z objednávky"}
  />
  }

  return <div>
    <Button
    {...otherProps}
    onClick={({currentTarget}) => setAnchorElement(currentTarget)}
    label={"Objednat"}
    />
    <Menu
      open={Boolean(anchorElement)}
      anchorEl={anchorElement}
      onClose={() => setAnchorElement(null)}
      anchorOrigin={{vertical: "bottom", horizontal: "left"}}
      transformOrigin={{vertical: "top", horizontal: "left"}}
    >
      {types.map((type) => {
        return <MenuItem onClick={() => handleClick(type)} disableRipple={false}>{messages[type]}</MenuItem>
      })}
    </Menu>
  </div>
}
