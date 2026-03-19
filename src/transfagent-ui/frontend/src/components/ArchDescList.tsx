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
import { confirm, open } from "jareco/dist/modal";
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
import { ClickableLink } from "../styles";
import { Api, ArchDescDef/* , ReimportType */ } from "../api";
import { getDaosPath, getFundsPath, getInstitutionsPath, getSourcesPath } from "../App";
import { archDescColumnInfo, ColumnInfo } from "./columnMap";
import { downloadFile } from "../utils";

import { ArchDescImport } from "./ArchDescImport";
import { AttachmentsModal } from "./AttachmentsModal";
import { LoadingOverlay } from "./LoadingOverlay";
import { useGeneralContext } from "../GeneralContext";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const ArchDescList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [filterFulltext, setFilterFulltext] = useState("");
    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<ArchDescDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);
    const [downloading, setDownloading] = useState(false);

    const { institutions, institutionId, setInstitutionId } = useGeneralContext();

    const requestData = async () => {
        if (page >= 1) {
            Api.transfagent
                .getArchDescs({
                    query: filterFulltext || undefined,
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

    const getChildId = (index: number) => items[index].archDescId || 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "archdescColumn." + archDescColumnInfo[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = archDescColumnInfo[props.columnIndex].code;
        const isEven = props.rowIndex % 2 === 0;
        return (
            <ListItem
                alter={!isEven}
                highlighted={props.highlighted}
                selected={false}
                key={item.archDescId}
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

    const getCellContent = (item: any, propCode: ColumnInfo<ArchDescDef>["code"]) => {
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "apusourceId") {
            const apuId = item[propCode];
            return <Link to={getSourcesPath(apuId)}>{apuId}</Link>
        } else if (propCode === "fundId") {
            const fundId = item[propCode];
            const fundName = item["fundName"];            
            return <Link to={getFundsPath(fundId)}>{fundName?fundName:fundId}</Link>
        }  else if (propCode === "institutionId") {
            const institutionId = item[propCode];
            const institution = institutions.itemsMap[institutionId];
            return <Link to={getInstitutionsPath(institutionId)}>{institution?.name || institutionId}</Link>
        } else if (propCode === "numAttachments") {
            const numAttachments = item[propCode];
            if (numAttachments > 0) {
                const apuSourceId = item["apusourceId"];
                return <ClickableLink onClick={() => open(({ onClose }) => <AttachmentsModal apuSourceId={apuSourceId} onClose={onClose} />)}>{numAttachments}</ClickableLink>
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

    const renderActions = (item: ArchDescDef) => {
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

    const handleReImport = (apuId: number, item: ArchDescDef) => {
        confirm(formatMessage({ id: "archdesc.reimport.confirm.title" }), formatMessage({ id: "archdesc.reimport.confirm.message" }, { id: item.archDescId }), (result) => {
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

    const handleDelete = (apuId: number, item: ArchDescDef) => {
        confirm(formatMessage({ id: "archdesc.delete.confirm.title" }), formatMessage({ id: "archdesc.delete.confirm.message" }, { id: item.archDescId }), (result) => {
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

    // const handleReimportAll = () => {
    //     confirm(formatMessage({ id: "fund.reimportAll.confirm.title" }), formatMessage({ id: "fund.reimportAll.confirm.message" }), (result) => {
    //         if (result === ConfirmationDialogResult.OK) {
    //             Api.transfagent.reimportAllApusOfType({ apuType: ReimportType.Fund })
    //         }
    //     });
    // }
    

    const handleImportArchDesc = () => {        

        open(({onClose}) => {
            return <ArchDescImport onClose={onClose}/>;
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

    const handleDownloadXlsx = () => {
        setDownloading(true);
        downloadFile(
            Api.transfagent.getArchDescsXlsxRaw({query: filterFulltext || undefined,
                    instId: institutionId,
                    uuid: undefined,})
        ).finally(() => setDownloading(false));
    };

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
                        <StyledSelect onChange={handleChangeInstitution} value={institutionId}>
                            <option value={-1}>Všechny instituce</option>
                            {institutions.items.map((inst)=>{return <option key={inst.institutionId} value={inst.institutionId}>{inst.name}</option>})}                        
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
                    <ActionButton onClick={handleImportArchDesc}>
                        <FaFileImport />
                        &nbsp;
                        <FormattedMessage id="archdesc.import" />
                    </ActionButton>
                </ActionsContainer>
                <Spacer />
                <ActionsContainer>
                    <ActionButton
                        id="buttonDownloadXlsx"
                        onClick={handleDownloadXlsx}
                        >
                        {formatMessage({ id: "archdesc.downloadArchDescXlsx" })}
                    </ActionButton>
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
                    columnCount={archDescColumnInfo.length}
                    columnInfo={archDescColumnInfo}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                    getItemIndex={(itemId) => items.findIndex(({ archDescId }) => archDescId === itemId)}
                />
            </div>
        </ListContainer>
        </>
    );
};
