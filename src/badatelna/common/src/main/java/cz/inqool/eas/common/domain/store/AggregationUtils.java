package cz.inqool.eas.common.domain.store;

import cz.inqool.eas.common.domain.index.dto.aggregation.AggregationResult;
import cz.inqool.eas.common.domain.index.dto.aggregation.AggregationResultAsString;
import cz.inqool.eas.common.domain.index.dto.aggregation.DefaultAggregationResult;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.elasticsearch.search.aggregations.metrics.Cardinality;
import org.elasticsearch.search.aggregations.metrics.InternalSum;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.Min;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AggregationUtils {
    public static Map<String, List<? extends AggregationResult>> processAggregations(Aggregations aggregations) {
        if (aggregations == null || aggregations.asList().isEmpty()) {
            return null;
        }

        Map<String, List<? extends AggregationResult>> processedAggregations = new HashMap<>();
        for (org.elasticsearch.search.aggregations.Aggregation aggregation : aggregations) {
            processedAggregations.put(aggregation.getName(), processAggregation(aggregation));
        }
        return processedAggregations;
    }

    public static List<? extends AggregationResult> processAggregation(org.elasticsearch.search.aggregations.Aggregation aggregation) {
        List<DefaultAggregationResult> resultList = new ArrayList<>();

        if (aggregation instanceof MultiBucketsAggregation) {
            MultiBucketsAggregation stringAgg = (MultiBucketsAggregation) aggregation;
            for (MultiBucketsAggregation.Bucket bucket : stringAgg.getBuckets()) {
                DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
                aggregationResult.setKey(bucket.getKeyAsString());
                aggregationResult.setValue(String.valueOf(bucket.getDocCount()));
                aggregationResult.setAggregations(processAggregations(bucket.getAggregations()));
                resultList.add(aggregationResult);
            }
        } else if (aggregation instanceof InternalSum) {   //sum aggregation
            InternalSum sumAgg = (InternalSum) aggregation;
            DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
            aggregationResult.setKey(sumAgg.getName());
            aggregationResult.setValue(String.valueOf(sumAgg.getValue()));
            resultList.add(aggregationResult);
        } else if (aggregation instanceof Cardinality) {   //sum aggregation
            Cardinality cardinality = (Cardinality) aggregation;
            DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
            aggregationResult.setKey(cardinality.getName());
            aggregationResult.setValue(String.valueOf(cardinality.getValue()));
            resultList.add(aggregationResult);
        } else if (aggregation instanceof Max) {   //max aggregation
            Max max = (Max) aggregation;
            AggregationResultAsString aggregationResult = new AggregationResultAsString();
            aggregationResult.setKey(max.getName());
            aggregationResult.setValue(String.valueOf(max.getValue()));
            aggregationResult.setAsString(max.getValueAsString());
            resultList.add(aggregationResult);
        } else if (aggregation instanceof Min) {   //min aggregation
            Min min = (Min) aggregation;
            AggregationResultAsString aggregationResult = new AggregationResultAsString();
            aggregationResult.setKey(min.getName());
            aggregationResult.setValue(String.valueOf(min.getValue()));
            aggregationResult.setAsString(min.getValueAsString());
            resultList.add(aggregationResult);
        } else if (aggregation instanceof ParsedFilter) {   //filter aggregation
            ParsedFilter parsedFilterAgg = (ParsedFilter) aggregation;
            DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
            aggregationResult.setKey(parsedFilterAgg.getName());
            aggregationResult.setValue(String.valueOf(parsedFilterAgg.getDocCount()));
            aggregationResult.setAggregations(processAggregations(parsedFilterAgg.getAggregations()));
            resultList.add(aggregationResult);
        } else if (aggregation instanceof ParsedNested) {   //nested aggregation
            ParsedNested parsedNestedAgg = (ParsedNested) aggregation;
            DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
            aggregationResult.setKey(parsedNestedAgg.getName());
            aggregationResult.setAggregations(processAggregations(parsedNestedAgg.getAggregations()));
            resultList.add(aggregationResult);
        } else if (aggregation instanceof ParsedReverseNested) {   //nested aggregation
            ParsedReverseNested parsedReverseNestedAgg = (ParsedReverseNested) aggregation;
            DefaultAggregationResult aggregationResult = new DefaultAggregationResult();
            aggregationResult.setKey(parsedReverseNestedAgg.getName());
            aggregationResult.setValue(String.valueOf(parsedReverseNestedAgg.getDocCount()));
            aggregationResult.setAggregations(processAggregations(parsedReverseNestedAgg.getAggregations()));
            resultList.add(aggregationResult);
        }
        return resultList;
    }

}
