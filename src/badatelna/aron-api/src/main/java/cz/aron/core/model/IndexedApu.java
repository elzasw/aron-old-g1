package cz.aron.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.model.types.dto.ItemType;
import cz.inqool.eas.common.domain.index.DomainIndexedObject;
import cz.inqool.eas.common.domain.index.field.Boost;
import cz.inqool.eas.common.utils.ApplicationContextUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.FOLDING_AND_TOKENIZING;
import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.FOLDING_AND_TOKENIZING_STOP;
import static cz.inqool.eas.common.domain.index.field.ES.Analyzer.SORTING;
import static cz.inqool.eas.common.domain.index.field.ES.Suffix.SORT;

/**
 * @author Lukas Jane (inQool) 29.10.2020.
 */
@Getter
@Setter
@Slf4j
@Document(indexName = "apu")
public class IndexedApu extends DomainIndexedObject<ApuEntity, ApuEntity> {

    @Boost(2)    
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = FOLDING_AND_TOKENIZING_STOP),
            otherFields = {
                    @InnerField(suffix = SORT, type = FieldType.Text, analyzer = SORTING, searchAnalyzer = SORTING, fielddata = true)
            }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = FOLDING_AND_TOKENIZING_STOP)
    private String description;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Boolean)
    private boolean containsDigitalObjects;

    @Field(type = FieldType.Nested)
    private List<Relation> rels = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> incomingRelTypeGroups = new ArrayList<>();

    @Field(type = FieldType.Keyword)
    private List<String> incomingRelTypes = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    private static class Relation {
        @Field(type = FieldType.Keyword)
        private String targetId;
        @Field(type = FieldType.Keyword)
        private String type;
        @Field(type = FieldType.Keyword)
        private List<String> groups;
        @Field(type = FieldType.Text, analyzer = FOLDING_AND_TOKENIZING)
        private String label;
        @Field(type = FieldType.Keyword)
        private String idLabel;
    }

    @Transient
    private Map<String, List<Object>> additionalDataToIndex = new HashMap<>();  //data are put here to be later inserted to dynamic ES fields

    @Override
    public void toIndexedObject(ApuEntity obj) {
        super.toIndexedObject(obj);
        name = obj.getName();
        description = obj.getDescription();
        type = obj.getType().name();
        containsDigitalObjects = obj.getDigitalObjects().size() > 0;
        incomingRelTypeGroups = obj.getIncomingRelTypeGroups();
        incomingRelTypes = obj.getIncomingRelTypes();

        TypesHolder typesHolder = ApplicationContextUtils.getApplicationContext().getBean(TypesHolder.class);
        ObjectMapper objectMapper = ApplicationContextUtils.getApplicationContext().getBean(ObjectMapper.class);
        for (ApuPart part : obj.getParts()) {
            for (ApuPartItem item : part.getItems()) {
                String value = item.getValue();
                ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                if (itemType == null) {
                    log.warn("Item type not recognized: " + item.getType());
                    continue;
                }
                if (!itemType.isIndexed()) {
                    continue;
                }
                Object data;
                switch (itemType.getType()) {
                    case STRING:
                        data = value;
                        break;
                    case ENUM:
                        data = value;
                        break;
                    case INTEGER:
                        data = Integer.valueOf(value);
                        break;
                    case APU_REF:
                        data = value;
                        break;
                    case UNITDATE:
                        try {
                            UniversalDate universalDate = objectMapper.readValue(value, UniversalDate.class);                            
                            var fromYear = universalDate.getFrom();
                            var toYear = universalDate.getTo();
                            
                            var r = new LinkedHashMap<String,String>();
                            r.put("gte", fromYear);
                            r.put("lte", toYear);
                            data = r;
                            var origL = additionalDataToIndex.get(itemType.getCode() + "~L");
                            if (origL == null||UniversalDate.isLower(fromYear,(String)origL.get(0))) {
                                additionalDataToIndex.put(itemType.getCode() + "~L", Collections.singletonList(fromYear));                                
                            }                            
                            var origH = additionalDataToIndex.get(itemType.getCode() + "~H");
                            if (origH == null||UniversalDate.isHigher(toYear,(String)origH.get(0))) {
                                additionalDataToIndex.put(itemType.getCode() + "~H", Collections.singletonList(toYear));
                            }
                        } catch (JsonProcessingException e) {
                        	log.error("Fail to read json with unit date", e);
                            throw new RuntimeException(e);
                        }
                        break;
                    case LINK:
                        data = value;
                        break;
                    default:
                        throw new RuntimeException("Unknown type");
                }
                additionalDataToIndex.computeIfAbsent(itemType.getCode(), k -> new ArrayList<>()).add(data);
                if (itemType.getType() == DataType.APU_REF && item.getTargetLabel() != null) {
                    List<String> itemTypeGroups = typesHolder.getItemGroupsForItemType(item.getType());
                    rels.add(new Relation((String) data, item.getType(), itemTypeGroups, item.getTargetLabel(), data + "|" + item.getTargetLabel()));
                    additionalDataToIndex.computeIfAbsent(itemType.getCode() + "~LABEL", k -> new ArrayList<>()).add(item.getTargetLabel());
                    additionalDataToIndex.computeIfAbsent(itemType.getCode() + "~ID~LABEL", k -> new ArrayList<>()).add(data + "|" + item.getTargetLabel());
                }
            }
        }
    }
}
