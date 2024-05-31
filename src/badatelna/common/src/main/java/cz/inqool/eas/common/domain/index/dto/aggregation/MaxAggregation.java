package cz.inqool.eas.common.domain.index.dto.aggregation;

import cz.inqool.eas.common.domain.index.field.IndexObjectFields;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

/**
 * A single-value metrics aggregation that calculates max value.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-metrics-max-aggregation.html">Max
 * Aggregation</a>
 */
@Getter
@Setter
public class MaxAggregation extends MetricAggregation {

    private String name;

    private String field;

    private String format;

    public MaxAggregation() {
        super(MetricAggregator.MAX);
    }

    @Builder
    public MaxAggregation(String name, String field, String format) {
        super(MetricAggregator.MAX);
        this.name = name;
        this.field = field;
        this.format = format;
    }

    @Override
    public AggregationBuilder toAggregationBuilder(IndexObjectFields indexObjectFields) {
        return AggregationBuilders.max(name).field(field).format(format);
    }
}
