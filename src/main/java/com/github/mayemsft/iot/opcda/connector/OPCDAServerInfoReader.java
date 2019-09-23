package com.github.mayemsft.iot.opcda.connector;

import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;
import org.openscada.opc.dcom.da.impl.OPCServer;


public class OPCDAServerInfoReader  {
	
    static {
		Logger.getLogger("org.jinterop").setLevel(Level.OFF);
	}
    
	private String host = "localhost";

	private String domain = "";

	private String user = "";

	private String password = "";

	private String clsid = null;

	private OPCServer server;

	private JISession session;
	
    private boolean connected = false; 

	
	public OPCDAServerInfoReader(String host, String domain, String user, String password, String clsid) {
		super();
		this.host = host;
		this.domain = domain;
		this.user = user;
		this.password = password;
		this.clsid = clsid;
	}
	
	public void connect() throws UnknownHostException, JIException {
		JISystem.setAutoRegisteration(true);

		session = JISession.createSession(
				this.domain, this.user, this.password);

		final JIComServer comServer = new JIComServer(
				JIClsid.valueOf(this.clsid),
				this.host, session);
		
		final IJIComObject serverObject = comServer.createInstance();

		server = new OPCServer(serverObject);
		
		this.connected = (true);
	}

	public Map<String, String> getStatus() throws JIException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		final OPCSERVERSTATUS status = server.getStatus();
		map.put("State", status.getServerState().toString());
		map.put("Vendor",  status.getVendorInfo());
		map.put("Major Version", String.valueOf(status.getMajorVersion()));
		map.put("Minor Version", String.valueOf(status.getMinorVersion()));
		map.put("Build Number", String.valueOf(status.getBuildNumber()));
		map.put("Bandwidth",String.valueOf(status.getBandWidth()));
		map.put("Start Time", DateFmt.formateTime(status.getStartTime().asCalendar().getTime()));
		map.put("Current Time", DateFmt.formateTime(status.getCurrentTime().asCalendar().getTime()));
		map.put("Last Update Time", DateFmt.formateTime(status
				.getLastUpdateTime().asCalendar().getTime()));
		return map;
	}

	public static void dumpServerStatus(final OPCServer server)
			throws JIException {

		final OPCSERVERSTATUS status = server.getStatus();

		System.out.println("===== SERVER STATUS ======");
		System.out.println("State: " + status.getServerState().toString());
		System.out.println("Vendor: " + status.getVendorInfo());
		System.out.println(String.format("Version: %d.%d.%d",
				status.getMajorVersion(), status.getMinorVersion(),
				status.getBuildNumber()));
		System.out.println("Groups: " + status.getGroupCount());
		System.out.println("Bandwidth: " + status.getBandWidth());
		System.out.println(String.format("Start Time: %tc", status
				.getStartTime().asCalendar()));
		System.out.println(String.format("Current Time: %tc", status
				.getCurrentTime().asCalendar()));
		System.out.println(String.format("Last Update Time: %tc", status
				.getLastUpdateTime().asCalendar()));
		System.out.println("===== SERVER STATUS ======");
	}
	
	
	public static void main(String[] args) {
		OPCDAServerInfoReader itemInfoReader = new OPCDAServerInfoReader("localhost", "", "", "", "F8582CF2-88FB-11D0-B850-00C0F0104305");
		try {
			itemInfoReader.connect();
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
