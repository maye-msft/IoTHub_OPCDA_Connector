package com.github.mayemsft.iot.opcda.connector;

import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.IJIUnsigned;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.AccessBase;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.DataCallback;
import org.openscada.opc.lib.da.DuplicateGroupException;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.SyncAccess;


public class OPCDAItemDataReader extends Observable {
	// /**
	// * Main application, arguments are provided as system properties, e.g.<br>
	// * java -Dhost="localhost" -Duser="admin" -Dpassword="secret" -jar
	// demo.opc.jar<br>
	// * Tested with a windows user having administrator rights<br>
	// * @param args unused
	// * @throws Exception in case of unexpected error
	// */
	// public static void main(String[] args) throws Exception {
	//// Logger.getLogger("org.jinterop").setLevel(Level.ALL); // Quiet => Level.OFF
	//
	//// final String host = System.getProperty("host", "localhost");
	//// final String user = System.getProperty("user",
	// System.getProperty("user.name"));
	//// final String password = System.getProperty("password");
	//// // Powershell: Get-ItemPropertyValue
	// 'Registry::HKCR\Matrikon.OPC.Simulation.1\CLSID' '(default)'
	//// final String clsId = System.getProperty("clsId",
	// "F8582CF2-88FB-11D0-B850-00C0F0104305");
	// final String itemId = System.getProperty("itemId", "Random.Qualities");
	//
	// final ConnectionInformation ci = new ConnectionInformation();
	// ci.setDomain("fareast");
	// ci.setUser("yem");
	// ci.setPassword("eyamW812127!");
	// ci.setHost("localhost");
	//// ci.setProgId("Matrikon.OPC.Simulation.1");
	// ci.setClsid("F8582CF2-88FB-11D0-B850-00C0F0104305");
	//
	// final Server server = new Server(ci,
	// Executors.newSingleThreadScheduledExecutor());
	// server.connect();
	//
	// final AccessBase access = new SyncAccess(server, 1000);
	// access.addItem(itemId, new DataCallback() {
	// public void changed(final Item item, final ItemState state) {
	// System.out.println(item.);
	// }
	// });
	//
	// access.bind();
	// Thread.sleep(100000L);
	// access.unbind();
	// }


    public static final String BASE_TYPE_PRDFIX = "java.lang";

    public static final String DATE_TYPE_PRDFIX = "java.util";

    static {
		Logger.getLogger("org.jinterop").setLevel(Level.OFF);
	}

	
	private String host = "localhost";

	private String domain = "";

	private String user = "";

	private String password = "";

	private String clsid = null;

	private String progId = null;

	private int period = 1000;

	private AccessBase access;

	private Server server;

    private boolean started = false;
    
	public OPCDAItemDataReader(String host, String domain, String user, String password, String clsid, String progId,
			int period) {
		super();
		this.host = host;
		this.domain = domain;
		this.user = user;
		this.password = password;
		this.clsid = clsid;
		this.progId = progId;
		this.period = period;
	}

	public void start(String items[]) throws IllegalArgumentException, UnknownHostException, JIException,
			AlreadyConnectedException, NotConnectedException, DuplicateGroupException, AddFailedException {
		final ConnectionInformation ci = new ConnectionInformation();
	    ci.setDomain(this.domain);
	    ci.setUser(this.user);
	    ci.setPassword(this.password);
	    ci.setHost(this.host);
	    ci.setProgId(this.progId);
	    ci.setClsid(this.clsid);
	    server = new Server(ci, Executors.newSingleThreadScheduledExecutor());
	  
	  
	    server.connect();
	    access = new SyncAccess(server, this.period);
	    for(String item : items) {
		    access.addItem(item, new DataCallback() {
		        public void changed(final Item item, final ItemState state) {
		        	OPCDAItemDataReader.this.notifyObservers(item);
		        	OPCDAItemDataReader.this.setChanged();
		        }
		      });
	    }
	    access.bind();
	    this.started = (true);
	}

	

	public void stop() throws JIException {
		access.unbind();
		server.disconnect();
		this.started = (false);
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setClsid(String clsid) {
		this.clsid = clsid;
	}

	public void setProgId(String progId) {
		this.progId = progId;
	}
	
	public static void main(String[] args) {
		OPCDAItemDataReader reader = new OPCDAItemDataReader("localhost", "", "", "", "F8582CF2-88FB-11D0-B850-00C0F0104305", "", 5000);
		reader.addObserver(new Observer() {
			
			public void update(Observable o, Object arg) {
				try {
					Item item = (Item)arg;
					String id = item.getId();
					DataItem dataItem = parseValue(id, item.read(false));
					System.out.println(dataItem.toJson());
				} catch (JIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		try {
			reader.start(new String[] {"Random.Qualities"});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AlreadyConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DuplicateGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AddFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(100000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			reader.stop();
		} catch (JIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static Map<String, Object> getValue(JIVariant jiVariant) throws Exception{
        Object newValue ;
        Object oldValue = jiVariant.getObject();
        String typeName = oldValue.getClass().getTypeName();
        if(typeName.startsWith(BASE_TYPE_PRDFIX) || typeName.startsWith(DATE_TYPE_PRDFIX)){
            newValue = jiVariant.getObject();
        }else if(oldValue instanceof JIArray){
            newValue = jiVariant.getObjectAsArray();
        }else if(oldValue instanceof IJIUnsigned){
            newValue = jiVariant.getObjectAsUnsigned().getValue();
        }else if(oldValue instanceof IJIComObject){
            newValue = jiVariant.getObjectAsComObject();
        }else if(oldValue instanceof JIString){
            newValue = jiVariant.getObjectAsString().getString();
        }else if(oldValue instanceof JIVariant){
            newValue = jiVariant.getObjectAsVariant();
        }else{
            newValue = oldValue;
        }

        HashMap<String, Object> result = new HashMap<String, Object>(2);
        result.put("type", newValue.getClass().getSimpleName());
        result.put("value", newValue);
        return result;
    }
	
	
    public static DataItem parseValue(String itemId, ItemState itemState) throws Exception{
        Map<String, Object> value = getValue(itemState.getValue());
        return new DataItem(
                itemId,
                value.get("type").toString(),
                value.get("value"),
                itemState.getQuality(),
                itemState.getTimestamp().getTime(),
                DateUtil.getRecentMoment(),
                DateUtil.getCurrentTime()
            );
    }
    
    
    public boolean isStarted() {
		return started;
	}



	public static class DataItem implements Serializable {


		private static final long serialVersionUID = -3762820144114122935L;

		private String ts;

		public String getItemId() {
			return itemId;
		}

		public String getDataType() {
			return dataType;
		}

		public Object getValue() {
			return value;
		}

		public Short getQuality() {
			return quality;
		}

		public Date getDataTime() {
			return dataTime;
		}

		public Date getCurrMonment() {
			return currMonment;
		}

		public DataItem(String itemId, String dataType, Object value, Short quality, Date dataTime, Date currMonment, String ts) {
			super();
			this.itemId = itemId;
			this.dataType = dataType;
			this.value = value;
			this.quality = quality;
			this.dataTime = dataTime;
			this.currMonment = currMonment;
			this.ts = ts;
		}

		private String itemId;

        private String dataType;

        private Object value;

        private Short quality;

        private Date dataTime;

        private Date currMonment;
        
        public String toJson() throws JsonGenerationException, JsonMappingException, IOException {
        	
    		ObjectMapper mapperObj = new ObjectMapper();
    		return mapperObj.writeValueAsString(this);

    	}

		public String getTs() {
			return ts;
		}
        
    }

}
