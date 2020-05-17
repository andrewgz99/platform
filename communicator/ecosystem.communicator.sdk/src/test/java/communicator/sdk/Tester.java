package communicator.sdk;

import com.alibaba.fastjson.JSON;

import communicator.sdk.constant.EventDict;

public class Tester implements EventNotify {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Tester tester = new Tester();
		
		tester.startConsume();
		
		tester.startSubscribe();
		
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onEvent(Event event) {
		// TODO Auto-generated method stub
		if (EventDict.DATA == event.getEvent()) {
			System.out.println("receiving push data->" + JSON.toJSONString(event));
		}
		
		int i;
		
		i = 100;
	}
	
	private void startSubscribe() {
		Tester tester = this;
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Channel channel = ChannelFactory.openChannel(tester);
				
				String[] urls = new String[1];
				urls[0] = "https://communicator1.openfunds.live:1863";
				ChannelInformation ci = new ChannelInformation();
				ci.setParentId(10008L);
				ci.setSystemId(10074l);
				ci.setAppId(11);
				ci.setTicket("c4c673155831a1b70c452e94bf2fa286c2736e912538");
				ci.setTopicParentId(10008L);
				ci.setTopicSystemId(10000L);
				ci.setTopicMemberSystemId(10074L);
				ci.setHttpAddresses(urls);
				ci.setKey("acccf9599e35aed8df4e80a56528ad67a4aa2b145749");
				ci.setAddress("meeting_topic_10008_10000_32379739");
				ci.setWebsocketAddress("wss://communicator1.openfunds.live:1864/client");

				if (!channel.subscribe(ci)) {
					return ;
				}
				
				int count = 0;
				while (true) {
					boolean result = channel.publish("test" + count, false);
					++count;
					
					System.out.println("publish->" + result);	
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}
	
	private void startConsume() {
		Tester tester = this;
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				//one channel one address.
				Channel channel = ChannelFactory.openChannel(tester);
				
				String[] urls = new String[1];
				urls[0] = "https://communicator1.openfunds.live:1863";
				ChannelInformation ci = new ChannelInformation();
				ci.setParentId(10008L);
				ci.setSystemId(10074L);
				ci.setHttpAddresses(urls);
				ci.setKey("703b8441fa1889ed88415ba36ab7b3685f21ef888876");
				ci.setAddress("meeting_queue_10008_10074");
				ci.setWebsocketAddress("wss://communicator1.openfunds.live:1864/client");
				ci.setAppId(11);
				ci.setTicket("c4c673155831a1b70c452e94bf2fa286c2736e912538");
				if (!channel.consume(ci)) {
					return ;
				}
				
				int count = 0;
				while (true) {
					boolean result = channel.produce("test" + count, false);
					++count;
					
					System.out.println("produce->" + result);	
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}

}
