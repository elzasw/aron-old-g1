import "@typespec/rest";
import "@typespec/openapi";
import "@typespec/openapi3";

using TypeSpec.Http;
using TypeSpec.Rest;

model apuId {
    /** id of apu */
    @path apuId: int64;
}

model daoId {
    /** id of dao */
    @path daoId: int64;
}

model uuidOptional {
    /** uuid volitelný */
    @query uuid?: string;
}

model page {
    /** číslo stránky */
    @query @minValue(0) page?: int32 = 0;
}

model pageSize {
    /** velikost stránky */
    @query @minValue(1) @maxValue(1000) pageSize?: int32 = 200;
}

/** Types of apu source */
enum ApuSourceType {
    DIRECT,
    FUND,
    FINDING_AID,
    ARCH_DESCS,
    COLLECTION,
    INSTITUTION,
    ARCH_ENTITY,
}

/** Apu source definition */
model ApuSourceDef {
    apusourceId?: int64;
    sourceType: ApuSourceType;
    uuid?: string;
    dataDir?: string;
    deleted?: boolean;
    reimport?: boolean;
    imported?: offsetDateTime;
}

/** Returned list of ApuSources */
model ApuSourceResponse {
    items: ApuSourceDef[];
    totalCount: int32;
}

/** Dao states */
enum DaoStateDef {
    READY,
    ACCESSIBLE,
    INACCESSIBLE,
}

/** Dao definition */
model DaoDef {
    daoId?: int64;
    uuid?: string;
    handle?: string;
    apusourceId?: int64;
    state?: DaoStateDef;
    transferred?: boolean;
    dataDir?: string;
    source?: string;
}

/** Returned list of daos */
model DaosResponse {
    items: DaoDef[];
    totalCount: int32;
}

/** Fund definition */
model FundDef {
    fundId?: int64;
    uuid?: string;
    code?: string;
    source?: string;
    apusourceId?: int64;
    institutionId?: int64;
}

/** Returned list of funds */
model FundsResponse {
    items: FundDef[];
    totalCount: int32;
}

/** Fnding aid definition */
model FindingAidDef {
    findingAidId?: int64;
    uuid?: string;
    code?: string;
    apusourceId?: int64;
    institutionId?: int64;
}

/** Returned list of finding aids */
model FindingAidsResponse {
    items: FindingAidDef[];
    totalCount: int32;
}

/** Entity states */
enum EntityStateDef {
    ACCESSIBLE,
    AVAILABLE,
    NOT_ACCESSIBLE,
    NOT_AVAILABLE,
}

/** Entity definition */
model EntityDef {
    entityId?: int64;
    uuid?: string;
    elzaId?: string;
    parentId?: int64;
    apusourceId?: int64;
    entityClass?: string;
    state?: EntityStateDef;
}

/** Returned list of entities */
model EntitiesResponse {
    items: EntityDef[];
    totalCount: int32;
}

@tag("Transfagent")
namespace TransfagentAPI {
    /**
     * Returns list of archives
     */ 
    @route("/archives")
    @get op getArchives(
        /** name of archive */
        @query name?: string;
    ): string;

    /**
     * Returns list of apus
     */
    @route("/apus")
    @get op getApus(
        /** directory of apu */
        @query datadir?: string;
        ...uuidOptional;
        ...page;
        ...pageSize;
    ): ApuSourceResponse;

    /**
     * Mark apu for reimport in database
     */
    @route("/apus/{apuId}/reimport")
    @patch op reimportApu(
        ...apuId
    ): ApuSourceDef;

    /**
     * Return list of daos filtered by required criteria
     */
    @route("/daos")
    @get op getDaos(
        ...uuidOptional;
        ...page;
        ...pageSize;
    ): DaosResponse;

    /**
     * Mark dao for reimport in database
     */
    @route("/daos/{daoId}/reimport")
    @patch op reimportDao(
        ...daoId
    ): DaoDef;

    /**
     * Mark daos for reimport in database
     */   
    @route("/daos/reimport")
    @post op reimportDaos(
        @header contentType: "*/*";
        @body data: bytes;
    ): {
        @Http.statusCode statusCode: 200;
    };

    /**
     * Return list of funds filtered by required criteria
     */
    @route("/funds")
    @get op getFunds(
        /** code of fund */
        @query code?: string;
        ...uuidOptional;
        ...page;
        ...pageSize;
    ): FundsResponse;

    /**
     * Return list of finding aids filtered by required criteria
     */
    @route("/findingaids")
    @get op getFindingAids(
        /** code of finding aids */
        @query code?: string;
        ...uuidOptional;
        ...page;
        ...pageSize;
    ): FindingAidsResponse;

    /**
     * Return list of entities filtered by required criteria
     */
    @route("/entities")
    @get op getEntities(
        /** code of entity */
        @query code?: string;
        ...uuidOptional;
        ...page;
        ...pageSize;
    ): EntitiesResponse;

}

@service({
    title: "Transfagent Service API"
})

@server("https://localhost:3000/", "Devel server")

namespace TransfagentAPI {
}