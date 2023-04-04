import styled, { ThemeProvider } from 'styled-components';
import { theme } from './theme';
import { ItemsList } from "./components/ItemList";
import { IntlProvider } from "react-intl";
import locale from "./locale";

const LayoutWrapper = styled.div`
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: ${(props) => props.theme.colors.base};
    font-size: 14px;
`;

const PageWrapper = styled.div`
    display: flex;
    flex-direction: column;
    flex-grow: 1;
    overflow: auto;
    padding: 50px;
`;

function App() {
  return (
    <ThemeProvider theme={theme}>
        <IntlProvider locale={locale.cs.locale} messages={locale.cs.messages}>
        <LayoutWrapper>
            <PageWrapper>
                <ItemsList />
            </PageWrapper>
        </LayoutWrapper>
        </IntlProvider>
    </ThemeProvider>
  )
}

export default App
