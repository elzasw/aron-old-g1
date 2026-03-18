import {
    ActionBar,
    ActionsContainer,
    CancelButton,
    DarkListHeader,
    ListContainer,
    ListItem,
    StyledInput,
    Button,
} from "jareco/dist/common-ui-elements";
import { Pagination } from "jareco/dist/pagination";
import {
    ItemProps,
    HeaderProps as RCGHeaderP,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { useEffect, useState } from "react";
import { FaFilter } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import { useNavigate, useSearchParams } from "react-router-dom";
import styled from "styled-components";
import { Api, AttachmentDef } from "../api";
import { getAttachmentsPath } from "../App";
import { cInfoAttachments } from "./columnMap";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const AttachmentsList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [params] = useSearchParams();
    const filterApuSourceIdRaw = params.get("apuSourceId") || "";
    const filterApuSourceId = filterApuSourceIdRaw ? Number(filterApuSourceIdRaw) : undefined;
    const navigate = useNavigate();

    const [filterInput, setFilterInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<AttachmentDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);

    const requestData = async () => {
        if (filterApuSourceId === undefined) {
            setItems([]);
            setTotalCount(0);
            return;
        }
        Api.transfagent
            .getAttchments({
                apuSourceId: filterApuSourceId,
                page: page - 1,
                pageSize: pageSize,
            })
            .then((result) => {
                if (result.items) {
                    setItems(result.items);
                    setTotalCount(result.totalCount || 0);
                }
            })
            .catch((err) => {
                console.log("Došlo k chybě", err);
            });
    };

    useEffect(() => {
        requestData();
    }, [page, filterApuSourceId]);

    useEffect(() => {
        setFilterInput(filterApuSourceIdRaw);
    }, [filterApuSourceIdRaw]);

    const getChildId = (index: number) => items[index].attachmentId;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "attachmentColumn." + cInfoAttachments[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = cInfoAttachments[props.columnIndex].code;
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

    const getCellContent = (item: any, propCode: string) => {
        if (propCode === "lastModified") {
            return item.lastModified && new Date(item.lastModified).toLocaleString("cs-CZ");
        }
        return item[propCode];
    };

    const goToPage = (pageNumber: number) => setPage(pageNumber);

    const isFilterActive = () => filterApuSourceIdRaw !== "";

    const handleClearFilter = () => {
        setFilterInput("");
        navigate(getAttachmentsPath());
    };

    const handleConfirmFilter = (e: React.FormEvent) => {
        e.preventDefault();
        e.stopPropagation();
        navigate(getAttachmentsPath(filterInput));
        setPage(1);
    };

    return (
        <ListContainer style={{ height: "100%", marginRight: 0 }}>
            <ActionBar marginBottom={10}>
                <ActionsContainer>
                    <Pagination
                        pageCount={Math.ceil(totalCount / pageSize) || 1}
                        pageFull={items.length === pageSize}
                        onChange={goToPage}
                        currentPage={page}
                    />
                </ActionsContainer>
                <ActionsContainer>
                    <form
                        onSubmit={handleConfirmFilter}
                        style={{
                            display: "flex",
                            margin: "0",
                            padding: "0",
                        }}
                    >
                        <StyledInput
                            color="dark"
                            value={filterInput}
                            placeholder={formatMessage({
                                id: "attachment.apuSourceId.placeholder",
                            })}
                            onChange={(e: React.FormEvent<HTMLInputElement>) => {
                                const val = e.currentTarget.value;
                                if (val === "" || /^\d+$/.test(val)) {
                                    setFilterInput(val);
                                }
                            }}
                            size={40}
                        />
                        <Button
                            type="submit"
                            title={formatMessage({ id: "filter" })}
                        >
                            <FaFilter />
                        </Button>
                        {isFilterActive() && (
                            <CancelButton
                                onClick={handleClearFilter}
                                title={formatMessage({ id: "filter.clear" })}
                            >
                                <FaFilter />
                                &nbsp;
                                <FormattedMessage id="filter.clear" />
                            </CancelButton>
                        )}
                    </form>
                </ActionsContainer>
            </ActionBar>
            <div id="list" style={{ height: "100%" }}>
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
        </ListContainer>
    );
};
