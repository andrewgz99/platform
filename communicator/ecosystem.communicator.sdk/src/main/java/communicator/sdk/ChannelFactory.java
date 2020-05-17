package communicator.sdk;

public class ChannelFactory {
	public static Channel openChannel(EventNotify eventNotify) {
		Channel retValue = null;
		
		if (null == eventNotify) {
			return retValue;
		}
		
		Channel newChannel = new Channel(eventNotify);
		
		retValue = newChannel;
		
		return retValue;
	}
	
	public static void closeChannel(Channel channel) {
		channel.close();	
	}
}
