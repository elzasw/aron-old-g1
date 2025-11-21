package cz.aron.transfagent.controller.vo;

import cz.aron.api.rest.model.FundDef;
import cz.aron.transfagent.domain.Fund;

public class FundDefVO extends FundDef {

    public FundDefVO(Fund fund) {
        setApusourceId(fund.getApuSource().getId().longValue());
        setCode(fund.getCode());
        setFundId(fund.getId().longValue());
        setInstitutionId(fund.getInstitution().getId().longValue());
        setSource(fund.getSource());
        setUuid(fund.getUuid().toString());
        setDeleted(fund.getApuSource().isDeleted());
        setName(fund.getName());
        if (fund.getNad() != null) {
        	if (fund.getSubNad()!=null) {
        		setNad(fund.getNad().toString() + "/" + fund.getSubNad().toString());		
        	} else {
        		setNad(fund.getNad().toString());
        	}
        }
    }

}
