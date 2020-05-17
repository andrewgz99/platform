package communicator.sdk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.InitializingBean;

import com.alibaba.fastjson.JSON;

import communicator.sdk.constant.AddressDict;
import communicator.sdk.constant.EventDict;
import communicator.sdk.constant.MessageDict;
import ecosystem.common.errorcode.ErrorCode;
import ecosystem.common.vo.BaseVO;

public class Channel {
	//data notify.
	private EventNotify eventNotify;
	
	//channel information.
	private ChannelInformation channelInformation;

	//websocket client.
	private WebsocketClient client;
	
	//packet id counter.
	private AtomicLong idCounter = new AtomicLong(0L);
	
	//if channel is ready.
	private AtomicBoolean ready = new AtomicBoolean(false);
	
	//channel address type: queue or topic.
	private String addressType;
	
	//send http client.
	private CloseableHttpClient httpClient;
	
	private Thread restartThread;
	
	private Boolean stopped = false;
	
	public Channel(EventNotify eventNotify) {
		if (null == eventNotify) {
			return ;
		}
		
		this.eventNotify = eventNotify;
		
		int timeout = 15;
		RequestConfig config = RequestConfig.custom()
		  .setConnectTimeout(timeout * 1000)
		  .setConnectionRequestTimeout(timeout * 1000)
		  .setSocketTimeout(timeout * 1000).build();
		httpClient = 
		  HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}
	
