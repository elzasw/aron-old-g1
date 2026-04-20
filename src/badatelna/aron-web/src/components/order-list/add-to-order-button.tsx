import { createUrl } from '../../common-utils';
import { Button } from '../../components';
import { Props as ButtonProps } from '../../components/button/types';
import { BASE_URL } from '../../enums/api';
import { ModulePath } from '../../enums';
import { useOrders } from './orders-provider';
import { OrderItemInfo, ZpusobZpristupneni } from './types';

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

export function AddToOrderButton({ apuId, ...otherProps }: Props) {
  const { orderItems, addItem, removeItem } = useOrders();
  const isOrdered = orderItems.findIndex(({ apuId: _apuId }) => _apuId === apuId) >= 0;

  async function handleAdd() {
    if (!isOrdered) {
      const orderItemInfo = await apiGet<OrderItemInfo>(`/script/order-item-info/${apuId}`)
      if (orderItemInfo) {
        const apuUrl = `${window.location.origin}${BASE_URL}${ModulePath.APU}/${apuId}`;
        addItem({
          ...orderItemInfo,
          archivalie: { ...orderItemInfo.archivalie, url: apuUrl },
          apuId,
          type: ZpusobZpristupneni.NAHLIZENI,
        });
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
    onClick={handleAdd}
    label={"Objednat"}
    />
  </div>
}
