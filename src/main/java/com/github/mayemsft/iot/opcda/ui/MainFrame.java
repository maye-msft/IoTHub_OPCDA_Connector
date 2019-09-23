package com.github.mayemsft.iot.opcda.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.da.Item;

import com.github.mayemsft.iot.opcda.connector.OPCDAItemDataReader;
import com.github.mayemsft.iot.opcda.connector.OPCDAItemDataReader.DataItem;
import com.github.mayemsft.iot.opcda.connector.OPCDAItemInfoReader;
import com.github.mayemsft.iot.opcda.connector.OPCDAServerInfoReader;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private OPCDAServerInfoReader serverInfoReader;

	private OPCDAItemInfoReader itemInfoReader;

	private OPCDAItemDataReader opcdaItemDataReader;

	public MainFrame(String string) {
		super(string);
	}

	private static void showError(Exception ex, JTextArea area) {
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		ex.printStackTrace();
		area.setText(errors.toString());
	}
	
	private void init() {

		JPanel rootPanel = new JPanel();
		JPanel controlPanel = new JPanel();
		JPanel formPanel = new JPanel();
		JPanel formWapperPanel = new JPanel();
		JPanel itemsPanel = new JPanel();
		JPanel messagePanel = new JPanel();
		rootPanel.setLayout(new GridLayout(1, 2, 10, 10));
		controlPanel.setLayout(new GridLayout(2, 1, 10, 10));
		formPanel.setLayout(new GridLayout(15, 1, 10, 10));
		itemsPanel.setLayout(new BorderLayout());
		messagePanel.setLayout(new BorderLayout());

		rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		formWapperPanel.setLayout(new BorderLayout());
		formWapperPanel.add(formPanel, BorderLayout.NORTH);
		rootPanel.add(formWapperPanel);
		rootPanel.add(controlPanel);

		controlPanel.add(itemsPanel);
		controlPanel.add(messagePanel);

		formPanel.add(new JLabel("Host"));
		final JTextField hostField = new JTextField();
		formPanel.add(hostField);

		formPanel.add(new JLabel("Domain"));
		final JTextField domainField = new JTextField();
		formPanel.add(domainField);

		formPanel.add(new JLabel("User"));
		final JTextField userField = new JTextField();
		formPanel.add(userField);

		formPanel.add(new JLabel("Password"));
		final JPasswordField passwordField = new JPasswordField();
		formPanel.add(passwordField);

		formPanel.add(new JLabel("Class Id"));
		final JTextField clsIdField = new JTextField();
		formPanel.add(clsIdField);

		formPanel.add(new JLabel("Period"));
		final JTextField periodField = new JTextField();
		formPanel.add(periodField);

		formPanel.add(new JLabel("IoTHub Key"));
		final JTextField iothubKeyField = new JTextField();
		formPanel.add(iothubKeyField);

		final JButton connectBtn = new JButton("Connect");

		formPanel.add(connectBtn);

		final DefaultListModel<String> listModel = new DefaultListModel<String>();
		// l1.addElement("C");
		// l1.addElement("C++");
		// l1.addElement("Java");
		// l1.addElement("PHP");
		final JList<String> itemList = new JList<String>(listModel);
		itemsPanel.add(new JScrollPane(itemList), BorderLayout.CENTER);

		itemsPanel.add(new JLabel("Items"), BorderLayout.NORTH);

		final JButton startBtn = new JButton("Start");
		itemsPanel.add(startBtn, BorderLayout.SOUTH);

		final JTextArea msgTextArea = new JTextArea();
		messagePanel.add(new JScrollPane(msgTextArea), BorderLayout.CENTER);

		connectBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String host = hostField.getText();
				String domain = domainField.getText();
				String user = userField.getText();
				char[] password = passwordField.getPassword();
				String clsId = clsIdField.getText();
				String iothubKey = iothubKeyField.getText();
				if (serverInfoReader == null) {
					serverInfoReader = new OPCDAServerInfoReader(host, domain, user, new String(password), clsId);

				}
				if (!serverInfoReader.isConnected()) {
					try {
						serverInfoReader.connect();
						final Map<String, String> status = serverInfoReader.getStatus();
						connectBtn.setText("Disconnect");
						status.keySet().forEach(new Consumer<String>() {

							public void accept(String key) {
								msgTextArea.append(key + ": " + status.get(key) + "\n");
							}

						});
					} catch (Exception e1) {
						showError(e1, msgTextArea);
					}
					
				} else {
					try {
						serverInfoReader.stop();
						connectBtn.setText("Connect");
					} catch (JIException e1) {
						showError(e1, msgTextArea);
						return;
					}

					if (opcdaItemDataReader != null && opcdaItemDataReader.isStarted()) {
						try {
							opcdaItemDataReader.stop();
							startBtn.setText("Start");
						} catch (JIException e1) {
							showError(e1, msgTextArea);
							return;
						}
					}
					
				}

				if (itemInfoReader == null) {
					itemInfoReader = new OPCDAItemInfoReader(host, domain, user, new String(password), clsId);
				}
				if (!itemInfoReader.isConnected()) {
					try {
						List<String> items = itemInfoReader.getItems();
						for (String item : items)
							listModel.addElement(item);
					} catch (UnknownHostException e1) {
						showError(e1, msgTextArea);
					} catch (JIException e1) {
						showError(e1, msgTextArea);
					}
				} else {
					try {
						itemInfoReader.stop();
					} catch (JIException e1) {
						showError(e1, msgTextArea);
					}
				}

			}
		});

		startBtn.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				String host = hostField.getText();
				String domain = domainField.getText();
				String user = userField.getText();
				char[] password = passwordField.getPassword();
				String clsId = clsIdField.getText();
				String iothubKey = iothubKeyField.getText();
				Integer period = Integer.parseInt(periodField.getText());

				if (opcdaItemDataReader == null) {
					opcdaItemDataReader = new OPCDAItemDataReader(host, domain, user, new String(password), clsId, "",
							period);

				}

				if (!opcdaItemDataReader.isStarted()) {
					opcdaItemDataReader.deleteObservers();
					opcdaItemDataReader.addObserver(new Observer() {

						public void update(Observable o, Object arg) {
							try {
								Item item = (Item) arg;
								String id = item.getId();
								DataItem dataItem = OPCDAItemDataReader.parseValue(id, item.read(false));
								msgTextArea.append(dataItem.toJson() + "\n");
							} catch (Exception e1) {
								showError(e1, msgTextArea);
							}

						}
					});

					List<String> selectedItem = itemList.getSelectedValuesList();
					String[] array = new String[selectedItem.size()];
					selectedItem.toArray(array);
					try {
						opcdaItemDataReader.start(array);
						startBtn.setText("Stop");
						msgTextArea.setText("");
					} catch (Exception e1) {
						showError(e1, msgTextArea);
					} 
					
					
				} else {
					try {
						opcdaItemDataReader.stop();
						startBtn.setText("Start");
					} catch (JIException e1) {
						showError(e1, msgTextArea);
					}
					
					
				}

			}
		});

		this.setLayout(new BorderLayout());
		this.add(rootPanel, BorderLayout.CENTER);
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);

		hostField.setText("localhost");
		domainField.setText("");
		userField.setText("");
		passwordField.setText("");
		clsIdField.setText("");
		iothubKeyField.setText("");
		periodField.setText("1000");
	}

	public static void main(String s[]) {

		MainFrame frame = new MainFrame("OPCDA Azure IoTHub Connector");
		frame.init();

	}
}