import { ApuSourceDef } from "../api";
/*
interface IColInfoObj<T> {
    code: keyof T;
    name?: string;
    width: number;
    sortable?: boolean;
}
*/
export const columnInfo = [
    {
        code: "actions",
        fixedWidth: true,
        width: 50,
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
        code: "imported",
        width: 170,
    },
]