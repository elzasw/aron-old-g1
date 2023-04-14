import { FormattedMessage } from "react-intl";
import { Outlet } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { LayoutWrapper, NavButton, Navigation, PageWrapper } from "./styles";

export default function RootLayout() {
    return (
        <LayoutWrapper>
            {renderNavigation()}
            <PageWrapper>
                <Outlet />
            </PageWrapper>
            <ToastContainer theme="colored" />
        </LayoutWrapper>
    );
}

function renderNavigation() {
    const navLeft: JSX.Element[] = [];
    navLeft.push(
        <NavButton key="sources" to="sources">
            <FormattedMessage id="navigation.sources" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="daos" to="daos">
            <FormattedMessage id="navigation.daos" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="funds" to="funds">
            <FormattedMessage id="navigation.funds" />
        </NavButton>
    );
    return <Navigation>{navLeft}</Navigation>;
}
