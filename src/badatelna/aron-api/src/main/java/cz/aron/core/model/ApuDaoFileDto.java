package cz.aron.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApuDaoFileDto {
	
    private final String apuId;
    private final String daoId;
    private final String fileId;

}
