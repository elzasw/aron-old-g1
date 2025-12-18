import {
    ApiResponse,
} from "../api";

/**
 * Returns filename from request header. 
 */
const getFileName = (headers: Headers) => {
    // const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
    const filenameRegex = /filename="(?<filename>.*)"/;
    const disposition = headers.get("Content-Disposition");
    if(disposition){
        const matches = filenameRegex.exec(disposition);
        if(matches?.groups?.filename){
            return matches.groups.filename;
        }
        throw Error(`Filename not found in: ${disposition}`)
    }
    throw Error("Content-Disposition not found.")
}

export const downloadFile = async (promise: Promise<ApiResponse<Blob>>) => {
    const {raw} = await promise;

    const filename = getFileName(raw.headers)
    if(filename){
        const data = await raw.blob();

        const link = document.createElement('a');
        link.download = filename;     
        link.href = URL.createObjectURL(data);
        link.click();
        URL.revokeObjectURL(link.href);
    } else {
        throw Error("Could not read filename.")
    }
}
