import { NavLink } from "react-router-dom";
import styled from "styled-components";

export const Navigation = styled.div`
    height: 2em;
    display: flex;
    flex-shrink: 0;
    background-color: ${(props) => props.theme.colors.c5};
`;

export const LayoutWrapper = styled.div`
    display: flex;
    flex-direction: column;
    height: 100%;
    font-size: 14px;
    background-color: ${(props) => props.theme.colors.base};
`;

export const PageWrapper = styled.div`
    display: flex;
    flex-direction: column;
    flex-grow: 1;
    overflow: auto;
    padding: 50px;
`;

export const NavButton = styled(NavLink)`
    background-color: ${(props) => props.theme.colors.c5};
    color: ${(props) => props.theme.colors.base};
    display: flex;
    align-items: center;
    text-decoration: none;
    padding: 0 10px;
    flex-shrink: 0;
    &.active {
        background-color: ${(props) => props.theme.colors.accent};
        color: ${(props) => props.theme.colors.accentInvert};
    }
`;

export const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const ClickableLink = styled.a`
    color: ${(props) => props.theme.colors.accent};
    text-decoration: underline;
    cursor: pointer;
`;

export const Spacer = styled.div`
    flex: 1;
`;

export const LogoutButton = styled.a`
  background-color: ${(props) => props.theme.colors.c5};
    color: ${(props) => props.theme.colors.base};
    display: flex;
    align-items: center;
    text-decoration: none;
    padding: 0 10px;
    flex-shrink: 0;
    &.active {
        background-color: ${(props) => props.theme.colors.accent};
        color: ${(props) => props.theme.colors.accentInvert};
    }
`;