import { createContext, PropsWithChildren, useContext, useState } from 'react';
import { InstitutionInfo, OrderItemInfoEx, ZpusobZpristupneni, BadatelnaInfo } from './types';


export interface OrdersContextType {
  orderItems: OrderItemInfoEx[];
  groups: string[];
  addItem: (itemInfo: OrderItemInfoEx) => void;
  removeItem: (apuId: string) => void;
  changeItemType: (apuId: string, type: ZpusobZpristupneni) => void;
}

const OrdersContext = createContext<OrdersContextType>({
  orderItems: [],
  groups: [],
  addItem: () => { return; },
  removeItem: () => { return;},
  changeItemType: () => { return;}
})

export function useOrders() {
  const orders = useContext<OrdersContextType>(OrdersContext);
  return orders;
}

function useLocalStorage<T>(key: string){
  function store(value: T) {
    const stringValue = JSON.stringify(value);
    localStorage.setItem(key, stringValue);
  }

  function load() {
    const stringValue = localStorage.getItem(key);
    if (!stringValue) { return null;}

    const value = JSON.parse(stringValue);
    return value as T;
  }

  function clear() {
    localStorage.removeItem(key);
  }

  return {store, load, clear}
}

export function getGroupName(institution: InstitutionInfo, badatelna?: BadatelnaInfo) {
  if (!badatelna) {
    return institution.code;
  }
  return `${institution.code}_${badatelna.code}`;
}

export function OrdersProvider({children}:PropsWithChildren<{}>) {
  const { store, load } = useLocalStorage<{ orderItems: OrderItemInfoEx[], groups: string[] }>("orderItems");
  const loadedState = load();

  const [orderItems, setOrderItems] = useState<OrderItemInfoEx[]>(loadedState?.orderItems || []);
  const [groups, setGroups] = useState<string[]>(loadedState?.groups || []);

  function addItem(itemInfo: OrderItemInfoEx) {
    const _orderItems = [...orderItems, itemInfo];
    let _groups = groups;

    setOrderItems(_orderItems);

    const groupName = getGroupName(itemInfo.institution, itemInfo.badatelna);
    const existingGroup = groups.find((_groupName) => _groupName === groupName);
    if (!existingGroup) {
      _groups = [...groups, groupName];
      setGroups(_groups);
    }

    store({orderItems: _orderItems, groups: _groups})
  }

  function removeItem(apuId: string) {
    const item = orderItems.find(({ apuId: _apuId }) => _apuId === apuId);
    if(!item){throw `Item with apuId: ${apuId} does not exist.`}

    const _orderItems = orderItems.filter(({apuId: _apuId}) => _apuId !== apuId);

    let _groups = groups;

    setOrderItems(_orderItems);

    const groupName = getGroupName(item.institution, item.badatelna)
    const itemsFromGroup = _orderItems.filter(({ institution, badatelna }) =>
      getGroupName(institution, badatelna) === groupName
    )
    console.log("remove item", _orderItems, groupName, itemsFromGroup);
    if (itemsFromGroup.length === 0) {
      _groups = groups.filter((_groupName) => _groupName !== groupName);
    }
    setGroups(_groups);

    store({orderItems: _orderItems, groups: _groups})
  }

  function changeItemType(apuId: string, type: ZpusobZpristupneni) {
    const itemIndex = orderItems.findIndex(({ apuId: _apuId }) => _apuId === apuId);
    const item = orderItems[itemIndex];
    const newItems = [...orderItems.slice(0, itemIndex), {...item, type}, ...orderItems.slice(itemIndex + 1)]
    setOrderItems(newItems)

    store({orderItems: newItems, groups})
  }

  return <OrdersContext.Provider value={{orderItems, addItem, removeItem, changeItemType, groups}}>
    {children}
  </OrdersContext.Provider>
}
