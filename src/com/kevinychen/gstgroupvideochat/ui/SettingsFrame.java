package com.kevinychen.gstgroupvideochat.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.kevinychen.gstgroupvideochat.core.Core;
import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.sun.jna.Platform;

public class SettingsFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private static SettingsFrame settingsWindowInstance = new SettingsFrame();

	private static JTabbedPane tabbedPane;
	private static JPanel buttonsPanel;
	private static JPanel buttonsSubPanel;
	private static JButton newPeerButton;
	private static JButton closeButton;
	private static JMenuBar menuBar;
	private static JMenu helpMenu;
	private static JMenuItem userManualMenuItem;
	private static JMenuItem developmentManualMenuItem;

	private static ArrayList<Integer> tabIDList;

	public static SettingsFrame getInstance() {
		return settingsWindowInstance;
	}

	public static JTabbedPane getTabbedPaneInstance() {
		return tabbedPane;
	}

	public static void loadInterface(boolean visible) {
		setupMenuBar();
		setupButtons();
		setupTabbedPane();

		settingsWindowInstance.setPreferredSize(new Dimension(600, 400));
		settingsWindowInstance.pack();
		settingsWindowInstance.setLocationRelativeTo(null);
		settingsWindowInstance.setVisible(visible);

		resourceSettingsMessage();
	}

	/**
	 * Set the visibility of the settings window. Used by "Settings" button in
	 * playback windows and the "close" button.
	 * 
	 * @param b
	 */
	public static void setSettingsWindowVisible(boolean b) {
		settingsWindowInstance.setVisible(b);
	}

	/**
	 * Used by PeerSettingsPanel class to delete or change the title of a tab.
	 * 
	 * @param tabID
	 * @return current index of that tab.
	 */
	public static int getTabIndexById(int tabID) {
		return tabIDList.indexOf(tabID);
	}

	/**
	 * Used by PeerSettingsPanel/ResourceSettingsPanel to make sure no garbage
	 * tab is created.
	 * 
	 * @param b
	 */
	public static void setNewPeerButtonEnabled(boolean b) {
		newPeerButton.setEnabled(b);
	}

	/**
	 * Used by PeerSettingsPanel/ResourceSettingsPanel to make sure no garbage
	 * tab is created.
	 * 
	 * @param b
	 */
	public static void setCloseButtonEnabled(boolean b) {
		closeButton.setEnabled(b);
	}

	public static void updateTabTitle(int peerId, String nickname) {
		String peerNickname = PeerManager.getPeer(peerId).getNickname();
		// peerNicknameTextField.setText(PeerManager.getPeer(connectionID).getNickname());
		SettingsFrame.getTabbedPaneInstance().setTitleAt(SettingsFrame.getTabIndexById(peerId),
				PeerManager.getPeer(peerId).getNickname());
		((PeerSettingsPanel) SettingsFrame.getTabbedPaneInstance().getComponent(SettingsFrame.getTabIndexById(peerId)))
				.setNicknameTextField(peerNickname);
	}

	/**
	 * Setup a JMenuBar with options to open User Manual and Development Manual.
	 */
	private static void setupMenuBar() {
		menuBar = new JMenuBar();

		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_A);

		userManualMenuItem = new JMenuItem("User Manual");
		userManualMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (Platform.isMac()) {
						Runtime.getRuntime().exec("open doc/user_manual.pdf");
					} else if (Platform.isLinux()) {
						Runtime.getRuntime().exec("edit doc/user_manual.pdf");
					} else if (Platform.isWindows()) {
						Runtime.getRuntime().exec("START \"\" doc/user_manual.pdf");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		developmentManualMenuItem = new JMenuItem("Development Manual");
		userManualMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (Platform.isMac()) {
						Runtime.getRuntime().exec("open docs/development_manual.pdf");
					} else if (Platform.isLinux()) {
						Runtime.getRuntime().exec("edit docs/development_manual.pdf");
					} else if (Platform.isWindows()) {
						Runtime.getRuntime().exec("START \"\" docs/development_manual.pdf");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		helpMenu.add(userManualMenuItem);
		helpMenu.add(developmentManualMenuItem);
		helpMenu.addSeparator();

		menuBar.add(helpMenu);
		menuBar.setVisible(true);
		settingsWindowInstance.setJMenuBar(menuBar);
	}

	/**
	 * Setup the main TabbedPane
	 */
	private static void setupTabbedPane() {
		tabbedPane = new JTabbedPane();
		settingsWindowInstance.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		tabIDList = new ArrayList<Integer>();
		addResourceSettingsTab();
	}

	/**
	 * Setup the New Peer Button
	 */
	private static void setupButtons() {
		newPeerButton = new JButton("New Peer");
		newPeerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addPeerSettingsTab();
			}
		});

		closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SettingsFrame.setSettingsWindowVisible(false);
			}
		});

		buttonsSubPanel = new JPanel();
		buttonsSubPanel.add(newPeerButton);
		buttonsSubPanel.add(closeButton);
		buttonsPanel = new JPanel(new BorderLayout());
		buttonsPanel.add(buttonsSubPanel, BorderLayout.EAST);
		settingsWindowInstance.getContentPane().add(buttonsPanel, BorderLayout.PAGE_END);
	}

	/**
	 * Add a new peer tab to the TabbedPane and generate an ID for it.
	 */
	private static void addPeerSettingsTab() {
		tabbedPane.addTab("New Peer", new PeerSettingsPanel(Core.getNextPeerId()));
		tabIDList.add(Core.getCurrentPeerId());
		newPeerButton.setEnabled(false);
		tabbedPane.setSelectedIndex(getTabIndexById(Core.getCurrentPeerId()));
		ResourceSettingsPanel.updatePeerCount(1);
	}

	/**
	 * Public version of addPeerSettingsTab(). Called by ConnectionManager only.
	 */
	public static void addPeerSettingsTab(int peerId, String peerIpAddress, int peerPort, String peerNickname) {
		tabbedPane.addTab(peerNickname, new PeerSettingsPanel(Core.getNextPeerId()));
		tabIDList.add(Core.getCurrentPeerId());
		tabbedPane.setSelectedIndex(getTabIndexById(Core.getCurrentPeerId()));
		ResourceSettingsPanel.updatePeerCount(1);
	}

	/**
	 * Called by PeerSettingsPanel when a tab is being removed
	 */
	public static void removePeerSettingsTab(int tabID) {
		tabbedPane.remove(getTabIndexById(tabID));
		tabIDList.remove(new Integer(tabID));
		ResourceSettingsPanel.updatePeerCount(-1);
	}

	/**
	 * Called only once when the client start up and run initial settings.
	 */
	private static void addResourceSettingsTab() {
		tabbedPane.addTab("Resource", ResourceSettingsPanel.getInstance());
		ResourceSettingsPanel.loadInterface(Core.getNextPeerId());
		tabIDList.add(Core.getCurrentPeerId());
		newPeerButton.setEnabled(false);
	}

	/**
	 * Notify the user if it's the client was just launched.
	 */
	private static void resourceSettingsMessage() {
		if (!PeerManager.resourceFileLoaded) {
			JOptionPane.showMessageDialog(settingsWindowInstance, "Please finish the resource settings first.");
		}
	}
}
