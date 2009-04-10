package net.juyantang.po;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Message {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private String path;
    
    @Persistent
	private String name;

    @Persistent
    private String data;
    
    @Persistent
    private int page;
    
    @Persistent
    @ManyToOne(fetch=FetchType.LAZY)
    private MessageGroup group;
    
    
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public MessageGroup getGroup() {
		return group;
	}
	public void setGroup(MessageGroup group) {
		this.group = group;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	
	
}
