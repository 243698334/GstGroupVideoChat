package com.kevinychen.gstgroupvideochat.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.kevinychen.gstgroupvideochat.core.Core;

public class WaitForPeerFrame extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;
	private static WaitForPeerFrame instance = new WaitForPeerFrame();

	private static JPanel waitForPeerPanel;
	private static JLabel waitForPeerLabel;
	private static JProgressBar waitForPeerProgressBar;

	private static JPanel errorPanel;
	private static JLabel errorLabel;
	private static JLabel errorMessageLabel;
	private static JButton closeButton;
	private static JPanel buttonPanel;

	private WaitForPeerFrame() {
	}

	public static WaitForPeerFrame getInstance() {
		return instance;
	}

	/**
	 * Used by PeerSettingsPanel to startup a new waiting dialog with nickname
	 * displayed.
	 * 
	 * @param peerNickName
	 */
	public static void loadWaitingInterface(String peerNickname) {
		waitForPeerLabel = new JLabel("Waiting for response from " + peerNickname + "...");
		waitForPeerProgressBar = new JProgressBar(SwingConstants.HORIZONTAL);
		waitForPeerProgressBar.setIndeterminate(true);
		waitForPeerPanel = new JPanel(new GridLayout(0, 1));
		waitForPeerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		waitForPeerPanel.add(waitForPeerLabel);
		waitForPeerPanel.add(waitForPeerProgressBar);

		instance.dispose();
		instance = new WaitForPeerFrame();
		instance.setTitle("Please wait");
		instance.add(waitForPeerPanel);
		instance.setPreferredSize(new Dimension(300, 150));
		instance.pack();
		instance.setLocationRelativeTo(SettingsFrame.getInstance());
		instance.setVisible(true);
	}

	/**
	 * Used by PeerManager/OutboundTCPConnection to set the visibility of a new
	 * peer waiting dialog.
	 * 
	 * @param newPeer if the peer is a new one so its tab should be closed.
	 * @param message Frame is updated and show message in it.
	 */
	public static void loadErrorInterface(final boolean newPeer, String message) {
		instance.setVisible(false);
		if (newPeer) {
			errorLabel = new JLabel("Failed to connect to your new peer.");
		} else {
			errorLabel = new JLabel("Your update request has been denied.");
		}
		errorMessageLabel = new JLabel(message);
		closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (newPeer) {
					SettingsFrame.removePeerSettingsTab(Core.getCurrentPeerId());
				}
				instance.dispose();
			}
		});
		buttonPanel = new JPanel();
		buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		buttonPanel.add(closeButton);
		errorPanel = new JPanel(new GridLayout(0, 1));
		errorPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		errorPanel.add(errorLabel);
		errorPanel.add(errorMessageLabel);

		instance.dispose();
		instance = new WaitForPeerFrame();
		instance.setTitle("Error");
		instance.add(errorPanel, BorderLayout.CENTER);
		instance.add(buttonPanel, BorderLayout.PAGE_END);
		instance.setPreferredSize(new Dimension(300, 150));
		instance.pack();
		instance.setLocationRelativeTo(null);
		instance.setVisible(true);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}
