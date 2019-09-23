package com.github.mayemsft.iot.opcda.connector;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.dcom.da.OPCBROWSEDIRECTION;
import org.openscada.opc.dcom.da.OPCBROWSETYPE;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;
import org.openscada.opc.dcom.da.impl.OPCServer;


public class OPCDAItemInfoReader  {
	
    static {
		Logger.getLogger("org.jinterop").setLevel(Level.OFF);
	}
    
	private String host = "localhost";

	private String domain = "";

	private String user = "";

	private String password = "";

	private String clsid = null;

	private JISession session;

	private boolean connected = false; 
	
	public OPCDAItemInfoReader(String host, String domain, String user, String password, String clsid) {
		super();
		this.host = host;
		this.domain = domain;
		this.user = user;
		this.password = password;
		this.clsid = clsid;
	}
	
	public List<String> getItems() throws UnknownHostException, JIException {
		JISystem.setAutoRegisteration(true);

		session = JISession.createSession(
				this.domain, this.user, this.password);

		final JIComServer comServer = new JIComServer(
				JIClsid.valueOf(this.clsid),
				this.host, session);

		final IJIComObject serverObject = comServer.createInstance();

		OPCServer server = new OPCServer(serverObject);
		final OPCBrowseServerAddressSpace serverBrowser = server.getBrowser();
		
		connected = true;
		
		return browseFlat(serverBrowser);
	}



	private List<String> browseFlat(final OPCBrowseServerAddressSpace browser)
			throws JIException, IllegalArgumentException, UnknownHostException {

//		System.out.println(String.format("Organization: %s",
//				browser.queryOrganization()));

		browser.changePosition(null, OPCBROWSEDIRECTION.OPC_BROWSE_TO);
		List<String> items = new ArrayList<String>();
		
		for (final String id : browser.browse(OPCBROWSETYPE.OPC_FLAT, "", 0,
				JIVariant.VT_EMPTY).asCollection()) {
//			System.out.println("Item: " + id);
			items.add(id);
		}
		return items;
	}
	
	public static void main(String[] args) {
		OPCDAItemInfoReader itemInfoReader = new OPCDAItemInfoReader("localhost", "", "", "", "F8582CF2-88FB-11D0-B850-00C0F0104305");
		try {
			List<String> items = itemInfoReader.getItems();
			items.forEach(new Consumer<String>() {

				public void accept(String t) {
					System.out.println(t);
				}
				
			});
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stop() throws JIException {
		JISession.destroySession(session);
		connected = false; 
	}
	

	public boolean isConnected() {
		return connected;
	}


}
