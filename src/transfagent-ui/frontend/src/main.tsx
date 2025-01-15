import { ModalContainer } from "jareco/dist/modal";
import ReactDOM from "react-dom/client";
import { IntlProvider } from "react-intl";
import { ThemeProvider } from "styled-components";
import App from "./App";
import "./index.css";
import locale from "./locale";
import { theme } from "./theme";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
    <ThemeProvider theme={theme}>
        <IntlProvider locale={locale.cs.locale} messages={locale.cs.messages}>
            <ModalContainer />
            <App />
        </IntlProvider>
    </ThemeProvider>
);
