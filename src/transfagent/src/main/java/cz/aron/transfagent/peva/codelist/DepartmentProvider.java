package cz.aron.transfagent.peva.codelist;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetDepartmentRequest;
import cz.aron.peva2.wsdl.ListDepartmentRequest;
import cz.aron.transfagent.peva.PEvA2Connection;

public class DepartmentProvider extends CodeProvider<Peva2Department> {
	
	private static final Logger log = LoggerFactory.getLogger(DepartmentProvider.class);

	public DepartmentProvider(PEvA2Connection peva2, Map<String, Peva2Department> cached) {
		super(peva2, cached);
	}

	@Override
	public Peva2Department downloadItem(String id) {
		var dReq = new GetDepartmentRequest();
		dReq.setId(id);
		var dResp = peva2.getPeva().getDepartment(dReq);
		log.info("Repartment downloaded {}, {}", id, dResp.getDepartment().getName());
		return new Peva2Department(dResp.getDepartment().getName(), dResp.getDepartment().getNumber());
	}
	
	public static DepartmentProvider create(PEvA2Connection peva2) {
		Map<String, Peva2Department> ret = new HashMap<>();
		var dReq = new ListDepartmentRequest();
		dReq.setSize(1000);
		var dResp = peva2.getPeva().listDepartment(dReq);
		for (var d : dResp.getDepartments().getDepartment()) {
			ret.put(d.getId(), new Peva2Department(d.getName(), d.getNumber()));
		}
		log.info("Departments downloaded.");		
		return new DepartmentProvider(peva2, ret);
	}
	
}
