import {
    Navigate,
    Route,
    RouterProvider,
    createBrowserRouter,
    createRoutesFromElements,
    Params,
} from "react-router-dom";
import RootLayout from "./RootLayout";
import { AttachmentsList, DaosList, FundsList, SourcesList, EntityList, AidsList, ArchDescList, UsersList } from "./components";
import { InstitutionsList } from "./components/IntitutionsList";

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
export const getDaosPath = (filter?: string) => {
    if (filter != undefined) {
        return `${URL_DAOS}?filter=${filter}`;
    }
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

export const URL_INSTITUTIONS = "/institutions";
export const getInstitutionsPath = (filter?: string) => {
    if (filter != undefined) {
        return `${URL_INSTITUTIONS}?filter=${filter}`;
    }
    return URL_INSTITUTIONS
}

export const URL_ATTACHMENTS = "/attachments";
export const getAttachmentsPath = (apuSourceId?: string) => {
    if (apuSourceId != undefined) {
        return `${URL_ATTACHMENTS}?apuSourceId=${apuSourceId}`;
    }
    return URL_ATTACHMENTS;
}

export const URL_USERS = "/users";
export const getUsersPath = (filter?: string) => {
    if (filter != undefined) {
        return `${URL_USERS}?filter=${filter}`;
    }
    return URL_USERS
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
            <Route path={`${URL_INSTITUTIONS}`} element={<InstitutionsList />} />
            <Route path={`${URL_USERS}`} element={<UsersList />} />
            <Route path={`${URL_ATTACHMENTS}`} element={<AttachmentsList />} />
        </Route>
    ),
    {
        basename: serverContextPath.startsWith("http") ? "" : serverContextPath,
    }
);

export default function App() {
    return <RouterProvider router={router} />;
}
