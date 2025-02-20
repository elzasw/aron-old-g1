import {
    ActionBar,
    ActionButton,
    ActionsContainer,
    Button,
    CancelButton,
    DarkListHeader,
    ListContainer,
    ListItem,
    StyledInput,
} from "jareco/dist/common-ui-elements";
import {
    ItemProps,
    HeaderProps as RCGHeaderP,
    ResizableColumnsGrid,
} from "jareco/dist/resizable-columns-grid";
import { confirm } from "jareco/dist/modal";
import { ConfirmationDialogResult } from "jareco/dist/modal/ConfirmationDialog";
import { Pagination } from "jareco/dist/pagination";
import { Api, UserDef } from "../api";
import { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useNavigate, useSearchParams } from "react-router-dom";
import { FaFilter, FaCheck, FaPlusCircle, FaMinusCircle, FaPlus, FaTrashAlt } from "react-icons/fa";
import styled from "styled-components";
import { userColumnInfo, ColumnInfo } from "./columnMap";
import { getUsersPath } from "../App";

const TableCell = styled.div`
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
`;

export const UsersList = () => {

    const pageSize = 200;
    const { formatMessage } = useIntl();
    
    const [params] = useSearchParams();
    const filterFulltext = params.get("filter") || "";
    const navigate = useNavigate();
    
    //const [filterFulltext, setFilterFulltext] = useState("");
    const [filterFulltextInput, setFilterFulltextInput] = useState("");
    const [page, setPage] = useState(1);
    const [items, setItems] = useState<UserDef[]>([]);
    const [totalCount, setTotalCount] = useState<number>(0);    

    const requestData = async () => {
        if (page >= 1) {
            Api.transfagent
                .getUsers({
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

    useEffect(() => {
        setFilterFulltextInput(filterFulltext || "");
    }, [filterFulltext])

    const getChildId = (index: number) => /*items[index].fundId ||*/ 1;

    const renderHeader = (props: RCGHeaderP) => {
        return (
            <DarkListHeader style={{ flexGrow: 1 }}>
                {formatMessage({
                    id: "userColumn." + userColumnInfo[props.columnIndex].code,
                })}
            </DarkListHeader>
        );
    };

    const renderItem = (props: ItemProps) => {
        const item = items[props.rowIndex];
        const propCode = userColumnInfo[props.columnIndex].code;
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
    
    const getCellContent = (item: any, propCode: ColumnInfo<UserDef>["code"]) => {
        if (propCode === "actions") {
            return renderActions(item);
        } else if (propCode === "enabled") {
            return renderEnabled(item);
        }
        return item[propCode];
    };

    const renderEnabled = (item: UserDef) => {
        const en = item.enabled;
        return (            
            <div style={{ display: "inline-flex" }}>
                {en&& <>
                    <FaCheck/>
                    </>}
            </div>            
        );
    }

    const renderActions = (item: UserDef) => {
        const en = item.enabled;
        return (
            <div style={{ display: "inline-flex" }}>
                {en && <>
                    <Button
                        onClick={() => handleDisable(item)}
                        iconOnly={true}
                        title={formatMessage({ id: "disable" })}
                    // disabled={item.reimport === true}
                    >
                        <FaMinusCircle />
                    </Button>
                    </>
                }
                {!en && <>
                    <Button
                        onClick={() => handleEnable(item)}
                        iconOnly={true}
                        title={formatMessage({ id: "enable" })}
                    >
                        <FaPlusCircle />
                    </Button>
                </>
                }
                {
                    <Button
                        onClick={() => handleDelete(item)}
                        iconOnly={true}
                        title={formatMessage({ id: "delete" })}
                >
                    <FaTrashAlt />
                </Button>
                }

            </div>
        );
    };

    const handleDisable = (item: UserDef) => {
        confirm(formatMessage({ id: "users.disable.confirm.title" }), formatMessage({ id: "users.disable.confirm.message" }, { id: item.username }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .changeEnabled({
                        userEnabled : {username: item.username,
                        enabled: false}
                    })
                    .then((result) => {
                        requestData();
                    })
                    .catch((err) => {
                        console.log("Došlo k chybě", err);
                    });
            }
        });
    };

    const handleEnable = (item: UserDef) => {
        confirm(formatMessage({ id: "users.enable.confirm.title" }), formatMessage({ id: "users.enable.confirm.message" }, { id: item.username }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .changeEnabled({
                        userEnabled : {username: item.username,
                        enabled: true,}
                    })
                    .then((result) => {
                        requestData();
                    })
                    .catch((err) => {
                        console.log("Došlo k chybě", err);
                    });
            }
            
        });
    };

    const handleDelete = (item: UserDef) => {
        confirm(formatMessage({ id: "users.delete.confirm.title" }), formatMessage({ id: "users.delete.confirm.message" }, { id: item.username }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .deleteUser({
                        userDelete : {username: item.username,
                        }
                    })
                    .then((result) => {
                        requestData();
                    })
                    .catch((err) => {
                        console.log("Došlo k chybě", err);
                    });
            }
            
        });
    };

    const goToPage = (pageNumber: number) => setPage(pageNumber);

    const isFilterActive = () => filterFulltext !== "";

    const handleClearFilter = () => {
        setFilterFulltextInput("");
        navigate(getUsersPath());
    };
    
    const handleConfirmFilter = (e: React.FormEvent) => {
        e.preventDefault();
        e.stopPropagation();

        navigate(getUsersPath(filterFulltextInput));
        setPage(1);
    };

    const handleFilterFulltextChange = (
        e: React.FormEvent<HTMLInputElement>
    ) => {
        setFilterFulltextInput(e.currentTarget.value);
    };

    const handleCreateUser = (e: React.FormEvent) => {

        const username = e.currentTarget.elements.username.value;
        const hash = e.currentTarget.elements.hash.value;

        e.currentTarget.elements.username.value = '';
        e.currentTarget.elements.hash.value = '';

        e.preventDefault();
        e.stopPropagation();

        confirm(formatMessage({ id: "users.create.confirm.title" }), formatMessage({ id: "users.create.confirm.message" }, { id: username }), (result) => {
            if (result === ConfirmationDialogResult.OK) {
                Api.transfagent
                    .createUser({
                        userCreate : {username: username,
                        hash: hash,}
                    })
                    .then((result) => {
                        requestData();
                    })
                    .catch((err) => {
                        console.log("Došlo k chybě", err);
                    });
            }
            
        });
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
                    <form onSubmit={handleCreateUser}
                    style={{
                        display: "flex",
                        margin: "0",
                        padding: "0",
                    }}>
                    <StyledInput
                            color="dark"                            
                            placeholder={formatMessage({
                                id: "createuser.username.placeholder",
                            })}                            
                            size={40}
                            id="username"
                        />
                    <StyledInput
                            color="dark"
                            placeholder={formatMessage({
                                id: "createuser.hash.placeholder",
                            })}
                            size={40}
                            id="hash"
                        />
                        <Button
                            type="submit"
                            id="user.create"
                            title={formatMessage({ id: "filter" })}
                        >
                            <FaPlus />
                        </Button>
                    </form>
                </ActionsContainer>
            </ActionBar>
            <div id="list" style={{ height: "100%" }}>
                <ResizableColumnsGrid
                    itemCount={items.length}
                    renderHeader={renderHeader}
                    renderItem={renderItem}
                    columnCount={userColumnInfo.length}
                    columnInfo={userColumnInfo}
                    getItemId={getChildId}
                    rowHeight={32}
                    noItemsText={formatMessage({ id: "noItems" })}
                    getItemIndex={(itemId) => items.findIndex(({ fundId }) => fundId === itemId)}
                />
            </div>
        </ListContainer>
    );


};

