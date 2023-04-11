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
import { FaCheck, FaFileImport, FaFilter } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import styled from "styled-components";
import { Api, DaoDef } from "../api";
import { cInfoDaos } from "./columnMap";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const DaosList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [filterFulltext, setFilterFulltext] = useState("");
    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<DaoDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);

    const requestData = async () => {
        if (page >= 1) {
            Api.transfagent
                .getDaos({
                    uuid: filterFulltext || undefined,
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

    const getChildId = (index: number) => items[index].daoId || 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "daoColumn." + cInfoDaos[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = cInfoDaos[props.columnIndex].code;
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
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "transferred") {
            return item[propCode] && <FaCheck />;
        }
        return item[propCode];
    };

    const renderActions = (item: DaoDef) => {
        const daoId = item.daoId;
        return (
            <div style={{ display: "inline-flex" }}>
                {daoId && (
                    <Button
                        onClick={() => doReImport(daoId)}
                        iconOnly={true}
                        title={formatMessage({ id: "reimport" })}
                    >
                        <FaFileImport />
                    </Button>
                )}
            </div>
        );
    };

    const doReImport = (daoId: number) => {
        Api.transfagent
            .reimportDao({ daoId })
            .then((res) => {
                requestData();
            })
            .catch((err) => {
                console.log("Došlo k chybě", err);
            });
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
                    columnCount={cInfoDaos.length}
                    columnInfo={cInfoDaos}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                />
            </div>
        </ListContainer>
    );
};
