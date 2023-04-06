import { ThemeProvider } from 'styled-components';
import { theme } from './theme';
import { FormattedMessage, IntlProvider } from "react-intl";
import locale from "./locale";
import { LayoutWrapper, PageWrapper, NavButton, Navigation } from './styles';

function App(){
  return (
    <ThemeProvider theme={theme}>
        <IntlProvider locale={locale.cs.locale} messages={locale.cs.messages}>
        <LayoutWrapper>
            {renderNavigation()}
            <PageWrapper>
            </PageWrapper>
        </LayoutWrapper>
        </IntlProvider>
    </ThemeProvider>
  )
}

function renderNavigation() {
    const navLeft: JSX.Element[] = [];
    navLeft.push(
        <NavButton key="sources" to="/sources">
            <FormattedMessage id="navigation.sources" />
        </NavButton>
    )
    navLeft.push(
        <NavButton key="daos" to="/daos">
            <FormattedMessage id="navigation.daos" />
        </NavButton>
    )
    navLeft.push(
        <NavButton key="funds" to="/funds">
            <FormattedMessage id="navigation.funds" />
        </NavButton>
    )   
    return (
        <Navigation>
            {navLeft}
        </Navigation>
    )
};

export default App
