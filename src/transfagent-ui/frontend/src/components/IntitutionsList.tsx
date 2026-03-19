import {
    ActionBar,
    // ActionButton,
    ActionsContainer,
    Button,
    CancelButton,
    DarkListHeader,
    ListContainer,
    ListItem,
    StyledInput,
} from "jareco/dist/common-ui-elements";
import { confirm } from "jareco/dist/modal";
import { ConfirmationDialogResult } from "jareco/dist/modal/ConfirmationDialog";
import { Pagination } from "jareco/dist/pagination";
import {
    ItemProps,
    HeaderProps as RCGHeaderP,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { useEffect, useState } from "react";
import { FaFileImport, FaFilter/* , FaTrashAlt */ } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import styled from "styled-components";
import { Api, InstitutionDef/* , ReimportType */ } from "../api";
import { getInstitutionsPath, getSourcesPath } from "../App";
import { ColumnInfo, institutionColumnInfo } from "./columnMap";
import { LoadingOverlay } from "./LoadingOverlay";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const InstitutionsList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [params] = useSearchParams();
    const filterFulltext = params.get("filter") || "";
    const navigate = useNavigate();

    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<InstitutionDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);
    const [downloading, setDownloading] = useState(false);

    const requestData = async () => {
        if (page >= 1) {
            setDownloading(true);
            Api.transfagent
                .getInstitutions({
                    code: filterFulltext || undefined,
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
                })
                .finally(() => setDownloading(false));
        }
    };

    useEffect(() => {
        requestData();
    }, [page, filterFulltext]);

    useEffect(() => {
        setFilterFulltextInput(filterFulltext || "");
    }, [filterFulltext])

    const getChildId = (index: number) => items[index].institutionId || 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "institutionColumn." + institutionColumnInfo[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = institutionColumnInfo[props.columnIndex].code;
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
        navigate(getInstitutionsPath());
    };

    const handleConfirmFilter = (e: React.FormEvent) => {
        e.preventDefault();
        e.stopPropagation();

        navigate(getInstitutionsPath(filterFulltextInput));
        setPage(1);
    };

    const handleFilterFulltextChange = (
        e: React.FormEvent<HTMLInputElement>
    ) => {
        setFilterFulltextInput(e.currentTarget.value);
    };

    const getCellContent = (item: any, propCode: ColumnInfo<InstitutionDef>["code"]) => {
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "apusourceId") {
            const apuId = item[propCode];
            return <Link to={getSourcesPath(apuId)}>{apuId}</Link>
        }
        return item[propCode];
    };

    const renderActions = (item: InstitutionDef) => {
        const apuId = item.apusourceId;
        return (
            <div style={{ display: "inline-flex" }}>
                {apuId && <>
                    <Button
                        onClick={() => handleReImport(apuId, item)}
                        iconOnly={true}
                        title={formatMessage({ id: "reimport" })}
                    // disabled={item.reimport === true}
                    >
                        <FaFileImport />
                    </Button>
                    {/* <Button */}
                    {/*     onClick={() => handleDelete(apuId, item)} */}
                    {/*     iconOnly={true} */}
                    {/*     title={formatMessage({ id: "delete" })} */}
                    {/*     disabled={item.deleted === true} */}
                    {/* > */}
                    {/*     <FaTrashAlt /> */}
                    {/* </Button> */}
                </>
                }
            </div>
        );
    };

    const handleReImport = (apuId: number, item: InstitutionDef) => {
        confirm(formatMessage({ id: "institution.reimport.confirm.title" }), formatMessage({ id: "institution.reimport.confirm.message" }, { id: item.institutionId }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .reimportApu({ apuId })
                    .then((res) => {
                        toast.success(formatMessage({ id: "reimport.success" }));
                        requestData();
                    })
                    .catch((err) => {
                        toast.error(formatMessage({ id: "reimport.error" }));
                        console.log("Došlo k chybě", err);
                    });
            }
        });
    };

    // const handleDelete = (apuId: number, item: InstitutionDef) => {
    //     confirm(formatMessage({ id: "fund.delete.confirm.title" }), formatMessage({ id: "fund.delete.confirm.message" }, { id: item.institutionId }), (result) => {
    //         if (result === ConfirmationDialogResult.OK) {
    //             Api.transfagent.deleteApu({ apuId })
    //                 .then((res) => {
    //                     toast.success(formatMessage({ id: "delete.success" }));
    //                     requestData();
    //                 })
    //                 .catch((err) => {
    //                     toast.error(formatMessage({ id: "delete.error" }));
    //                     console.log("Došlo k chybě", err);
    //                 });
    //         }
    //     });
    // };

    // const handleReimportAll = () => {
    //     confirm(formatMessage({ id: "fund.reimportAll.confirm.title" }), formatMessage({ id: "fund.reimportAll.confirm.message" }), (result) => {
    //         if (result === ConfirmationDialogResult.OK) {
    //             Api.transfagent.reimportAllApusOfType({ apuType: ReimportType.Fund })
    //         }
    //     });
    // }

    return (
        <>
        {downloading && <LoadingOverlay messageId="operationInProgress" />}
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
                {/* <ActionsContainer> */}
                {/*     <ActionButton onClick={handleReimportAll}> */}
                {/*         <FaFileImport /> */}
                {/*         &nbsp; */}
                {/*         <FormattedMessage id="reimportAll" /> */}
                {/*     </ActionButton> */}
                {/* </ActionsContainer> */}
            </ActionBar>
            <div id="list" style={{ height: "100%" }}>
                <ResizableColumnsGrid
                    itemCount={items.length}
                    renderHeader={renderHeader}
                    renderItem={renderItem}
                    columnCount={institutionColumnInfo.length}
                    columnInfo={institutionColumnInfo}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                    getItemIndex={(itemId) => items.findIndex(({ institutionId }) => institutionId === itemId)}
                />
            </div>
        </ListContainer>
        </>
    );
};
