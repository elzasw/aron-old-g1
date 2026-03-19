import {
    ActionBar,
    ActionButton,
    ActionsContainer,
    Button,
    CancelButton,
    DarkListHeader,
    ListContainer,
    ListItem,
    Spacer,
    StyledInput,
    StyledSelect,
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
import { FaFileImport, FaFilter, FaTrashAlt } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import styled from "styled-components";
import { Api, FindingAidDef, ReimportType } from "../api";
import { getAttachmentsPath, getDaosPath, getInstitutionsPath, getSourcesPath } from "../App";
import { useGeneralContext } from "../GeneralContext";
import { cInfoAids, ColumnInfo } from "./columnMap";
import { downloadFile, } from "../utils";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const AidsList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [filterFulltext, setFilterFulltext] = useState("");
    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<FindingAidDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);

    const { institutions, institutionId, setInstitutionId } = useGeneralContext();

    const requestData = async () => {
        if (page >= 1) {
            Api.transfagent
                .getFindingAids({
                    code: filterFulltext || undefined,
                    instId: institutionId,
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
    }, [page, filterFulltext, institutionId]);

    const getChildId = (index: number) => items[index].findingAidId || 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "aidColumn." + cInfoAids[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = cInfoAids[props.columnIndex].code;
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
        setPage(1);
    };

    const getCellContent = (item: any, propCode: ColumnInfo<FindingAidDef>["code"]) => {
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "apusourceId") {
            const apuId = item[propCode];
            return <Link to={getSourcesPath(apuId)}>{apuId}</Link>
        } else if (propCode === "institutionId") {
            const institutionId = item[propCode];
            const institution = institutions.itemsMap[institutionId];
            return <Link to={getInstitutionsPath(institutionId)}>{institution?.name|| institutionId}</Link>
        } else if (propCode === "numAttachments") {
                    const numAttachments = item[propCode];
                    if (numAttachments > 0) {
                        const apuSourceId = item["apusourceId"];
                        return <Link to={getAttachmentsPath(apuSourceId)}>{numAttachments}</Link>
                    }
                } else if (propCode === "numDaos") {
                    const numDaos = item[propCode];
                    if (numDaos > 0 ) {
                        const apuSourceId = item["apusourceId"];
                        return <Link to={getDaosPath(apuSourceId)}>{numDaos}</Link>
                    }
                }
        return item[propCode];
    };

    const renderActions = (item: FindingAidDef) => {
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
                    <Button
                        onClick={() => handleDelete(apuId, item)}
                        iconOnly={true}
                        title={formatMessage({ id: "delete" })}
                        disabled={item.deleted === true}
                    >
                        <FaTrashAlt />
                    </Button>
                </>
                }
            </div>
        );
    };

    const handleReImport = (apuId: number, item: FindingAidDef) => {
        confirm(formatMessage({ id: "aid.reimport.confirm.title" }), formatMessage({ id: "aid.reimport.confirm.message" }, { id: item.findingAidId }), (result) => {
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

    const handleDelete = (apuId: number, item: FindingAidDef) => {
        confirm(formatMessage({ id: "aid.delete.confirm.title" }), formatMessage({ id: "aid.delete.confirm.message" }, { id: item.findingAidId }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent.deleteApu({ apuId })
                    .then((res) => {
                        toast.success(formatMessage({ id: "delete.success" }));
                        requestData();
                    })
                    .catch((err) => {
                        toast.error(formatMessage({ id: "delete.error" }));
                        console.log("Došlo k chybě", err);
                    });
            }
        });
    };

    const handleReimportAll = () => {
        confirm(formatMessage({ id: "aid.reimportAll.confirm.title" }), formatMessage({ id: "aid.reimportAll.confirm.message" }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent.reimportAllApusOfType({ apuType: ReimportType.Findingaid })
                    .then(() => {
                        toast.success(formatMessage({ id: "reimport.success" }));
                    })
                    .catch((err) => {
                        toast.error(formatMessage({ id: "reimport.error" }));
                        console.log("Došlo k chybě", err);
                    });
            }
        });
    }

    const handleChangeInstitution = (event:React.ChangeEvent<HTMLSelectElement>) => {
        const instId = parseInt(event.currentTarget.value);
        if (isNaN(instId)) {
            setInstitutionId(-1);
        } else {
            setInstitutionId(instId);
        }
        setPage(1);
    }

        const handleDownloadXlsx = () =>
            downloadFile(
                Api.transfagent.getFindingAidsXlsxRaw({code: filterFulltext || undefined,
                        instId: institutionId,
                        uuid: undefined,})
            );

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
                        <StyledSelect onChange={handleChangeInstitution} value={institutionId}>
                            <option value={-1}>Všechny instituce</option>
                            {institutions.items.map((inst)=>{return <option value={inst.institutionId}>{inst.name}</option>})}                        
                        </StyledSelect>                        
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
                <ActionsContainer>
                    <ActionButton onClick={handleReimportAll}>
                        <FaFileImport />
                        &nbsp;
                        <FormattedMessage id="reimportAll" />
                    </ActionButton>
                </ActionsContainer>
                <Spacer />
                <ActionsContainer>
                    <ActionButton
                        id="buttonDownloadXlsx"
                        onClick={handleDownloadXlsx}
                    >
                        {formatMessage({ id: "aid.downloadAidsXlsx" })}
                    </ActionButton>
                </ActionsContainer>
            </ActionBar>
            <div id="list" style={{ height: "100%" }}>
                <ResizableColumnsGrid
                    itemCount={items.length}
                    renderHeader={renderHeader}
                    renderItem={renderItem}
                    columnCount={cInfoAids.length}
                    columnInfo={cInfoAids}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                    getItemIndex={(itemId) => items.findIndex(({ findingAidId }) => findingAidId === itemId)}
                />
            </div>
        </ListContainer>
    );
};
