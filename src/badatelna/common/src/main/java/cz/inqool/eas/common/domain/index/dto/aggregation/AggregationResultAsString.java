package cz.inqool.eas.common.domain.index.dto.aggregation;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO holding aggregation results parsed from ElasticSearch
 */
@Getter
@Setter
public class AggregationResultAsString extends DefaultAggregationResult {

    private String asString;

}
