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
    navLeft.push(
        <NavButton key="aids" to="aids">
            <FormattedMessage id="navigation.aids" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="entities" to="entities">
            <FormattedMessage id="navigation.entities" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="archdescs" to="archdescs">
            <FormattedMessage id="navigation.archdescs" />
        </NavButton>
    );
    return <Navigation>{navLeft}</Navigation>;
}
