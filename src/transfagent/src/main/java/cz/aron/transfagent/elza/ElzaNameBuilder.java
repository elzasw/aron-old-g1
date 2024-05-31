package cz.aron.transfagent.elza;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.Fragment;

/**
 *  Trida pro generovani prefereovaneho jmena
 */
public class ElzaNameBuilder {
    
    private static final Map<String, SupplementBuilder> SUPPLEMENT_BUILDERS = new HashMap<>();
    private static final Map<String, SupplementBuilder> SUPPLEMENT_BUILDERS_NO_PRIV = new HashMap<>();
    
    private static final SupplementBuilder DEFAULT_SUPPLEMENT_BUILDER;
    
    public static final String NM_SUPS_PERSON[] = {
            ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_DIFF, ElzaTypes.NM_SUP_PRIV
    };    
    public static final String NM_SUPS_PARTY[] = {
            ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_GEO, ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_DIFF, ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS_DYNASTY[] = {
            ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS_EVENT[] = {
            ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_GEO, ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS_GEO[] = {
            ElzaTypes.NM_SUP_GEO, ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_CHRO,  ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS_ARTWORK[] = {
            ElzaTypes.NM_SUP_GEO, ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_CHRO,  ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS_TERM[] = {
            ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_GEO, ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_PRIV
    };
    public static final String NM_SUPS[] = {
            ElzaTypes.NM_SUP_GEN, ElzaTypes.NM_SUP_CHRO, ElzaTypes.NM_SUP_GEO
    };
    
    static {
        SUPPLEMENT_BUILDERS.put("PERSON", new SupplementBuilder(NM_SUPS_PERSON));
        SUPPLEMENT_BUILDERS.put("PARTY_GROUP", new SupplementBuilder(NM_SUPS_PARTY));
        SUPPLEMENT_BUILDERS.put("DYNASTY", new SupplementBuilder(NM_SUPS_DYNASTY));
        SUPPLEMENT_BUILDERS.put("FAMILY", new SupplementBuilderWithPrefix(NM_SUPS_DYNASTY,"rod/rodina"));
        SUPPLEMENT_BUILDERS.put("FAMILY_BRANCH", new SupplementBuilderWithPrefix(NM_SUPS_DYNASTY,"větev rodu"));
        SUPPLEMENT_BUILDERS.put("FICTIVE_DYNASTY", new SupplementBuilderWithPrefix(NM_SUPS_DYNASTY,"fiktivní rod/rodina"));
        SUPPLEMENT_BUILDERS.put("EVENT", new SupplementBuilder(NM_SUPS_EVENT));
        SUPPLEMENT_BUILDERS.put("GEO", new SupplementBuilder(NM_SUPS_GEO));
        SUPPLEMENT_BUILDERS.put("ARTWORK", new SupplementBuilder(NM_SUPS_ARTWORK));
        SUPPLEMENT_BUILDERS.put("TERM", new SupplementBuilder(NM_SUPS_TERM));
        DEFAULT_SUPPLEMENT_BUILDER = new SupplementBuilder(ElzaTypes.NM_SUPS);
                
        SUPPLEMENT_BUILDERS_NO_PRIV.put("PERSON", new SupplementBuilder(removePriv(NM_SUPS_PERSON)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("PARTY_GROUP", new SupplementBuilder(removePriv(NM_SUPS_PARTY)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("DYNASTY", new SupplementBuilder(removePriv(NM_SUPS_DYNASTY)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("FAMILY", new SupplementBuilderWithPrefix(removePriv(NM_SUPS_DYNASTY),"rod/rodina"));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("FAMILY_BRANCH", new SupplementBuilderWithPrefix(removePriv(NM_SUPS_DYNASTY),"větev rodu"));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("FICTIVE_DYNASTY", new SupplementBuilderWithPrefix(removePriv(NM_SUPS_DYNASTY),"fiktivní rod/rodina"));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("EVENT", new SupplementBuilder(removePriv(NM_SUPS_EVENT)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("GEO", new SupplementBuilder(removePriv(NM_SUPS_GEO)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("ARTWORK", new SupplementBuilder(removePriv(NM_SUPS_ARTWORK)));
        SUPPLEMENT_BUILDERS_NO_PRIV.put("TERM", new SupplementBuilder(removePriv(NM_SUPS_TERM)));
    }
    
	private static String[] removePriv(String[] orig) {
		return Arrays.stream(orig).filter(i -> !ElzaTypes.NM_SUP_PRIV.equals(i)).toArray(String[]::new);
	}

    private final ApTypeService typeService;

    public ElzaNameBuilder(ApTypeService typeService) {
        this.typeService = typeService;
    }
    
    /**
     * Vytvori plny nazev entity
     * @param frg
     * @param entityClass
     * @param noPriv true - nepouzije NM_SUP_PRIV, false - pouzije ho
     * @return String
     */
    public String createFullName(Fragment frg, String entityClass, boolean noPriv) {
        var sb = new StringBuilder();
        sb.append(ElzaXmlReader.getStringType(frg, ElzaTypes.NM_MAIN));
        
        var minor = ElzaXmlReader.getStringsType(frg, ElzaTypes.NM_MINOR, ", ");
        if(StringUtils.isNotEmpty(minor)) {
            sb.append(", ").append(minor);
        }
        
        var sbTitules = new StringBuilder();
        
        var degreePre = ElzaXmlReader.getStringType(frg, ElzaTypes.NM_DEGREE_PRE);     
        if(StringUtils.isNotEmpty(degreePre)) {
            sbTitules.append(degreePre);
        }
        var degreePost = ElzaXmlReader.getStringType(frg, ElzaTypes.NM_DEGREE_POST);
        if(StringUtils.isNotBlank(degreePost)) {
            sbTitules.append(degreePost);
        }
        if(sbTitules.length()>0) {
            sb.append(" ");
            sb.append(String.join(", ", sbTitules));
        }
        
        Map<String,SupplementBuilder> builders;
        if (noPriv) {
        	builders = SUPPLEMENT_BUILDERS_NO_PRIV;
        } else {
        	builders = SUPPLEMENT_BUILDERS;
        }
        
        var supplementBuilder = builders.get(entityClass);
        if (supplementBuilder == null) {
            var parentEntityClass = typeService.getParentCode(entityClass);
            if (parentEntityClass == null) {
                supplementBuilder = DEFAULT_SUPPLEMENT_BUILDER;
            } else {
                supplementBuilder = builders.get(parentEntityClass);
                if (supplementBuilder == null) {
                    supplementBuilder = DEFAULT_SUPPLEMENT_BUILDER;
                }
            }
        }
        
        supplementBuilder.addSuplement(frg, sb);        
        return sb.toString();
    }
    
    private static class SupplementBuilder {
        
        private final Map<String,Integer> types;

        public SupplementBuilder(String[] orderedTypes) {            
            types = new HashMap<String, Integer>();
            for(int i=0;i<orderedTypes.length;i++) {
                types.put(orderedTypes[i],i);
            }            
        }
        
        public void addSuplement(Fragment frg, StringBuilder sb) {            
            var additions = getTypes(frg, null);     
            if(additions.size()>0) {
                sb.append(" (");
                sb.append(String.join(" : ", additions));
                sb.append(")");
            }            
        }

        protected Collection<String> getTypes(Fragment frg, String prefix) {
            var result = new TreeMap<Integer, String>();
            if (prefix != null) {
                result.put(-1, prefix);
            }
            for (DescriptionItem item : frg.getDdOrDoOrDp()) {
                var position = types.get(item.getT());
                if (position != null) {
                    if (item instanceof DescriptionItemString) {
                        var dis = (DescriptionItemString) item;
                        result.put(position, dis.getV());
                    } else {
                        throw new RuntimeException(
                                "Failed to extract String value from: " + item.getT() + ", real type is: " + item);
                    }
                }
            }
            return result.values();
        }

    }
        
    private static class SupplementBuilderWithPrefix extends SupplementBuilder {

        private final String prefix;

        public SupplementBuilderWithPrefix(String[] orderedTypes, String prefix) {
            super(orderedTypes);
            this.prefix = prefix;
        }

        public void addSuplement(Fragment frg, StringBuilder sb) {
            var additions = getTypes(frg, prefix);
            if (additions.size() > 0) {
                sb.append(" (");
                sb.append(String.join(" : ", additions));
                sb.append(")");
            }
        }

    }
    
}
