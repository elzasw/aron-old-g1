package cz.aron.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApuEntityTreeViewDto {
	
	private final String id;
    private final String name;
    private final String description;
    private final int depth;
    private final int pos;
    private final int childCnt;

}
