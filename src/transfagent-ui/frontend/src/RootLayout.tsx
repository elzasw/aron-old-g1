import { FormattedMessage } from "react-intl";
import { Outlet } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { getAidsPath, getArchDescsPath, getDaosPath, getEntitiesPath, getFundsPath, getInstitutionsPath, getSourcesPath } from "./App";
import { GeneralContextProvider } from "./GeneralContext";
import { LayoutWrapper, NavButton, Navigation, PageWrapper } from "./styles";

export default function RootLayout() {
    return (
        <GeneralContextProvider>
            <LayoutWrapper>
                {renderNavigation()}
                <PageWrapper>
                    <Outlet />
                </PageWrapper>
                <ToastContainer theme="colored" />
            </LayoutWrapper>
        </GeneralContextProvider>
    );
}

function renderNavigation() {
    const navLeft: JSX.Element[] = [];
    navLeft.push(
        <NavButton key="sources" to={getSourcesPath()}>
            <FormattedMessage id="navigation.sources" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="daos" to={getDaosPath()}>
            <FormattedMessage id="navigation.daos" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="funds" to={getFundsPath()}>
            <FormattedMessage id="navigation.funds" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="aids" to={getAidsPath()}>
            <FormattedMessage id="navigation.aids" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="entities" to={getEntitiesPath()}>
            <FormattedMessage id="navigation.entities" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="archdescs" to={getArchDescsPath()}>
            <FormattedMessage id="navigation.archdescs" />
        </NavButton>
    );
    navLeft.push(
        <NavButton key="institutions" to={getInstitutionsPath()}>
            <FormattedMessage id="navigation.institutions" />
        </NavButton>
    );
    return <Navigation>{navLeft}</Navigation>;
}
