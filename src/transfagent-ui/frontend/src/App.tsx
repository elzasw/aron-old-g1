import {
    Navigate,
    Route,
    RouterProvider,
    createBrowserRouter,
    createRoutesFromElements,
} from "react-router-dom";
import RootLayout from "./RootLayout";
import { DaosList, FundsList, SourcesList } from "./components";

//@ts-ignore
const serverContextPath = window.serverContextPath;

const router = createBrowserRouter(
    createRoutesFromElements(
        <Route path="/" element={<RootLayout />}>
            <Route path="/sources" element={<SourcesList />} />
            <Route path="/" element={<Navigate replace to="/sources" />} />
            <Route path="/daos" element={<DaosList />} />
            <Route path="/funds" element={<FundsList />} />
        </Route>
    ),
    {
        basename: serverContextPath.startsWith("http") ? "" : serverContextPath,
    }
);

export default function App() {
    return <RouterProvider router={router} />;
}
