import {
    CancelButton,
    DarkListHeader,
    ListItem,
} from "jareco/dist/common-ui-elements";
import { ModalBase } from "jareco/dist/modal/ModalBase";
import { Pagination } from "jareco/dist/pagination";
import {
    ItemProps,
    HeaderProps as RCGHeaderP,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import styled from "styled-components";
import { Api, DaoFileDef } from "../api";
import { ColumnInfo } from "./columnMap";
import { LoadingOverlay } from "./LoadingOverlay";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

const columnInfo: ColumnInfo<DaoFileDef>[] = [
    { code: "fileId",       width: 80  },
    { code: "uuid",         width: 320 },
    { code: "state",        width: 150 },
    { code: "type",         width: 100 },
    { code: "lastModified", width: 150 },
    { code: "fileName",     width: 1500 },
];

interface Props {
    daoUuid: string;
    onClose: () => void;
}

export const DaoFileList = ({ daoUuid, onClose }: Props) => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [page, setPage] = useState(1);
    const [items, setItems] = useState<DaoFileDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setLoading(true);
        Api.transfagent
            .getDaoFiles({
                daoUuid,
                page: page - 1,
                pageSize,
            })
            .then((result) => {
                if (result.items) {
                    setItems(result.items);
                    setTotalCount(result.totalCount || 0);
                }
            })
            .catch((err) => {
                console.log("Došlo k chybě", err);
            })
            .finally(() => setLoading(false));
    }, [page, daoUuid]);

    const getChildId = (index: number) => items[index].fileId;

    const renderHeader = (props: RCGHeaderP) => (
        <DarkListHeader style={{ flexGrow: 1 }}>
            {formatMessage({ id: "daoFileColumn." + columnInfo[props.columnIndex].code })}
        </DarkListHeader>
    );

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = columnInfo[props.columnIndex].code;
        const isEven = props.rowIndex % 2 === 0;
        return (
            <ListItem alter={!isEven} highlighted={props.highlighted} selected={false}>
                <TableCell>{getCellContent(item, propCode)}</TableCell>
            </ListItem>
        );
    };

    const getCellContent = (item: any, propCode: ColumnInfo<DaoFileDef>["code"]) => {
        if (propCode === "lastModified") {
            return item.lastModified && new Date(item.lastModified).toLocaleString("cs-CZ");
        }
        return item[propCode];
    };

    // @ts-ignore  v ModalBase props nejsou children
    return (
        <>
        {loading && <LoadingOverlay messageId="operationInProgress" />}
        <ModalBase
            height={Math.round(window.innerHeight * 0.9)}
            width={Math.round(window.innerWidth * 0.9)}
            title={formatMessage({ id: "daoFileList.title" }, { uuid: daoUuid })}
            onClose={onClose}
        >
            <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
                <Pagination
                    pageCount={Math.ceil(totalCount / pageSize) || 1}
                    pageFull={items.length === pageSize}
                    onChange={setPage}
                    currentPage={page}
                />
                <div style={{ flexGrow: 1 }}>
                    <ResizableColumnsGrid
                        itemCount={items.length}
                        renderHeader={renderHeader}
                        renderItem={renderItem}
                        columnCount={columnInfo.length}
                        columnInfo={columnInfo}
                        getItemId={getChildId}
                        rowHeight={32}
                        noItemsText={formatMessage({ id: "noItems" })}
                        getItemIndex={(itemId) => items.findIndex(({ fileId }) => fileId === itemId)}
                    />
                </div>
                <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 8 }}>
                    <CancelButton onClick={onClose}>
                        {formatMessage({ id: "daoFileList.close" })}
                    </CancelButton>
                </div>
            </div>
        </ModalBase>
        </>
    );
};
