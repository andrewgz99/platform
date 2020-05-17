package communicator.sdk;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

public final class WebsocketClient {
	private EventLoopGroup group = new NioEventLoopGroup();
	private Bootstrap b = new Bootstrap();
	
	private Channel channel;
	
	public boolean send(String data) {
		boolean retValue = false;
		
		try {
			WebSocketFrame frame = new TextWebSocketFrame(data);
			
			this.channel.writeAndFlush(frame);
			
			retValue = true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return retValue;
	}
	
	public boolean open(communicator.sdk.Channel channel, String url) {
		boolean retValue = false;
		
		if (null == channel) {
			return retValue;
		}
		if (null == url || url.isEmpty()) {
			return retValue;
		}
		
		try {
			URI uri = new URI(url);
			if (null == uri.getScheme()) {
				return retValue;
			}
			if (uri.getHost() == null) {
				return retValue;
			}
			
	        String scheme = uri.getScheme();
	        final String host = uri.getHost();
	        final int port;
	        port = uri.getPort();
	        if (port <= 0) {
	            return retValue;
	        }

	        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
	            System.err.println("Only WS(S) is supported.");
	            return retValue;
	        }

	        final boolean ssl = "wss".equalsIgnoreCase(scheme);
	        final SslContext sslCtx;
	        if (ssl) {
	            sslCtx = SslContextBuilder.forClient()
	                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
	        } else {
	            sslCtx = null;
	        }

	        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final ChannelSocket handler =
                    new ChannelSocket(
                    		channel, 
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                     }
                     p.addLast(
                             new HttpClientCodec(),
                             new HttpObjectAggregator(8192),
                             WebSocketClientCompressionHandler.INSTANCE,
                             handler);
                 }
             });

            this.channel = b.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();
            
            retValue = true;

            /*
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();
                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    ch.writeAndFlush(new CloseWebSocketFrame());
                    ch.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
                    ch.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    ch.writeAndFlush(frame);
                }
            }
            */
		} catch (Exception ex) {
			ex.printStackTrace();
		}
        
        return retValue;
	}
	
	public void close() {
		try {
			group.shutdownGracefully();	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
