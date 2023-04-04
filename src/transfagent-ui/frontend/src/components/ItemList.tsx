import {
    ActionBar,
    ActionButton,
    ActionsContainer,
    DarkListHeader,
    ListContainer,
    ListItem,
} from "jareco/dist/common-ui-elements";
import { Pagination } from "jareco/dist/pagination";
import {
    HeaderProps as RCGHeaderP,
    ItemProps,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { useState } from "react";
import { FaPlus } from "react-icons/fa";
import { useIntl } from "react-intl";
import styled from "styled-components";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const ordersColumns = [
    {
        code: "id",
        width: 150,
        sortable: false,
    },
    {
        code: "archiveName",
        width: 300,
        sortable: false,
    },
];

const fakeData = [
    { id: 1, archiveName: "Test1" },
    { id: 2, archiveName: "Test2" },
];

export const ItemsList = () => {
    const { formatMessage } = useIntl();
    const [items, setItems] =
        useState<{ id: number; archiveName: string }[]>(fakeData);

    const getChildId = (index: number) => items[index].id;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {ordersColumns[props.columnIndex].code}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = ordersColumns[props.columnIndex].code;
        const isEven = props.rowIndex % 2 === 0;
        return (
            <ListItem
                alter={!isEven}
                highlighted={props.highlighted}
                selected={false}
            >
                <TableCell>{getCellContent(item, propCode)}</TableCell>
            </ListItem>
        );
    };

    const getCellContent = (item: any, propCode: string) => item[propCode];
    return (
        <ListContainer style={{ height: "100%" }}>
            <ActionBar marginBottom={10}>
                <ActionsContainer>
                    <ActionButton id="button">
                        <FaPlus />
                        &nbsp;
                        {formatMessage({ id: "btn" })}
                    </ActionButton>
                </ActionsContainer>
                <ActionsContainer>
                    <Pagination pageCount={1} pageFull={true} currentPage={1} />
                </ActionsContainer>
            </ActionBar>
            <div id="list" style={{ height: "100%" }}>
                <ResizableColumnsGrid
                    itemCount={items.length}
                    renderHeader={renderHeader}
                    renderItem={renderItem}
                    columnCount={ordersColumns.length}
                    columnInfo={ordersColumns}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={"Žádné záznamy"}
                />
            </div>
        </ListContainer>
    );
};
