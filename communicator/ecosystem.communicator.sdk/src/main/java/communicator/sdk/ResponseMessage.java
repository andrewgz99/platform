package communicator.sdk;

public class ResponseMessage {
	private Integer result;
	
	private String description;
	
	private Long id;
	
	private String type;
	
	private String address;
	
	private String action;
	
	private Integer appId;
	
	private String data;
	
	private Long fromSystemId;
	
	private Long parentId;
	
	private Long toSystemId;
	


	public Integer getAppId() {
		return appId;
	}

	public void setAppId(Integer appId) {
		this.appId = appId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Long getFromSystemId() {
		return fromSystemId;
	}

	public void setFromSystemId(Long fromSystemId) {
		this.fromSystemId = fromSystemId;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Long getToSystemId() {
		return toSystemId;
	}

	public void setToSystemId(Long toSystemId) {
		this.toSystemId = toSystemId;
	}

	public Integer getResult() {
		return result;
	}

	public void setResult(Integer result) {
		this.result = result;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	
	
	//"action":"ACK_CONSUME","address":"meeting_queue_10008_10074",
	//"description":"ok.","id":1,"result":0,"type":"QUEUE"}
}
