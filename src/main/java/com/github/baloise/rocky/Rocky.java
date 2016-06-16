package com.github.baloise.rocky;

import static com.github.baloise.rocky.JSONBuilder.array;
import static com.github.baloise.rocky.JSONBuilder.json;
import static com.github.baloise.rocky.JSONBuilder.msg;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

public class Rocky extends Thread {
	

	final String user;
	final String password;
	final String url;
	String stopMessage = "rocky stop";
	List<Consumer<String>> handlers = new ArrayList<>();
	private Session session;
	
	public Rocky(String user, String password, String url) {
		super();
		this.user = user;
		this.password = password;
		this.url = url;
	}


	@Override
	public void run() {
		while (doRun());
		logout();
	}

	long lastPing = Long.MAX_VALUE;
	
	boolean stopped = false;
	private boolean doRun() {
		lastPing = Long.MAX_VALUE;
		try {
			final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
			ClientManager client = ClientManager.createClient();
			session = client.connectToServer(new Endpoint() {

				@Override
				public void onOpen(final Session session, EndpointConfig config) {
					log("Session "+ session.getId() + " opened");
					try {
						session.addMessageHandler(new MessageHandler.Whole<String>() {

							@Override
							public void onMessage(String message) {
								if("a[\"{\\\"msg\\\":\\\"ping\\\"}\"]".equals(message)) {
									session.getAsyncRemote().sendText("[\"{\\\"msg\\\":\\\"pong\\\"}\"]");
									lastPing = currentTimeMillis();
								}
								else if(message.contains(stopMessage)) {
									logout();
									log("received stop message");
									stopped = true;
								} else {
									for (Consumer<String> consumer : handlers) {
										consumer.accept(message);
									}
								}
							}

						});
						
						session.getBasicRemote().sendText("[\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\",\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"]");
						session.getBasicRemote().sendText(
								json("user", json("username", user))
								.field("password", json("digest", sha256(password)).field("algorithm", "sha-256"))
								.ddpMethodCall("login").buildDDP()
								
								);
						
						
						session.getBasicRemote().sendText(
								msg("sub").id().field("name", "stream-messages").field("params", array())
								.buildDDP()
								);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				@Override
				public void onClose(Session session, CloseReason closeReason) {
					log("Closed");
					super.onClose(session, closeReason);
				}
				
				@Override
				public void onError(Session session, Throwable thr) {
					System.err.println("Error");
					thr.printStackTrace();
					super.onError(session, thr);
				}
			}, cec, new URI(url));
			while (currentTimeMillis()-lastPing < 1000 * 60 * 1) {
				try {
					if(stopped) return !stopped;
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					log(e1.getLocalizedMessage());
				}
			}
			System.err.println("No ping received -> restarting");
			logout();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				int secs = 60;
				log("Waiting "+secs+" seconds");
				Thread.sleep(1000*secs);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		return !stopped;
	}

	private void logout() {
		log("Closing session");
		try {
			session.close();
		} catch (IOException e) {
		}
	}


	private void log(String s) {
		 System.out.println(new Timestamp(currentTimeMillis())+ " : "+s);
	}


	public static String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public Rocky withStopMessage(String stopMessage) {
		this.stopMessage = stopMessage;
		return this;
	}
	
	public Rocky withHandler(Consumer<String> handler) {
		handlers.add(handler);
		return this;
	}

}
