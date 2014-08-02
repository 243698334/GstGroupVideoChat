package com.kevinychen.gstgroupvideochat.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;

public class PeerSettingsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private int connectionID;
	private boolean newConnection = true;
	private String peerIpAddress;
	private int peerPort;
	private int frameRate;
	private PeerVideoMode peerVideoMode;
	private String peerNickname;

	private JPanel tcpSettingsPanel;
	private JLabel peerIpAddressLabel;
	private JLabel peerPortLabel;
	private JLabel peerNicknameLabel;
	private JTextField peerIpAddressTextField;
	private JTextField peerPortTextField;
	private JTextField peerNicknameTextField;
	private JCheckBox sslCheckBox;

	private JPanel peerVideoQualityPanel;
	private JLabel frameRateLabel;
	private JLabel audioRateLabel;
	private JLabel resolutionLabel;
	private JTextField frameRateTextField;
	private JLabel audioRateValueLabel;
	private JLabel resolutionValueLabel;
	private JCheckBox muteCheckBox;

	private JPanel peerVideoModePanel;
	private JRadioButton activeRadioButton;
	private JRadioButton passiveRadioButton;
	private ButtonGroup peerVideoModeButtonGroup;

	private JPanel buttonsPanel;
	private JButton applyButton;
	private JButton disconnectButton;

	private JPanel peerSubPanel;

	public PeerSettingsPanel(int tabID) {
		super(new BorderLayout());
		this.connectionID = tabID;
		setupInterface();
	}
	
	public void setNicknameTextField(String nickname) {
		peerNicknameTextField.setText(nickname);
	}

	private void setupInterface() {
		// start of TCP settings section
		peerIpAddressLabel = new JLabel("IP Address: ");
		peerIpAddressTextField = new JTextField("0.0.0.0");
		peerPortLabel = new JLabel("Port: ");
		peerPortTextField = new JTextField("9212");
		peerNicknameLabel = new JLabel("Peer Nickname: ");
		peerNicknameTextField = new JTextField("Peer " + connectionID);
		sslCheckBox = new JCheckBox("Connect via SSL");
		tcpSettingsPanel = new JPanel(new GridLayout(2, 4));
		tcpSettingsPanel.setBorder(BorderFactory.createTitledBorder("TCP Settings"));
		tcpSettingsPanel.add(peerIpAddressLabel);
		tcpSettingsPanel.add(peerIpAddressTextField);
		tcpSettingsPanel.add(peerPortLabel);
		tcpSettingsPanel.add(peerPortTextField);
		tcpSettingsPanel.add(peerNicknameLabel);
		tcpSettingsPanel.add(peerNicknameTextField);
		tcpSettingsPanel.add(sslCheckBox);
		// end of TCP settings section

		// start of peerVideo quality section
		frameRateLabel = new JLabel("Frame Rate: ");
		frameRateTextField = new JTextField();
		audioRateLabel = new JLabel("Audio Rate: ");
		audioRateValueLabel = new JLabel("n/a");
		resolutionLabel = new JLabel("Resolution :");
		resolutionValueLabel = new JLabel("n/a");
		muteCheckBox = new JCheckBox("Mute");
		muteCheckBox.setSelected(true);
		peerVideoQualityPanel = new JPanel(new GridLayout(2, 4));
		peerVideoQualityPanel.setBorder(BorderFactory.createTitledBorder("Peer Video Quality"));
		peerVideoQualityPanel.add(frameRateLabel);
		peerVideoQualityPanel.add(frameRateTextField);
		peerVideoQualityPanel.add(audioRateLabel);
		peerVideoQualityPanel.add(audioRateValueLabel);
		peerVideoQualityPanel.add(resolutionLabel);
		peerVideoQualityPanel.add(resolutionValueLabel);
		peerVideoQualityPanel.add(muteCheckBox);
		// end of peerVideo quality section

		// start of peerVideo mode section
		activeRadioButton = new JRadioButton("Active");
		activeRadioButton.setSelected(false);
		activeRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frameRateTextField.setText("30");
				frameRateTextField.setEditable(true);
				audioRateValueLabel.setText("8000Hz");
				resolutionValueLabel.setText("640x480");
				muteCheckBox.setEnabled(true);
			}
		});
		passiveRadioButton = new JRadioButton("Passive");
		passiveRadioButton.setSelected(false);
		passiveRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frameRateTextField.setText("10");
				frameRateTextField.setEditable(false);
				audioRateValueLabel.setText("No Audio");
				resolutionValueLabel.setText("320x240");
				muteCheckBox.setEnabled(false);
			}
		});
		peerVideoModeButtonGroup = new ButtonGroup();
		peerVideoModeButtonGroup.add(activeRadioButton);
		peerVideoModeButtonGroup.add(passiveRadioButton);
		peerVideoModePanel = new JPanel();
		peerVideoModePanel.setBorder(BorderFactory.createTitledBorder("PeerVideo Mode"));
		peerVideoModePanel.add(activeRadioButton);
		peerVideoModePanel.add(passiveRadioButton);
		// end of peerVideo mode section

		// start of buttons section
		applyButton = new JButton("Apply");
		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					peerIpAddress = peerIpAddressTextField.getText();
					peerPort = new Integer(peerPortTextField.getText());
					frameRate = new Integer(frameRateTextField.getText());
					peerNickname = peerNicknameTextField.getText();
					if (activeRadioButton.isSelected()) {
						peerVideoMode = PeerVideoMode.ACTIVE;
					} else if (passiveRadioButton.isSelected()) {
						peerVideoMode = PeerVideoMode.PASSIVE;
					} else {
						throw new Exception();
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "Invalid settings.");
					System.out.println("PeerSettings [ID=" + connectionID + "]: ERROR: Invalid settings.");
					return;
				}
				
				synchronized(this) {
					WaitForPeerFrame.loadWaitingInterface(peerNickname);
				}
				
				if (newConnection) {
					System.out.println("PeerSettings [ID=" + connectionID + "]: Starting connection...");
					
					if (!PeerManager.onOutboundConnectionCreateCommand(connectionID, peerIpAddress, peerPort, peerVideoMode, frameRate)) {
						WaitForPeerFrame.getInstance().setVisible(false);
						JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "Failed to connect to peer.");
						newConnection = true;
					} else {
						SettingsFrame.setNewPeerButtonEnabled(true);
						disconnectButton.setEnabled(true);
						peerIpAddressTextField.setEditable(false);
						peerPortTextField.setEditable(false);
						peerNicknameTextField.setEditable(false);
						newConnection = false;
					}
				} else {
					System.out.println("PeerSettings [ID=" + connectionID + "]: Updating connection...");
					PeerManager.onOutboundConnectionUpdateCommand(connectionID, peerVideoMode, frameRate);
				}
			}
		});
		disconnectButton = new JButton("Disconnect");
		disconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int userChoice = JOptionPane.showConfirmDialog(SettingsFrame.getInstance(),
						"Are you sure to disconnect?", "Comfirm Disconnect", JOptionPane.YES_NO_OPTION);
				if (userChoice == JOptionPane.YES_OPTION) {
					System.out.println("PeerSettings [ID=" + connectionID + "]: Disconnecting...");
					if (!PeerManager.onOutboundConnectionEndCommand(connectionID)) {
						int userChoiceForceDisconnect = JOptionPane.showConfirmDialog(SettingsFrame.getInstance(),
								"Error occured when disconnecting. Force closing it?", "Comfirm Disconnect", JOptionPane.YES_NO_OPTION);
						if (userChoiceForceDisconnect == 0) {
							SettingsFrame.removePeerSettingsTab(connectionID);
						}
					} else {
						SettingsFrame.removePeerSettingsTab(connectionID);
					}
				}
			}
		});
		disconnectButton.setEnabled(false);
		buttonsPanel = new JPanel();
		buttonsPanel.add(applyButton);
		buttonsPanel.add(disconnectButton);
		// end of buttons section

		peerSubPanel = new JPanel();
		peerSubPanel.setLayout(new BoxLayout(peerSubPanel, BoxLayout.Y_AXIS));
		peerSubPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		peerSubPanel.add(tcpSettingsPanel);
		peerSubPanel.add(peerVideoModePanel);
		peerSubPanel.add(peerVideoQualityPanel);
		this.add(peerSubPanel, BorderLayout.NORTH);
		this.add(buttonsPanel, BorderLayout.PAGE_END);
	}
}

