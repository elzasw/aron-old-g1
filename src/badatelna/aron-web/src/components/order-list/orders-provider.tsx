import { createContext, PropsWithChildren, useContext, useState } from 'react';
import { InstitutionInfo, OrderItemInfoEx, ZpusobZpristupneni, BadatelnaInfo } from './types';


export interface OrdersContextType {
  orderItems: OrderItemInfoEx[];
  groups: string[];
  addItem: (itemInfo: OrderItemInfoEx) => void;
  removeItem: (apuId: string) => void;
  changeItemType: (apuId: string, type: ZpusobZpristupneni) => void;
  clearAll: () => void;
  clearGroup: (groupName: string) => void;
}

const OrdersContext = createContext<OrdersContextType>({
  orderItems: [],
  groups: [],
  addItem: () => { return; },
  removeItem: () => { return;},
  changeItemType: () => { return;},
  clearAll: () => { return;},
  clearGroup: () => { return;},
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

interface OrdersState {
  orderItems: OrderItemInfoEx[];
  groups: string[];
}

export function OrdersProvider({children}:PropsWithChildren<{}>) {
  const { store, load, clear } = useLocalStorage<OrdersState>("orderItems");

  const [state, setState] = useState<OrdersState>(() => {
    const loadedState = load();
    return {
      orderItems: loadedState?.orderItems || [],
      groups: loadedState?.groups || [],
    };
  });
  const { orderItems, groups } = state;

  // orderItems and groups must always be updated in a single setState call: React 17
  // does not batch updates triggered from async code (e.g. after an order is sent),
  // so two separate calls would produce a render with a torn, inconsistent state.
  function update(newState: OrdersState) {
    setState(newState);
    store(newState);
  }

  function addItem(itemInfo: OrderItemInfoEx) {
    const groupName = getGroupName(itemInfo.institution, itemInfo.badatelna);
    const existingGroup = groups.find((_groupName) => _groupName === groupName);

    update({
      orderItems: [...orderItems, itemInfo],
      groups: existingGroup ? groups : [...groups, groupName],
    });
  }

  function removeItem(apuId: string) {
    const item = orderItems.find(({ apuId: _apuId }) => _apuId === apuId);
    if(!item){throw `Item with apuId: ${apuId} does not exist.`}

    const _orderItems = orderItems.filter(({apuId: _apuId}) => _apuId !== apuId);

    const groupName = getGroupName(item.institution, item.badatelna)
    const itemsFromGroup = _orderItems.filter(({ institution, badatelna }) =>
      getGroupName(institution, badatelna) === groupName
    )

    update({
      orderItems: _orderItems,
      groups: itemsFromGroup.length === 0
        ? groups.filter((_groupName) => _groupName !== groupName)
        : groups,
    });
  }

  function changeItemType(apuId: string, type: ZpusobZpristupneni) {
    const itemIndex = orderItems.findIndex(({ apuId: _apuId }) => _apuId === apuId);
    const item = orderItems[itemIndex];
    const newItems = [...orderItems.slice(0, itemIndex), {...item, type}, ...orderItems.slice(itemIndex + 1)]

    update({ orderItems: newItems, groups });
  }

  function clearAll() {
    setState({ orderItems: [], groups: [] });
    clear();
  }

  function clearGroup(groupName: string) {
    update({
      orderItems: orderItems.filter(({ institution, badatelna }: OrderItemInfoEx) =>
        getGroupName(institution, badatelna) !== groupName
      ),
      groups: groups.filter((g: string) => g !== groupName),
    });
  }

  return <OrdersContext.Provider value={{orderItems, addItem, removeItem, changeItemType, groups, clearAll, clearGroup}}>
    {children}
  </OrdersContext.Provider>
}
