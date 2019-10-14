// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

// This application uses the Azure IoT Hub device SDK for Java
// For samples see: https://github.com/Azure/azure-iot-sdk-java/tree/master/device/iot-device-samples

package com.github.mayemsft.iot.opcda.connector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openscada.opc.lib.da.Item;

import com.github.mayemsft.iot.opcda.connector.OPCDAItemDataReader.DataItem;
import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

public class SendEvent extends Observable {

	private String connString ;

	// Using the MQTT protocol to connect to IoT Hub
	private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
	private DeviceClient client;
	private OPCDAItemDataReader dataReader;
	private final long interval;
	private static final Map<String, DataItem> valueMap = new HashMap<String, DataItem>();
	private Observer observer;

	private ExecutorService executor;

	private Observer eventObserver;

	public SendEvent(String connString, OPCDAItemDataReader dataReader, long interval, Observer eventObserver) throws IllegalArgumentException, URISyntaxException {
		super();
		this.connString = connString;
		this.dataReader = dataReader;
		this.eventObserver = eventObserver;
		this.client = new DeviceClient(this.connString, this.protocol);
		this.interval = interval;
		this.addObserver(eventObserver);
	}
	
	private static boolean isNumeric(String str) {
		  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
//	private class EventCallback implements IotHubEventCallback {
//		public void execute(IotHubStatusCode status, Object context) {
//			System.out.println("IoT Hub responded to message with status: " + status.name());
//			
//			if (context != null) {
//				synchronized (context) {
//					context.notify();
//				}
//			}
//			SendEvent.this.notifyObservers(((SendContext)context).getMsg());
//			SendEvent.this.setChanged();
//		}
//	}
	public static class SendContext  {
		private Message msg;
		private IotHubStatusCode status;
		private String msgStr;
		public IotHubStatusCode getStatus() {
			return status;
		}

		public void setStatus(IotHubStatusCode status) {
			this.status = status;
		}

		public SendContext(Message msg, String msgStr) {
			super();
			this.msg = msg;
			this.msgStr = msgStr;
		}

		public Message getMsg() {
			return msg;
		}

		public String getMsgStr() {
			return msgStr;
		}


	}

	
	private static class MessageSender implements Runnable {
		private DeviceClient client;
		private SendEvent sendEvent;
		private long interval;
		public MessageSender(SendEvent sendEvent) {
			super();
			this.client = sendEvent.client;
			this.interval = sendEvent.interval;
			this.sendEvent = sendEvent;
		}
		public void run() {
			
			try {
				
				

				while (true) {
					Gson gsonObj = new Gson();
					Map<String, Object> map = new HashMap<String, Object>();
					for( String key:valueMap.keySet()) {
						DataItem value = valueMap.get(key);
						Object val = value.getValue();
						String id = value.getItemId();
						if(isNumeric(val.toString())) {
							map.put(id, 
									Double.parseDouble(val.toString())
							);
						} else {
							map.put(id, val);
						}
						
					}
					String msgStr = gsonObj.toJson(map);
					Message msg = new Message(msgStr);

					SendContext lockobj = new SendContext(msg, msgStr);

					// Send the message.

					this.client.sendEventAsync(msg, new IotHubEventCallback() {

						public void execute(IotHubStatusCode status, Object context) {
							((SendContext)context).setStatus(status);
							sendEvent.setChanged();
							sendEvent.notifyObservers(context);
							if (context != null) {
								synchronized (context) {
									context.notify();
								}
							}
							
							
							
						}
						
					}, lockobj);

					synchronized (lockobj) {
						lockobj.wait();
					}
					Thread.sleep(interval);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() throws IOException {

		client.open();
		
		observer = new Observer() {



			public void update(Observable o, Object arg) {
				try {
					Item item = (Item) arg;
					String id = item.getId();
					DataItem dataItem = OPCDAItemDataReader.parseValue(id, item.read(false));

					synchronized(SendEvent.this) {
						valueMap.put(dataItem.getItemId(), dataItem);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					
				}

			}
		};
		MessageSender sender = new MessageSender(this);
		executor = Executors.newFixedThreadPool(1);
		executor.execute(sender);
		dataReader.addObserver(observer);
		
		
	}
	
	public void stop() throws IOException {
		dataReader.deleteObserver(observer);
		this.deleteObserver(eventObserver);
		executor.shutdownNow();
		client.closeNow();
	}

	
}
