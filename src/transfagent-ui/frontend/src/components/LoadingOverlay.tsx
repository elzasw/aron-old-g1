import { useEffect, useState } from "react";
import { FormattedMessage } from "react-intl";
import styled from "styled-components";

const Backdrop = styled.div`
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
`;

const MessageBox = styled.div`
    background-color: ${(props) => props.theme.colors.base};
    color: ${(props) => props.theme.colors.c5};
    padding: 24px 40px;
    border-radius: 4px;
    font-size: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
`;

interface LoadingOverlayProps {
    messageId: string;
    delay?: number;
}

export const LoadingOverlay = ({ messageId, delay = 500 }: LoadingOverlayProps) => {
    const [showMessage, setShowMessage] = useState(delay === 0);

    useEffect(() => {
        if (delay === 0) return;
        const id = setTimeout(() => setShowMessage(true), delay);
        return () => clearTimeout(id);
    }, [delay]);

    return (
        <Backdrop>
            {showMessage && (
                <MessageBox>
                    <FormattedMessage id={messageId} />
                </MessageBox>
            )}
        </Backdrop>
    );
};
