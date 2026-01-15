import { createContext, PropsWithChildren, useContext, useState } from 'react';
import { InstitutionInfo, OrderItemInfoEx } from './types';


export interface OrdersContextType {
  orderItems: OrderItemInfoEx[];
  institutions: InstitutionInfo[];
  addItem: (itemInfo: OrderItemInfoEx) => void;
  removeItem: (apuId: string) => void;
}

const OrdersContext = createContext<OrdersContextType>({
  orderItems: [],
  institutions: [],
  addItem: () => { return; },
  removeItem: () => { return;}
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

export function OrdersProvider({children}:PropsWithChildren<{}>) {
  const { store, load } = useLocalStorage<{ orderItems: OrderItemInfoEx[], institutions: InstitutionInfo[] }>("orderItems");
  const loadedState = load();

  const [orderItems, setOrderItems] = useState<OrderItemInfoEx[]>(loadedState?.orderItems || []);
  const [institutions, setInstitutions] = useState<InstitutionInfo[]>(loadedState?.institutions || []);

  function addItem(itemInfo: OrderItemInfoEx) {
    const _orderItems = [...orderItems, itemInfo];
    let _institutions = institutions;

    setOrderItems(_orderItems);

    const existingInstitution = institutions.find(({ number }) => { return number === itemInfo.institution.number });
    if (!existingInstitution) {
      _institutions = [...institutions, itemInfo.institution]
      setInstitutions(_institutions);
    }

    store({orderItems: _orderItems, institutions: _institutions})
  }

  function removeItem(apuId: string) {
    const _orderItems = orderItems.filter(({apuId: _apuId}) => _apuId !== apuId);
    let _institutions = institutions;

    setOrderItems(_orderItems);

    const item = orderItems.find(({ apuId: _apuId }) => _apuId === apuId);

    const itemsFromInstitution = orderItems.filter(({ institution }) =>
      institution.number === item?.institution.number
    )
    if (itemsFromInstitution.length === 1) {
      _institutions = institutions.filter(({ number: _number}) => _number !== item?.institution.number)
      setInstitutions(_institutions);
    }

    store({orderItems: _orderItems, institutions: _institutions})
  }

  return <OrdersContext.Provider value={{orderItems, addItem, removeItem, institutions}}>
    {children}
  </OrdersContext.Provider>
}
