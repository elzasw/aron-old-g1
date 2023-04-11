import {
    ActionBar,
    ActionsContainer,
    Button,
    CancelButton,
    DarkListHeader,
    ListContainer,
    ListItem,
    StyledInput,
} from "jareco/dist/common-ui-elements";
import { Pagination } from "jareco/dist/pagination";
import {
    ItemProps,
    HeaderProps as RCGHeaderP,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { useEffect, useState } from "react";
import { FaCheck, FaFileImport, FaFilter, FaTrashAlt } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import styled from "styled-components";
import { Api, ApuSourceDef } from "../api";
import { columnInfo } from "./columnMap";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const SourcesList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [filterFulltext, setFilterFulltext] = useState("");
    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<ApuSourceDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);

    const requestData = async () => {
        if (page >= 1) {
            Api.transfagent
                .getApus({
                    datadir: filterFulltext || undefined,
                    uuid: undefined,
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
        }
    };

    useEffect(() => {
        requestData();
    }, [page, filterFulltext]);

    const getChildId = (index: number) => items[index].apusourceId || 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "apuColumn." + columnInfo[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = columnInfo[props.columnIndex].code;
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

    const goToPage = (pageNumber: number) => setPage(pageNumber);

    const isFilterActive = () => filterFulltext !== "";

    const handleClearFilter = () => {
        setFilterFulltextInput("");
        setFilterFulltext("");
    };

    const handleConfirmFilter = (e: React.FormEvent) => {
        e.preventDefault();
        e.stopPropagation();
        setFilterFulltext(filterFulltextInput);
        setPage(1);
    };

    const handleFilterFulltextChange = (
        e: React.FormEvent<HTMLInputElement>
    ) => {
        setFilterFulltextInput(e.currentTarget.value);
    };

    const getCellContent = (item: any, propCode: string) => {
        if (propCode === "imported") {
            return (
                item.imported && new Date(item.imported).toLocaleString("cs-CZ")
            );
        } else if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "deleted" || propCode === "reimport") {
            return item[propCode] && <FaCheck />;
        }
        return item[propCode];
    };

    const renderActions = (item: ApuSourceDef) => {
        const apuId = item.apusourceId;
        return (
            <div style={{ display: "inline-flex" }}>
                {apuId && (
                    <Button
                        onClick={() => doReImport(apuId)}
                        iconOnly={true}
                        title={formatMessage({ id: "reimport" })}
                        disabled={item.reimport === true}
                    >
                        <FaFileImport />
                    </Button>
                )}
                {apuId && (
                    <Button
                        onClick={() => doDelete(apuId)}
                        iconOnly={true}
                        title={formatMessage({ id: "delete" })}
                        disabled={true /*item.deleted === true*/}
                    >
                        <FaTrashAlt />
                    </Button>
                )}
            </div>
        );
    };

    const doReImport = (apuId: number) => {
        Api.transfagent
            .reimportApu({ apuId })
            .then((res) => {
                requestData();
            })
            .catch((err) => {
                console.log("Došlo k chybě", err);
            });
    };

    const doDelete = (apuId: number) => {
        console.log("DELETE action");
    };

    return (
        <ListContainer style={{ height: "100%" }}>
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
                            value={filterFulltextInput}
                            placeholder={formatMessage({
                                id: "filter.placeholder",
                            })}
                            onChange={handleFilterFulltextChange}
                            size={40}
                        />
                        <Button
                            type="submit"
                            id="order.filter"
                            title={formatMessage({ id: "filter" })}
                        >
                            <FaFilter />
                        </Button>
                        {isFilterActive() && (
                            <CancelButton
                                onClick={handleClearFilter}
                                id="order.filter.clear"
                                title={formatMessage({
                                    id: "filter.clear",
                                })}
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
                    columnCount={columnInfo.length}
                    columnInfo={columnInfo}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                />
            </div>
        </ListContainer>
    );
};
