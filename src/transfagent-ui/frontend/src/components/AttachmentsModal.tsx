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
import { Api, AttachmentDef } from "../api";
import { cInfoAttachments } from "./columnMap";
import { LoadingOverlay } from "./LoadingOverlay";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

interface Props {
    apuSourceId: number;
    onClose: () => void;
}

export const AttachmentsModal = ({ apuSourceId, onClose }: Props) => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [page, setPage] = useState(1);
    const [items, setItems] = useState<AttachmentDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        setLoading(true);
        Api.transfagent
            .getAttchments({
                apuSourceId,
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
    }, [page, apuSourceId]);

    const getChildId = (index: number) => items[index].attachmentId;

    const renderHeader = (props: RCGHeaderP) => (
        <DarkListHeader style={{ flexGrow: 1 }}>
            {formatMessage({ id: "attachmentColumn." + cInfoAttachments[props.columnIndex].code })}
        </DarkListHeader>
    );

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = cInfoAttachments[props.columnIndex].code;
        const isEven = props.rowIndex % 2 === 0;
        return (
            <ListItem alter={!isEven} highlighted={props.highlighted} selected={false}>
                <TableCell>{getCellContent(item, propCode)}</TableCell>
            </ListItem>
        );
    };

    const getCellContent = (item: any, propCode: string) => {
        if (propCode === "lastModified") {
            return item.lastModified && new Date(item.lastModified).toLocaleString("cs-CZ");
        }
        return item[propCode];
    };

    return (
        <>
        {loading && <LoadingOverlay messageId="operationInProgress" />}
        {/* @ts-ignore  v ModalBase props nejsou children */}
        <ModalBase
            height={Math.round(window.innerHeight * 0.9)}
            width={Math.round(window.innerWidth * 0.9)}
            title={formatMessage({ id: "attachmentsModal.title" }, { apuSourceId })}
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
                        columnCount={cInfoAttachments.length}
                        columnInfo={cInfoAttachments}
                        getItemId={getChildId}
                        rowHeight={32}
                        noItemsText={formatMessage({ id: "noItems" })}
                        getItemIndex={(itemId) =>
                            items.findIndex(({ attachmentId }) => attachmentId === itemId)
                        }
                    />
                </div>
                <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 8 }}>
                    <CancelButton onClick={onClose}>
                        {formatMessage({ id: "attachmentsModal.close" })}
                    </CancelButton>
                </div>
            </div>
        </ModalBase>
        </>
    );
};
