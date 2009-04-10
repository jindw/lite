package net.juyantang.po;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class MessageGroup {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
	private Long id;
    @Persistent
	private String prefix;
    @Persistent
	private String admin;
    @Persistent
	private String template;
    @Persistent
    private int pageSize = 20;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String name) {
		this.prefix = name;
	}
	public String getAdmin() {
		return admin;
	}
	public void setAdmin(String admin) {
		this.admin = admin;
	}
	public String getTemplate() {
		return template;
	}
	public void setTemplate(String template) {
		this.template = template;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
}
