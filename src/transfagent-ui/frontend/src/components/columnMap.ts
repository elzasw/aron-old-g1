import {
    IColumnInfo
} from "jareco/dist/resizable-columns-grid";
import { ApuSourceDef, ArchDescDef, DaoDef, EntityDef, FindingAidDef, FundDef } from "../api";

export interface ColumnInfo<T> extends IColumnInfo {
    code: keyof T | "actions",
}

export const columnInfo: ColumnInfo<ApuSourceDef>[] = [
    {
        code: "actions",
        width: 80,
    },
    {
        code: "apusourceId",
        width: 120,
    },
    {
        code: "sourceType",
        width: 170,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "dataDir",
        width: 300,
    },
    {
        code: "deleted",
        width: 150,
    },
    {
        code: "reimport",
        width: 150,
    },
    {
        code: "imported",
        width: 300,
    },
]

export const cInfoDaos: ColumnInfo<DaoDef>[] = [
    {
        code: "actions",
        fixedWidth: true,
        width: 80,
    },
    {
        code: "daoId",
        width: 120,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "handle",
        width: 170,
    },
    {
        code: "apusourceId",
        width: 170,
    },
    {
        code: "state",
        width: 170,
    },
    {
        code: "transferred",
        width: 170,
    },
    {
        code: "dataDir",
        width: 170,
    },
    {
        code: "source",
        width: 170,
    },
]

export const cInfoFunds: ColumnInfo<FundDef>[] = [
    {
        code: "actions",
        fixedWidth: true,
        width: 80,
    },
    {
        code: "fundId",
        width: 120,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "deleted",
        width: 150,
    },
    {
        code: "code",
        width: 300,
    },
    {
        code: "source",
        width: 170,
    },
    {
        code: "apusourceId",
        width: 170,
    },
    {
        code: "institutionId",
        width: 170,
    },
]

export const cInfoEntities: ColumnInfo<EntityDef>[] = [
    {
        code: "actions",
        fixedWidth: true,
        width: 80,
    },
    {
        code: "entityId",
        width: 120,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "deleted",
        width: 150,
    },
    {
        code: "elzaId",
        width: 300,
    },
    {
        code: "parentId",
        width: 170,
    },
    {
        code: "apusourceId",
        width: 170,
    },
    {
        code: "entityClass",
        width: 170,
    },
    {
        code: "state",
        width: 300,
    },
]

export const cInfoAids: ColumnInfo<FindingAidDef>[] = [
    {
        code: "actions",
        fixedWidth: true,
        width: 80,
    },
    {
        code: "findingAidId",
        width: 120,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "deleted",
        width: 150,
    },
    {
        code: "code",
        width: 300,
    },
    {
        code: "apusourceId",
        width: 170,
    },
    {
        code: "institutionId",
        width: 170,
    },
]

export const archDescColumnInfo: ColumnInfo<ArchDescDef>[] = [
    {
        code: "actions",
        fixedWidth: true,
        width: 80,
    },
    {
        code: "archDescId",
        width: 120,
    },
    {
        code: "uuid",
        width: 300,
    },
    {
        code: "deleted",
        width: 150,
    },
    {
        code: "apusourceId",
        width: 170,
    },
    {
        code: "fundId",
        width: 170,
    },
]
