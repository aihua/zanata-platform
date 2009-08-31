package net.openl10n.api.rest.document;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.fedorahosted.flies.Namespaces;


@XmlType(name="dataHookType", namespace=Namespaces.DOCUMENT)
public class DataHook implements Resource {

	private String id;
	private long version = 1;
	
	private List<Object> extensions;
	
	public DataHook() {
	}
	
	public DataHook(String id) {
		this.id = id;
	}

	@XmlAttribute(name="id", required=true)
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	@XmlAttribute(name="version", required=true)
	public long getVersion() {
		return version;
	}
	
	public void setVersion(long version) {
		this.version = version;
	}

	@XmlAnyElement(lax=true)
	public List<Object> getExtensions() {
		if(extensions == null)
			extensions = new ArrayList<Object>();
		return extensions;
	}

	@Override
	public <T> T getExtension(Class<T> clz){
		if(extensions == null)
			return null;
		for(Object o : extensions){
			if(clz.isInstance(o))
				return clz.cast(o);
		}
		return null;
	}

	@Override
	public <T> T getOrAddExtension(Class<T> clz) {
		T ext = getExtension(clz);
		if(ext == null){
			try {
				ext = clz.newInstance();
			} catch (Throwable e) {
				throw new RuntimeException("unable to create instance", e);
			}
		}
		return ext;
	}
}
