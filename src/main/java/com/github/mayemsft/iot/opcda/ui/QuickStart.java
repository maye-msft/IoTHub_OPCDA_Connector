package com.github.mayemsft.iot.opcda.ui;

import java.net.URISyntaxException;
import java.util.Observable;
import java.util.Observer;

import com.github.mayemsft.iot.opcda.connector.OPCDAItemDataReader;
import com.github.mayemsft.iot.opcda.connector.SendEvent;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

public class QuickStart {

	public QuickStart(String host, String domain, String user, String password, String clsId, int period,
			String iothubKey) throws Exception {
		super();

		OPCDAItemDataReader reader = new OPCDAItemDataReader(host, domain, user, new String(password), clsId, "",
				period);
		SendEvent sendEvent = new SendEvent(iothubKey, reader, (long)period, new Observer() {

			public void update(Observable o, Object arg) {
				try {
					Message message = ((SendEvent.SendContext)arg).getMsg();
					String msgStr = ((SendEvent.SendContext)arg).getMsgStr();
					IotHubStatusCode code = ((SendEvent.SendContext)arg).getStatus();
					System.out.println("[Send To IoTHub]: "+code.name()+"; "+ msgStr + "\n");
				
				} catch (Exception e1) {
					e1.printStackTrace();
				}

			}
		});
		sendEvent.start();
		
	}
	
	public static void main(String[] args) {
		try {
			new QuickStart(args[0], args[1], args[2], args[3], args[4], Integer.parseInt(args[5]), args[6]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
