package communicator.sdk;

public class RequestMessage {
	private Long parentId;
	
	private Long systemId;
	
	private Long topicParentId;
	
	private Long topicSystemId;
	
	private Long topicMemberSystemId;
	
	private String ticket;
	
	private Long id;
	
	private String action;
	
	private String address;
	
	private Integer appId;
	
	private String key;
	
	private Long destSystemId;
	
	private String type;
	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Long getSystemId() {
		return systemId;
	}

	public void setSystemId(Long systemId) {
		this.systemId = systemId;
	}

	public Long getTopicParentId() {
		return topicParentId;
	}

	public void setTopicParentId(Long topicParentId) {
		this.topicParentId = topicParentId;
	}

	public Long getTopicSystemId() {
		return topicSystemId;
	}

	public void setTopicSystemId(Long topicSystemId) {
		this.topicSystemId = topicSystemId;
	}

	public Long getTopicMemberSystemId() {
		return topicMemberSystemId;
	}

	public void setTopicMemberSystemId(Long topicMemberSystemId) {
		this.topicMemberSystemId = topicMemberSystemId;
	}

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Long getDestSystemId() {
		return destSystemId;
	}

	public void setDestSystemId(Long destSystemId) {
		this.destSystemId = destSystemId;
	}
	
	
}
