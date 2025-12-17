import { createContext, PropsWithChildren, useContext, useEffect, useState } from "react";
import { Api, InstitutionDef, UserStatus  } from "./api";

interface ItemStore<T> {
  items: T[];
  itemsMap: Record<string | number, T>;
  isFetching: boolean;
  isFetched: boolean;
}

interface GeneralContextValue {
  institutions: ItemStore<InstitutionDef>;
  institutionId?: number;
  userStatus: UserStatus;
  setInstitutionId:(institutionId: number)=>void;
}

const initialInstitutions: ItemStore<InstitutionDef> = {
  items: [],
  itemsMap: {},
  isFetching: false,
  isFetched: false
}

const initialUserStatus : UserStatus = {
    username: "unknown",
    admin: false
}

const initialContextValue: GeneralContextValue = {
  institutions: initialInstitutions,
  userStatus: initialUserStatus,
  setInstitutionId:()=>{}
}

export const GeneralContext = createContext<GeneralContextValue>(initialContextValue);

export function GeneralContextProvider({ children }: PropsWithChildren<{}>) {
  const [institutions, setInstitutions] = useState<ItemStore<InstitutionDef>>(initialInstitutions);
  const [userStatus, setUserStatus] = useState<UserStatus>(initialUserStatus);
  const [institutionId, setInstitutionId] = useState<number>(-1);

  useEffect(() => {
    (async () => {
      setInstitutions({ ...institutions, isFetching: true })
      const { items } = await Api.transfagent.getInstitutions({ pageSize: 1000 });
      items.sort((i1,i2)=> {
        if (i1.name && i2.name) { 
          const n1 = i1.name?.toLocaleLowerCase();
          const n2 = i2.name?.toLocaleLowerCase()
          return n1.localeCompare(n2);
        } else {
          return -1;
        }
      } );
      const itemsMap: Record<string | number, InstitutionDef> = {}
      items.forEach((institution) => {
        itemsMap[institution.institutionId] = institution;
      })
      setInstitutions({ items, itemsMap, isFetching: false, isFetched: true });      
    })()

  }, []);

  useEffect(() => {
    (async () => {
      setUserStatus(userStatus);
      const newUserStatus = await Api.transfagent.getUser();
      setUserStatus(newUserStatus);
    })()
  }, []);

  return <GeneralContext.Provider value={{ institutions, userStatus, institutionId, setInstitutionId }}>
    {children}
  </GeneralContext.Provider>;
}

export function useGeneralContext() {
  const generalContext = useContext(GeneralContext);
  return generalContext;
}
