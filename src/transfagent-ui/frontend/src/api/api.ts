import { TransfagentApi } from "../generated/api/main";
import { ApiConfig } from "./ApiConfig";

export const Api = {
    transfagent: new TransfagentApi(ApiConfig),
};

export * from "../generated/api/main";
