import ReactDOM from "react-dom/client";
import {
    createBrowserRouter,
    RouterProvider,
    Navigate,
} from "react-router-dom";
import "./index.css";
import { SourcesList, DaosList, FundsList } from "./components";
import { ThemeProvider } from 'styled-components';
import { theme } from './theme';
import { IntlProvider } from "react-intl";
import locale from "./locale";
import { LayoutWrapper, PageWrapper } from './styles';

//@ts-ignore
const serverContextPath = window.serverContextPath;

const router = createBrowserRouter(
    [
        {
            path: "/",
            element: <Navigate to="/source" replace/>,
        },
        {
            path: "/sources",
            element: <SourcesList />,
        },
        {
            path: "/daos",
            element: <DaosList />,
        },
        {
            path: "/funds",
            element: <FundsList />,
        },
    ],
    {
        basename: serverContextPath.startsWith("http") ? "" : serverContextPath,
    }
);

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <ThemeProvider theme={theme}>
        <IntlProvider locale={locale.cs.locale} messages={locale.cs.messages}>
        <LayoutWrapper>
            <PageWrapper>
                <RouterProvider router={router} />
            </PageWrapper>
        </LayoutWrapper>
        </IntlProvider>
    </ThemeProvider>
);