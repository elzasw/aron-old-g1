import { createContext, PropsWithChildren, useContext, useEffect, useState } from "react";
import { Api, InstitutionDef } from "./api";

interface ItemStore<T> {
  items: T[];
  itemsMap: Record<string | number, T>;
  isFetching: boolean;
  isFetched: boolean;
}

interface GeneralContextValue {
  institutions: ItemStore<InstitutionDef>;
}

const initialInstitutions: ItemStore<InstitutionDef> = {
  items: [],
  itemsMap: {},
  isFetching: false,
  isFetched: false
}

const initialContextValue: GeneralContextValue = {
  institutions: initialInstitutions
}

export const GeneralContext = createContext<GeneralContextValue>(initialContextValue);

export function GeneralContextProvider({ children }: PropsWithChildren<{}>) {
  const [institutions, setInstitutions] = useState<ItemStore<InstitutionDef>>(initialInstitutions);

  useEffect(() => {
    (async () => {
      setInstitutions({ ...institutions, isFetching: true })
      const { items } = await Api.transfagent.getInstitutions({ pageSize: 1000 });
      const itemsMap: Record<string | number, InstitutionDef> = {}
      items.forEach((institution) => {
        itemsMap[institution.institutionId] = institution;
      })
      setInstitutions({ items, itemsMap, isFetching: false, isFetched: true });
    })()

  }, []);
  return <GeneralContext.Provider value={{ institutions }}>
    {children}
  </GeneralContext.Provider>;
}

export function useGeneralContext() {
  const generalContext = useContext(GeneralContext);
  return generalContext;
}