	public void onReceive(String message) {
		try {
			ResponseMessage responseMessage = JSON.parseObject(message, ResponseMessage.class);
			
			if (MessageDict.ACK_CONSUME.contentEquals(responseMessage.getAction())) {
				//consume ack.
				if (0 != responseMessage.getResult()) {
					onError();
					
					return ;
				}
				
				ready.set(true);
				
				return ;
			} else if (MessageDict.ACK_SUBSCRIBE.contentEquals(responseMessage.getAction())) {
				//subscribe ack.
				if (0 != responseMessage.getResult()) {
					onError();
					
					return ;
				}
				
				ready.set(true);
				
				return ;
			} else if (MessageDict.PUSH.contentEquals(responseMessage.getAction())) {
				//receiving push data.
				//{"action":"PUSH","address":"meeting_queue_10008_10074","appId":11,
				//"data":"test","durable":false,"fromSystemId":10074,"id":2,
				//"key":"10008_10074_QUEUE_meeting_queue_10008_10074_2","length":240,
				//"parentId":10008,"toSystemId":10074,"type":"QUEUE"}
				if (AddressDict.QUEUE.contentEquals(responseMessage.getType())) {
					//ack queue push.
					
					RequestMessage requestMessage = new RequestMessage();
					requestMessage.setParentId(this.channelInformation.getParentId());
					requestMessage.setSystemId(this.channelInformation.getSystemId());
					requestMessage.setTicket(this.channelInformation.getTicket());
					requestMessage.setType(AddressDict.QUEUE);
					requestMessage.setAddress(this.channelInformation.getAddress());
					requestMessage.setId(responseMessage.getId());
					requestMessage.setAppId(responseMessage.getAppId());
					requestMessage.setAction(MessageDict.ACK_PUSH);
					requestMessage.setDestSystemId(responseMessage.getFromSystemId());
					
					this.client.send(JSON.toJSONString(requestMessage));
					
					//notify data.
					Event notifyEvent = new Event();
					notifyEvent.setEvent(EventDict.DATA);
					notifyEvent.setAction(responseMessage.getAction());
					notifyEvent.setType(responseMessage.getType());
					notifyEvent.setAddress(responseMessage.getAddress());
					notifyEvent.setId(responseMessage.getId());
					notifyEvent.setAppId(responseMessage.getAppId());
					notifyEvent.setData(responseMessage.getData());
					notifyEvent.setUserData(this.channelInformation.getUserData());
					
					this.eventNotify.onEvent(notifyEvent);
				} else if (AddressDict.TOPIC.contentEquals(responseMessage.getType())) {
					//notify data.
					Event notifyEvent = new Event();
					notifyEvent.setEvent(EventDict.DATA);
					notifyEvent.setAction(responseMessage.getAction());
					notifyEvent.setType(responseMessage.getType());
					notifyEvent.setAddress(responseMessage.getAddress());
					notifyEvent.setId(responseMessage.getId());
					notifyEvent.setAppId(responseMessage.getAppId());
					notifyEvent.setData(responseMessage.getData());
					notifyEvent.setUserData(this.channelInformation.getUserData());
					
					this.eventNotify.onEvent(notifyEvent);
				}
				
				return ;
			}
			else {
				int i;
				
				i = 100;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private boolean isReady() {
		return ready.get();
	}
	
	public boolean publish(String data, boolean durable) {
		boolean retValue = false;
		
		if (!AddressDict.TOPIC.contentEquals(this.addressType)) {
			return retValue;
		}
		
		if (!this.isReady()) {
			return retValue;
		}
		
        boolean success = true;
        try {
            List<NameValuePair> form = new ArrayList<>();

            form.add(new BasicNameValuePair("parentId",
            		this.channelInformation.getParentId().toString()));
            form.add(new BasicNameValuePair("systemId",
            		this.channelInformation.getSystemId().toString()));
            form.add(new BasicNameValuePair("ticket",
            		this.channelInformation.getTicket()));
            form.add(new BasicNameValuePair("topicParentId",
            		this.channelInformation.getTopicParentId().toString()));
            form.add(new BasicNameValuePair("topicSystemId",
            		this.channelInformation.getTopicSystemId().toString()));
            form.add(new BasicNameValuePair("topicMemberSystemId",
            		this.channelInformation.getTopicMemberSystemId().toString()));
            form.add(new BasicNameValuePair("id",
            		this.idCounter.incrementAndGet() + ""));
            form.add(new BasicNameValuePair("destSystemId",
            		this.channelInformation.getTopicMemberSystemId().toString()));
            form.add(new BasicNameValuePair("type",
            		AddressDict.TOPIC));
            form.add(new BasicNameValuePair("address",
            		this.channelInformation.getAddress()));
            form.add(new BasicNameValuePair("appId",
            		this.channelInformation.getAppId().toString()));
            form.add(new BasicNameValuePair("data",
            		data));
            form.add(new BasicNameValuePair("durable",
            		durable + ""));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

            for (int i = 0; i < this.channelInformation.getHttpAddresses().length; ++i) {
            	String url = this.channelInformation.getHttpAddresses()[i] + "/client/send";
                
        		BaseVO baseVO = null;
                HttpPost httpPost = new HttpPost(url);
                httpPost.setEntity(entity);

                // Create a custom response handler
                ResponseHandler<BaseVO> responseHandler = new ResponseHandler<BaseVO>() {	
                    @Override
                    public BaseVO handleResponse(
                            final HttpResponse response) throws ClientProtocolException, IOException {
                        int status = response.getStatusLine().getStatusCode();
                        if (status == 200) {
                            String responseData = EntityUtils.toString(response.getEntity(), "UTF-8");
                            
                            BaseVO baseVO = 
                            		JSON.parseObject(responseData, BaseVO.class);
                            return baseVO;
                        } else {
                            throw new ClientProtocolException("Unexpected response status: " + status);
                        }
                    }
                };
                
                baseVO = httpClient.execute(httpPost, responseHandler);
                if (null == baseVO || 0 != baseVO.getResult()) {
                	success = false;
                }
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        
        retValue = success;
        
		return retValue;
	}
	
	public boolean produce(String data, boolean durable) {
		boolean retValue = false;
		
		if (!AddressDict.QUEUE.contentEquals(this.addressType)) {
			return retValue;
		}
		
		if (!this.isReady()) {
			return retValue;
		}
		
    	BaseVO baseVO = null;
        try {
            List<NameValuePair> form = new ArrayList<>();

            form.add(new BasicNameValuePair("parentId",
            		this.channelInformation.getParentId().toString()));
            form.add(new BasicNameValuePair("systemId",
            		this.channelInformation.getSystemId().toString()));
            form.add(new BasicNameValuePair("ticket",
            		this.channelInformation.getTicket()));
            form.add(new BasicNameValuePair("id",
            		this.idCounter.incrementAndGet() + ""));
            form.add(new BasicNameValuePair("destSystemId",
            		this.channelInformation.getSystemId().toString()));
            form.add(new BasicNameValuePair("type",
            		this.addressType));
            form.add(new BasicNameValuePair("address",
            		this.channelInformation.getAddress()));
            form.add(new BasicNameValuePair("appId",
            		this.channelInformation.getAppId().toString()));
            form.add(new BasicNameValuePair("data",
            		data));
            form.add(new BasicNameValuePair("durable",
            		durable + ""));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

            String url = this.channelInformation.getHttpAddresses()[0] + "/client/send";
            
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);

            // Create a custom response handler
            ResponseHandler<BaseVO> responseHandler = new ResponseHandler<BaseVO>() {            	
                @Override
                public BaseVO handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status == 200) {
                        String responseData = EntityUtils.toString(response.getEntity(), "UTF-8");
                        
                        BaseVO baseVO = 
                        		JSON.parseObject(responseData, BaseVO.class);
                        return baseVO;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            
            baseVO = httpClient.execute(httpPost, responseHandler);
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
        
        if (null == baseVO || baseVO.getResult() != ErrorCode.OK.getCode()) {
        	return retValue;
        }
        
        retValue = true;
		
		return retValue;
	}
	
	public boolean subscribe(ChannelInformation ci) {
		boolean retValue = false;
		
		if (null != client) {
			return retValue;
		}

		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setParentId(ci.getParentId());
		requestMessage.setSystemId(ci.getSystemId());
		requestMessage.setTicket(ci.getTicket());
		requestMessage.setId(idCounter.incrementAndGet());
		requestMessage.setAction(MessageDict.SUBSCRIBE);
		requestMessage.setAddress(ci.getAddress());
		requestMessage.setAppId(ci.getAppId());
		requestMessage.setKey(ci.getKey());
		requestMessage.setTopicParentId(ci.getTopicParentId());     
		requestMessage.setTopicSystemId(ci.getTopicSystemId());
		requestMessage.setTopicMemberSystemId(ci.getTopicMemberSystemId());
		requestMessage.setDestSystemId(ci.getTopicMemberSystemId());
		
		this.channelInformation = ci;
		
		this.addressType = AddressDict.TOPIC;
		
		this.client = new WebsocketClient();
		if (!this.client.open(this, ci.getWebsocketAddress())) {
			onError();
			
			return retValue;
		}
		//requestMessage.setDestSystemId();
		//send consume message.
		this.client.send(JSON.toJSONString(requestMessage));
		
		try {
			boolean finished = false;
			int count = 0;
			while (!finished) {
				Thread.sleep(1000 * 1);
				if (count > 15) {
					finished = true;
				}
				
				++count;
				
				if (this.isReady()) {
					finished = true;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (isReady()) {
			retValue = true;
		}
		
		return retValue;
	}
	
	public boolean unsubscribe() {
		boolean retValue = false;
		
		if (null == client) {
			return retValue;
		}
		
		if (!AddressDict.TOPIC.contentEquals(this.addressType)) {
			return retValue;
		}
		
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setParentId(channelInformation.getParentId());
		requestMessage.setSystemId(channelInformation.getSystemId());
		requestMessage.setTicket(channelInformation.getTicket());
		requestMessage.setId(idCounter.incrementAndGet());
		requestMessage.setAction(MessageDict.UNCONSUME);
		requestMessage.setAddress(channelInformation.getAddress());
		requestMessage.setAppId(channelInformation.getAppId());
		requestMessage.setKey(channelInformation.getKey());
		requestMessage.setDestSystemId(channelInformation.getTopicMemberSystemId());
		retValue = this.client.send(JSON.toJSONString(requestMessage));
		
		return retValue;
	}
	
	public boolean consume(ChannelInformation ci) {
		boolean retValue = false;
		
		if (null != client) {
			return retValue;
		}
		
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setParentId(ci.getParentId());
		requestMessage.setSystemId(ci.getSystemId());
		requestMessage.setTicket(ci.getTicket());
		requestMessage.setId(idCounter.incrementAndGet());
		requestMessage.setAction(MessageDict.CONSUME);
		requestMessage.setAddress(ci.getAddress());
		requestMessage.setAppId(ci.getAppId());
		requestMessage.setKey(ci.getKey());
		
		this.channelInformation = ci;
		
		this.addressType = AddressDict.QUEUE;
		
		this.client = new WebsocketClient();
		if (!this.client.open(this, ci.getWebsocketAddress())) {
			onError();
			
			return retValue;
		}
		//requestMessage.setDestSystemId();
		//send consume message.
		this.client.send(JSON.toJSONString(requestMessage));
		
		try {
			boolean finished = false;
			int count = 0;
			while (!finished) {
				Thread.sleep(1000 * 1);
				if (count > 15) {
					finished = true;
				}
				
				++count;
				
				if (this.isReady()) {
					finished = true;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (isReady()) {
			retValue = true;
		}

		return retValue;
	}
	
	public boolean unconsume(String queue) {
		boolean retValue = false;
		
		if (null == client) {
			return retValue;
		}
		
		if (!AddressDict.QUEUE.contentEquals(this.addressType)) {
			return retValue;
		}
		
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setParentId(channelInformation.getParentId());
		requestMessage.setSystemId(channelInformation.getSystemId());
		requestMessage.setTicket(channelInformation.getTicket());
		requestMessage.setId(idCounter.incrementAndGet());
		requestMessage.setAction(MessageDict.UNCONSUME);
		requestMessage.setAddress(channelInformation.getAddress());
		requestMessage.setAppId(channelInformation.getAppId());
		requestMessage.setKey(channelInformation.getKey());
		requestMessage.setDestSystemId(channelInformation.getSystemId());
		retValue = this.client.send(JSON.toJSONString(requestMessage));
		/*
		 *     const requestMessage = {
      parentId: this._authenticatedInfo.parentId, 
      systemId: this._authenticatedInfo.systemId, 
      ticket: this._authenticatedInfo.ticket, 
      id: ++this._idCounter, 
      action: channelInfo.type == Communicator.TYPE.QUEUE ?
         Communicator.MESSAGE.UNCONSUME : Communicator.MESSAGE.UNSUBSCRIBE, 
      destSystemId: channelInfo.destSystemId,
      appId: this._authenticatedInfo.appId, 
      address: channelInfo.address
    };
    socketInfo.socket.send(JSON.stringify(requestMessage));
		 */
		
		return retValue;
	}
	
	public void close() {
		this.stopped = true;
		
		this.client.close();
		this.client = null;
	}
	
	public void onError() {
		if (stopped) {
			return ;
		}
		
		if (null != restartThread) {
			return ;
		}
		
		ready.set(false);
		
		restartThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				boolean finished = false;
				while (!finished) {
					//try to restart channel.
					client.close();
					client = null;
					
					if (AddressDict.TOPIC.contentEquals(addressType)) {
						if (subscribe(channelInformation)) {
							finished = true;
							
							restartThread = null;
						}
					} else if (AddressDict.QUEUE.contentEquals(addressType)) {
						if (consume(channelInformation)) {
							finished = true;
							
							restartThread = null;
						}
					}
					
					try {
						Thread.sleep(1000 * 6);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		});
		
		restartThread.start();
	}
}
