import { Configuration } from "../generated/api/main";

interface IBasePaths {
    REST_API: string;
    RESOURCES: string;
}

// get the url of the server from global variable
// @ts-ignore
export const serverUrl:string = window.hostUrl;
// get server context path
// @ts-ignore
export const contextPath:string = window.contextPath || "";
// get paths to parts of the service
// @ts-ignore
export const basePaths:IBasePaths = window.basePaths;
// get gateway context path
// @ts-ignore
export const gatewayPath:string = window.gatewayPath || "";
// get relative or absolute URL to ADM service
// @ts-ignore
export const admUrl:string = window.admUrl;
// get relative or absolute URL to PEV service
// @ts-ignore
export const pevUrl:string = window.pevUrl;
// get relative or absolute URL to UPL service
// @ts-ignore
export const uplUrl:string = window.uplUrl;
// @ts-ignore
export const logoutUrl:string = window.logoutUrl || "";

export let urls:{
    api?: string;
    baseContextPath?: string;
    baseGatewayPath?: string;
    resources?: string;
    context?: string;
    gatewayPath?: string;
    logout?: string,
}

const baseGatewayPath = `${serverUrl}${gatewayPath}`;
const baseContextPath = `${serverUrl}${contextPath}`;

const getUrl = (url: string) =>
    url.startsWith("/") ? `${baseGatewayPath}${url}` : url;

const getContextUrl = (url: string) => 
    url.startsWith("/") ? `${baseContextPath}${url}`: url;

if(basePaths){
    urls = {
        api: getContextUrl(basePaths.REST_API),
        baseContextPath,
        baseGatewayPath,
		context: contextPath,
		gatewayPath,
        logout: logoutUrl ? getUrl(logoutUrl) : getUrl('/logout'),
        resources: getContextUrl(basePaths.RESOURCES),
    }
}
else {
    throw new Error("Some of the path values are missing");
}

export const GatewayApiConfig = new Configuration({
    basePath: baseGatewayPath,
})

export const ApiConfig = new Configuration({
    basePath: urls.api,
})

