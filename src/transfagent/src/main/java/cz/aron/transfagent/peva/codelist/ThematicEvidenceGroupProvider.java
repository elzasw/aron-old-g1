package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetThematicEvidenceGroupRequest;
import cz.aron.peva2.wsdl.ListThematicEvidenceGroupRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class ThematicEvidenceGroupProvider extends CodeProvider<Peva2ThematicEvidenceGroup> {

	private static final Logger log = LoggerFactory.getLogger(ThematicEvidenceGroupProvider.class);

	private Map<String, Peva2ThematicEvidenceGroup> group2virtual = new HashMap<>();
	
	public ThematicEvidenceGroupProvider(PEvA2Connection peva2, Map<String, Peva2ThematicEvidenceGroup> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2ThematicEvidenceGroup downloadItem(String id) {
		var gtegReq = new GetThematicEvidenceGroupRequest();
		gtegReq.setId(id);
		var gtegResp = peva2.getPeva().getThematicEvidenceGroup(gtegReq);
		var teg = gtegResp.getThematicEvidenceGroup();
		log.info("Thematic evidence group downloaded, uuid={}, name={}", teg.getId(), teg.getName());
		return new Peva2ThematicEvidenceGroup(teg.getName(), teg.getCode());
	}
	
	@Override
	protected void itemAdded(Peva2ThematicEvidenceGroup item) {
		computeVirtualGroups();
	}
	
	public Peva2ThematicEvidenceGroup getVirtualGroup(String code) {
		return group2virtual.get(code);
	}

	public static ThematicEvidenceGroupProvider create(PEvA2Connection peva2) {
		Map<String, Peva2ThematicEvidenceGroup> ret = new HashMap<>();
		var ltegReq = new ListThematicEvidenceGroupRequest();
		ltegReq.setSize(1000);
		var ltegResp = peva2.getPeva().listThematicEvidenceGroup(ltegReq);
		for (var teg : ltegResp.getThematicEvidenceGroups().getThematicEvidenceGroup()) {
			ret.put(teg.getId(), new Peva2ThematicEvidenceGroup(teg.getName(), teg.getCode()));
		}
		log.info("Thematic groups downloaded.");		
		var tmp = new ThematicEvidenceGroupProvider(peva2, ret); 
		tmp.computeVirtualGroups();
		return tmp;
	}
	
	private void computeVirtualGroups() {		
		var code2virtual = new HashMap<String, Peva2ThematicEvidenceGroup>();		
		// compute virtual groups
		for(var entry:cache.entrySet()) {
			var code = entry.getValue().getCode();
			if (StringUtils.isBlank(code)||code.length()<=2) {
				continue;
			}
			var len = code.length();
			boolean isRoot = true;
			for(int i=2;i<len;i++) {
				if (code.charAt(i)!='0') {
					isRoot = false;
					break;
				}
			}
			if (isRoot) {
				code2virtual.put(code.substring(0, 2), entry.getValue());
			}			
		}
		
		var group2virtual = new HashMap<String, Peva2ThematicEvidenceGroup>();		
		// map group to virtual group
		for(var entry:cache.entrySet()) {			
			var code = entry.getValue().getCode();
			if (StringUtils.isBlank(code)||code.length()<=2) {
				continue;
			}
			var root = code2virtual.get(code.substring(0,2));
			if (root!=null) {
				group2virtual.put(entry.getKey(), root);
			}
		}
		this.group2virtual.clear();
		this.group2virtual.putAll(group2virtual);
	}
	
	
	
}
