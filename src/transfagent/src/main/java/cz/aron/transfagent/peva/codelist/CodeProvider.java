package cz.aron.transfagent.peva.codelist;

import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.transfagent.peva.PEvA2Connection;
import cz.aron.transfagent.peva.Peva2Constants;

public abstract class CodeProvider<T> {
	
	private static final Logger log = LoggerFactory.getLogger(CodeProvider.class);

	protected final PEvA2Connection peva2;

	protected final Map<String, T> cache;

	public CodeProvider(PEvA2Connection peva2, Map<String, T> cached) {
		this.peva2 = peva2;
		this.cache = cached;
	}

	public synchronized T getItemById(String id) {
		var cached = cache.get(id);
		if (cached != null || cache.containsKey(id)) {
			return cached;
		}

		try {
			T item = downloadItem(id);
			cache.put(id, item);
			itemAdded(item);
			return item;
		} catch (SOAPFaultException sfEx) {
			if (Peva2Constants.DELETED_OBJECT.equals(sfEx.getMessage())) {
				log.warn("Required object in Deleted state, {}", id);
				cache.put(id, null);
			} else if (Peva2Constants.MISSING_OBJECT.equals(sfEx.getMessage())) {
				log.warn("Required Missing object {}", id);
				cache.put(id, null);
			}
			return null;
		}
	}

	public abstract T downloadItem(String id);

	protected void itemAdded(T item) {}
	
}
