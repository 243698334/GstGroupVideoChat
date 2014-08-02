package com.kevinychen.gstgroupvideochat.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.kevinychen.gstgroupvideochat.core.Core;
import com.kevinychen.gstgroupvideochat.core.PeerManager;

public class ResourceSettingsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static ResourceSettingsPanel instance = new ResourceSettingsPanel();
	
	private static JPanel loadResourceFilePanel;
	private static JButton chooseFileButton;
	private static JLabel resourceFileLabel;
	private static JFileChooser resourceFileChooser;
	
	private static JPanel resourceInfoPanel;
	private static JLabel myNicknameLabel;
	private static JLabel totalBandwidthLabel;
	private static JLabel availableBandwidthLabel;
	private static JLabel peersConnectedLabel;
	private static JTextField myNicknameTextField;
	private static JLabel totalBandwidthValueLabel;
	private static JLabel availableBandwidthValueLabel;
	private static JLabel peersConnectedValueLabel;
	
	private static JPanel tcpSettingsPanel;
	private static JLabel tcpListenPortLabel;
	private static JTextField tcpListenPortTextField;
	
	private static JPanel buttonsPanel;
	private static JButton applyButton;
	
	private static JPanel resourceSubPanel;

	private static File resourceFile;
	
	private ResourceSettingsPanel() {
		super(new BorderLayout());
	}
	
	public static ResourceSettingsPanel getInstance() {
		return instance;
	}
	
	public static void loadInterface(int tabID) {
		setupInterface();
	}
	
	/**
	 * Called by SettingsFrame to modify the number of servers connected.
	 * @param diff values can be -1 or 1.
	 */
	public static void updatePeerCount(int diff) {
		int currentCount = new Integer(peersConnectedValueLabel.getText());
		peersConnectedValueLabel.setText(Integer.toString(currentCount + diff));
	}
	
	/**
	 * Called by SettingsFrame to modify the value of available bandwidth
	 * @param diff
	 */
	public static void updateAvailableBandwidth(long diff) {
		long currentBandwidth = new Long(availableBandwidthValueLabel.getText());
		availableBandwidthValueLabel.setText(Long.toString(currentBandwidth + diff));
	}
	
	private static void setupInterface() {
		// start of load resource file section
		resourceFileLabel = new JLabel("No file choosen");
		resourceFileChooser = new JFileChooser("Choose Resource File");
		resourceFileChooser.setCurrentDirectory(new File("config/"));
		resourceFileChooser.setFileFilter(new FileNameExtensionFilter("Resources Files", "res"));
		chooseFileButton = new JButton("Choose File");
		chooseFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resourceFileChooser.showOpenDialog(SettingsFrame.getInstance());
				resourceFile = resourceFileChooser.getSelectedFile();
				if (resourceFile != null) {
					resourceFileLabel.setText(resourceFile.getName());
				}
			}
		});
		loadResourceFilePanel = new JPanel();
		loadResourceFilePanel.setBorder(BorderFactory.createTitledBorder("Load Resource File"));
		loadResourceFilePanel.add(chooseFileButton);
		loadResourceFilePanel.add(resourceFileLabel);
		// end of load resource file section
		
		// start of resource info section
		myNicknameLabel = new JLabel("Nickname: ");
		myNicknameTextField = new JTextField();
		myNicknameTextField.setText(getHostName());
		totalBandwidthLabel = new JLabel("Total Bandwidth: ");
		totalBandwidthValueLabel = new JLabel("n/a");
		availableBandwidthLabel = new JLabel("Available Bandwidth: ");
		availableBandwidthValueLabel = new JLabel("n/a");
		peersConnectedLabel = new JLabel("Peers Connected: ");
		peersConnectedValueLabel = new JLabel("n/a");
		resourceInfoPanel = new JPanel(new GridLayout(2, 4));
		resourceInfoPanel.setBorder(BorderFactory.createTitledBorder("Resource Info"));
		resourceInfoPanel.add(myNicknameLabel);
		resourceInfoPanel.add(myNicknameTextField);
		resourceInfoPanel.add(totalBandwidthLabel);
		resourceInfoPanel.add(totalBandwidthValueLabel);
		resourceInfoPanel.add(peersConnectedLabel);
		resourceInfoPanel.add(peersConnectedValueLabel);
		resourceInfoPanel.add(availableBandwidthLabel);
		resourceInfoPanel.add(availableBandwidthValueLabel);
		// end of resource info section
		
		// start of tcp settings section
		tcpListenPortLabel = new JLabel("TCP Listen Port: ");
		tcpListenPortTextField = new JTextField("9212");
		tcpSettingsPanel = new JPanel();
		tcpSettingsPanel.setBorder(BorderFactory.createTitledBorder("TCP Settings"));
		tcpSettingsPanel.add(tcpListenPortLabel);
		tcpSettingsPanel.add(tcpListenPortTextField);
		// end of tcp settings section
		
		// start of buttons section
		applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (resourceFileChooser.getSelectedFile() == null) {
					JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "No resource file was selected.");
				} else {
					PeerManager.setTCPListenPort(new Integer(tcpListenPortTextField.getText()));
					loadResourceInfo();
					Core.setMyNickname(myNicknameTextField.getText());
				}
			}
		});
		buttonsPanel = new JPanel();
		buttonsPanel.add(applyButton);		
		// end of buttons section
		
		resourceSubPanel = new JPanel();
		resourceSubPanel.setLayout(new BoxLayout(resourceSubPanel, BoxLayout.Y_AXIS));
		resourceSubPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		resourceSubPanel.add(loadResourceFilePanel);
		resourceSubPanel.add(resourceInfoPanel);
		resourceSubPanel.add(tcpSettingsPanel);
		instance.add(resourceSubPanel, BorderLayout.NORTH);
		instance.add(buttonsPanel, BorderLayout.PAGE_END);
	}
	
	private static void loadResourceInfo() {
		if (PeerManager.loadResourceFile(resourceFile) == false) {
			JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "Invalid resource file.");
			return;
		}
		if (PeerManager.resourceAdmissionOk) {
			totalBandwidthValueLabel.setText(Long.toString(PeerManager.getTotalBandwidth()));
			availableBandwidthValueLabel.setText(Long.toString(PeerManager.getAvailableBandwidth()));
			peersConnectedValueLabel.setText(Integer.toString(PeerManager.getPeersConnected()));
			SettingsFrame.setNewPeerButtonEnabled(true);
		} else {
			JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "Resource admission failed. Check the new Total Bandwidth");
		}
	}
	
	private static String getHostName() {
		String hostName;
		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostName = new String(addr.getHostName());
		} catch (UnknownHostException ex) {
			hostName = "default_client_id";
		}
		return hostName;
	}
}