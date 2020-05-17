package communicator.sdk;

public class ChannelInformation {
	private String[] httpAddresses;
	
	private String key;
	
	private Long parentId;
	
	private Long systemId;
	
	private String ticket;
	
	private Integer appId;
	
	private String address;
	
	private String websocketAddress;
	
	private Object userData;
	
	private Long topicParentId;
	
	private Long topicSystemId;
	
	private Long topicMemberSystemId;
	
	private Long destSystemId;
	
	

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

	public Long getDestSystemId() {
		return destSystemId;
	}

	public void setDestSystemId(Long destSystemId) {
		this.destSystemId = destSystemId;
	}

	public Object getUserData() {
		return userData;
	}

	public void setUserData(Object userData) {
		this.userData = userData;
	}

	public String getWebsocketAddress() {
		return websocketAddress;
	}

	public void setWebsocketAddress(String websocketAddress) {
		this.websocketAddress = websocketAddress;
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

	public String getTicket() {
		return ticket;
	}

	public void setTicket(String ticket) {
		this.ticket = ticket;
	}


	public String[] getHttpAddresses() {
		return httpAddresses;
	}

	public void setHttpAddresses(String[] httpAddresses) {
		this.httpAddresses = httpAddresses;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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
}
