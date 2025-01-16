import {
    Navigate,
    Route,
    RouterProvider,
    createBrowserRouter,
    createRoutesFromElements,
    Params,
} from "react-router-dom";
import RootLayout from "./RootLayout";
import { DaosList, FundsList, SourcesList, EntityList, AidsList, ArchDescList } from "./components";

//@ts-ignore
const serverContextPath = window.serverContextPath;

export const URL_SOURCES = "/sources";
export const getSourcesPath = (filter?: string) => {
    if (filter != undefined) {
        return `${URL_SOURCES}?filter=${filter}`;
    }
    return URL_SOURCES;
}

export const URL_DAOS = "/daos";
export const getDaosPath = () => {
    return URL_DAOS
}

export const URL_FUNDS = "/funds";
export const getFundsPath = (filter?: string) => {
    if (filter != undefined) {
        return `${URL_FUNDS}?filter=${filter}`;
    }
    return URL_FUNDS
}

export const URL_ENTITIES = "/entities";
export const getEntitiesPath = () => {
    return URL_ENTITIES
}

export const URL_AIDS = "/aids";
export const getAidsPath = () => {
    return URL_AIDS
}

export const URL_ARCHDESCS = "/archdescs";
export const getArchDescsPath = () => {
    return URL_ARCHDESCS
}

const router = createBrowserRouter(
    createRoutesFromElements(
        <Route path="/" element={<RootLayout />}>
            <Route path={`${URL_SOURCES}`} element={<SourcesList />} />
            <Route path={``} element={<Navigate replace to={`${URL_SOURCES}`} />} />
            <Route path={`${URL_DAOS}`} element={<DaosList />} />
            <Route path={`${URL_FUNDS}`} element={<FundsList />} />
            <Route path={`${URL_ENTITIES}`} element={<EntityList />} />
            <Route path={`${URL_AIDS}`} element={<AidsList />} />
            <Route path={`${URL_ARCHDESCS}`} element={<ArchDescList />} />
        </Route>
    ),
    {
        basename: serverContextPath.startsWith("http") ? "" : serverContextPath,
    }
);

export default function App() {
    return <RouterProvider router={router} />;
}
