import Icon from '@material-ui/icons/ShoppingBasket';
import classNames from 'classnames';
import { useState } from 'react';
import { Button } from '../../components';
import { useLayoutStyles } from '../../styles';
import { OrderList } from './order-list';
import { useOrders } from './orders-provider';
import { useConfiguration } from '../configuration';

export function OrderListButton() {
  const [isOpen, setIsOpen] = useState(false);
  const layoutClasses = useLayoutStyles();
  const { orderItems } = useOrders();
  const { showOrders } = useConfiguration();

  async function handleClick() {
    setIsOpen(!isOpen);
  }

  if (orderItems.length < 1 || !showOrders) {
    return <></>
  }

  return <div className={classNames(layoutClasses.flexCentered)}>
    <div style={{position: "relative"}}>
      <Button
          onClick={handleClick}
          startIcon={<Icon/>}
          label={orderItems.length}
          style={{color: "white"}}
          />
      {isOpen && <OrderList />}
    </div>
  </div>;
}
