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
    Spinner,
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
import Upload from "rc-upload";
import { useEffect, useState } from "react";
import { FaCheck, FaFileImport, FaFilter, FaTrashAlt, FaUpload } from "react-icons/fa";
import { FormattedMessage, useIntl } from "react-intl";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import { Api, DaoDef, DaoStateDef } from "../api";
import { getSourcesPath } from "../App";
import { TableCell } from "../styles";
import { cInfoDaos, ColumnInfo } from "./columnMap";

export interface IRcFile extends File {
    uid: string;
}

export const DaosList = () => {
    const pageSize = 200;
    const { formatMessage } = useIntl();

    const [params] = useSearchParams();
    const filter = params.get("filter") || "";

    const [filterFulltext, setFilterFulltext] = useState(filter);
    const [filterFulltextInput, setFilterFulltextInput] = useState(filter);
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<DaoDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);
    const [operationInProgress, setOperationInProgress] =
        useState<boolean>(false);

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

    const getCellContent = (item: any, propCode: ColumnInfo<DaoDef>["code"]) => {
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "transferred") {
            return item[propCode] && <FaCheck />;
        } else if (propCode === "state") {
            return formatMessage({ id: `dao.state.${item[propCode]}` });
        } else if (propCode === "apusourceId") {
            const apuId = item[propCode];
            return <Link to={getSourcesPath(apuId)}>{apuId}</Link>
        }

        return item[propCode];
    };

    const renderActions = (item: DaoDef) => {
        const daoId = item.daoId;
        const isReady = item.state === DaoStateDef.Ready;
        const isInaccessible = item.state === DaoStateDef.Inaccessible;

        return (
            <div style={{ display: "inline-flex" }}>
                {daoId && isReady && <>
                    <Button
                        onClick={() => handleReImport(daoId, item)}
                        iconOnly={true}
                        title={formatMessage({ id: "reimport" })}
                        disabled={isInaccessible}
                    >
                        <FaFileImport />
                    </Button>
                    <Button
                        onClick={() => handleDelete(daoId, item)}
                        iconOnly={true}
                        title={formatMessage({ id: "delete" })}
                        disabled={!isReady}
                    >
                        <FaTrashAlt />
                    </Button>
                </>
                }
            </div>
        );
    };

    const handleReImport = (daoId: number, item: DaoDef) => {
        confirm(formatMessage({ id: "dao.reimport.confirm.title" }), formatMessage({ id: "dao.reimport.confirm.message" }, { id: item.daoId }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .reimportDao({ daoId })
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

    const handleDelete = (daoId: number, item: DaoDef) => {
        confirm(formatMessage({ id: "dao.delete.confirm.title" }), formatMessage({ id: "dao.delete.confirm.message" }, { id: item.daoId }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent.deleteDao({ daoId })
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

    const handleFileUpload = async (file: IRcFile, fileList: IRcFile[]) =>
        new Promise<void>((resolve, reject) => {
            setOperationInProgress(true);
            Api.transfagent
                .reimportDaos({ body: file })
                .then(() => {
                    toast.success(formatMessage({ id: "uploadFile.success" }));
                    resolve();
                })
                .catch((err) => {
                    console.log("Error:", err);
                    reject(err);
                })
                .finally(() => setOperationInProgress(false));
        });
        
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
                    <Upload
                        component={ActionButton}
                        beforeUpload={handleFileUpload}
                    // accept={".csv, .txt"}
                    >
                        <FaUpload />
                        &nbsp;
                        <FormattedMessage id="upload" />
                    </Upload>
                </ActionsContainer>
                <Spacer />
                {operationInProgress && (
                    <>
                        <Spinner />
                        &nbsp;&nbsp;
                        <FormattedMessage id="operationInProgress" />
                        &nbsp;
                    </>
                )}
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
                    getItemIndex={(itemId) => items.findIndex(({ daoId }) => daoId === itemId)}
                />
            </div>
        </ListContainer>
    );
};
