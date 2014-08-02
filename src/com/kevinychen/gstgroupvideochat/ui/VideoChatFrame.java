package com.kevinychen.gstgroupvideochat.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.gstreamer.swing.VideoComponent;

import com.kevinychen.gstgroupvideochat.core.Core;
import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.sun.jna.Platform;

public class VideoChatFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private static VideoChatFrame videoChatFrameInstance = new VideoChatFrame();

	private static Map<Integer, VideoComponent> videoComponents;
	private static int activeVideoComponentId;

	private static JMenuBar menuBar;
	private static JMenu videoChatMenu;
	private static JMenu helpMenu;
	private static JMenuItem exitMenuItem;
	private static JMenuItem settingsMenuItem;
	private static JMenuItem userManualMenuItem;
	private static JMenuItem developmentManualMenuItem;

	private static JPanel activeVideoComponentPanel;

	private static JScrollPane passiveVideoComponentsScrollPane;
	private static JPanel passiveVideoComponentsPanel;
	private static Map<Integer, JPanel> passiveVideoComponentPanelsMap;

	private static JPanel controlPanel;
	private static JButton settingsButton;
	private static JComboBox<String> selectActiveComboBox;
	private static Map<Integer, Integer> comboBoxIndexAndPeerIdMap;
	
	private static JTextArea consoleTextArea;
	private static JLabel copyrightLabel;

	private static JPanel activeVideoComponentAndConsoleSubPanel;
	private static JPanel passiveVideoComponentsAndButtonSubPanel;
	

	private VideoChatFrame() {
		super("GstGroupChat");
		videoComponents = new LinkedHashMap<Integer, VideoComponent>();
		activeVideoComponentId = -1;
	}

	public static VideoChatFrame getInstance() {
		return videoChatFrameInstance;
	}

	public static void loadInterface(boolean visible) {
		setupMenuBar();
		setupVideoChatView();

		videoChatFrameInstance.setLayout(new BorderLayout());
		videoChatFrameInstance.setPreferredSize(new Dimension(1000, 720));

		videoChatFrameInstance.add(activeVideoComponentAndConsoleSubPanel, BorderLayout.LINE_START);
		videoChatFrameInstance.add(passiveVideoComponentsAndButtonSubPanel, BorderLayout.LINE_END);
		//videoChatFrameInstance.add(passiveVideoComponentsPanel, BorderLayout.LINE_END);
		
		videoChatFrameInstance.pack();
		videoChatFrameInstance.setLocationRelativeTo(null);
		videoChatFrameInstance.setVisible(visible);
		
		comboBoxIndexAndPeerIdMap = new LinkedHashMap<Integer, Integer>();
	}

	public static synchronized void onConnectionCreate(final int connectionId, VideoComponent videoComponent) {
		videoChatFrameInstance.setVisible(true);
		JPanel newPassiveVideoComponentPanel = new JPanel(new GridLayout());
		newPassiveVideoComponentPanel.setPreferredSize(new Dimension(320, 240));
		newPassiveVideoComponentPanel.add(videoComponent);
		
		videoComponents.put(connectionId, videoComponent);
		passiveVideoComponentPanelsMap.put(connectionId, newPassiveVideoComponentPanel);
		passiveVideoComponentsPanel.add(newPassiveVideoComponentPanel);
		passiveVideoComponentsPanel.validate();

		updateSelectActivePeerComboBoxList();
	}

	// TODO
	public static void onConnectionEnd(int connectionId) {
		System.out.println("Active VideoComponent has been set to be Connection [id=" + connectionId + "].");
		selectActiveComboBox.getSelectedIndex();
	}

	public static synchronized boolean setActiveVideoComponent(int connectionId) {
		VideoComponent newActiveVideoComponent = videoComponents.get(connectionId);
		if (newActiveVideoComponent == null) {
			printToConsole("fail to set Active mode");
			return false;
		}
		int lastActiveVideoComponentId = activeVideoComponentId;
		VideoComponent lastActiveVideoComponent = videoComponents.get(lastActiveVideoComponentId);
		passiveVideoComponentsPanel.remove(passiveVideoComponentPanelsMap.remove(connectionId));
		passiveVideoComponentsPanel.repaint();
		if (lastActiveVideoComponentId != -1) {
			JPanel newPassiveVideoComponentPanel = new JPanel(new GridLayout());
			newPassiveVideoComponentPanel.setPreferredSize(new Dimension(320, 240));
			newPassiveVideoComponentPanel.add(lastActiveVideoComponent);
			
			passiveVideoComponentPanelsMap.put(lastActiveVideoComponentId, newPassiveVideoComponentPanel);
			passiveVideoComponentsPanel.add(newPassiveVideoComponentPanel);
			passiveVideoComponentsPanel.validate();
		}
		activeVideoComponentId = connectionId;
		activeVideoComponentPanel.removeAll();
		activeVideoComponentPanel.add(newActiveVideoComponent);
		activeVideoComponentPanel.validate();
		updateSelectActivePeerComboBoxList();
		return true;
	}

	public static void printToConsole(String message) {
		consoleTextArea.append(message + "\n");
	}

	private static void setupMenuBar() {
		// start of VideoChat menu
		settingsMenuItem = new JMenuItem("User Manual");
		settingsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
			}
		});

		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int userChoice = JOptionPane.showConfirmDialog(videoChatFrameInstance,
						"Do you really want to exit GstGroupVideoChat?", "Comfirm exit", JOptionPane.YES_NO_OPTION);
				if (userChoice == JOptionPane.YES_OPTION) {
					Core.onClientExit();
				}
			}
		});

		videoChatMenu = new JMenu("Video Chat");
		videoChatMenu.add(settingsMenuItem);
		videoChatMenu.addSeparator();
		videoChatMenu.add(exitMenuItem);
		videoChatMenu.addSeparator();
		// end of VideoChat menu

		// start of Help menu
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
		developmentManualMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (Platform.isMac()) {
						Runtime.getRuntime().exec("open doc/development_manual.pdf");
					} else if (Platform.isLinux()) {
						Runtime.getRuntime().exec("edit doc/development_manual.pdf");
					} else if (Platform.isWindows()) {
						Runtime.getRuntime().exec("START \"\" doc/development_manual.pdf");
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		helpMenu = new JMenu("Help");
		helpMenu.add(userManualMenuItem);
		helpMenu.add(developmentManualMenuItem);
		helpMenu.addSeparator();
		// end of Help menu

		menuBar = new JMenuBar();
		menuBar.add(videoChatMenu);
		menuBar.add(helpMenu);
		menuBar.setVisible(true);
		videoChatFrameInstance.setJMenuBar(menuBar);
	}

	private static void setupVideoChatView() {
		activeVideoComponentPanel = new JPanel(new GridLayout());
		activeVideoComponentPanel.setPreferredSize(new Dimension(640, 480));

		passiveVideoComponentsPanel = new JPanel(new GridLayout(3,1));
		passiveVideoComponentsPanel.setPreferredSize(new Dimension(320, 1440));
		passiveVideoComponentsPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		passiveVideoComponentsScrollPane = new JScrollPane(passiveVideoComponentsPanel);
		passiveVideoComponentsScrollPane.setPreferredSize(new Dimension(320, 720));
		passiveVideoComponentPanelsMap = new LinkedHashMap<Integer, JPanel>();

		settingsButton = new JButton("New Peer");
		settingsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SettingsFrame.setSettingsWindowVisible(true);
			}
		});
		String[] selectActiveComboxBoxInitialList = {"Select Active Peer"};
		selectActiveComboBox = new JComboBox<String>(selectActiveComboxBoxInitialList);
		selectActiveComboBox.setPrototypeDisplayValue("------------------------");
		selectActiveComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectActiveComboBox.getSelectedIndex() != 0) {
					setActiveVideoComponent(comboBoxIndexAndPeerIdMap.get(selectActiveComboBox.getSelectedIndex()));
				}
			}
		});
		controlPanel = new JPanel();
		controlPanel.add(selectActiveComboBox);
		controlPanel.add(settingsButton);
		
		consoleTextArea = new JTextArea();
		consoleTextArea.setBackground(Color.BLACK);
		consoleTextArea.setForeground(Color.GREEN);
		consoleTextArea.setEditable(false);
		consoleTextArea.setPreferredSize(new Dimension(640, 240));

		copyrightLabel = new JLabel(Core.copyrightText);

		activeVideoComponentAndConsoleSubPanel = new JPanel(new BorderLayout());
		activeVideoComponentAndConsoleSubPanel.add(activeVideoComponentPanel, BorderLayout.PAGE_START);
		activeVideoComponentAndConsoleSubPanel.add(consoleTextArea, BorderLayout.CENTER);
		activeVideoComponentAndConsoleSubPanel.add(copyrightLabel, BorderLayout.PAGE_END);

		passiveVideoComponentsAndButtonSubPanel = new JPanel(new BorderLayout());
		passiveVideoComponentsAndButtonSubPanel.add(controlPanel, BorderLayout.PAGE_START);
		passiveVideoComponentsAndButtonSubPanel.add(passiveVideoComponentsPanel, BorderLayout.CENTER);
		
		
	}
	
	private static void updateSelectActivePeerComboBoxList() {
		String[] selectActivePeerComboBoxList = new String[passiveVideoComponentPanelsMap.size() + 1];
		selectActivePeerComboBoxList[0] = new String("Select Active Peer");
		int currentIndex = 1;
		for (Map.Entry<Integer, JPanel> currenIdPanelPair : passiveVideoComponentPanelsMap.entrySet()) {
			selectActivePeerComboBoxList[currentIndex] = PeerManager.getPeer(currenIdPanelPair.getKey()).getNickname();
			comboBoxIndexAndPeerIdMap.put(currentIndex, currenIdPanelPair.getKey());
			currentIndex++;
		}
		selectActiveComboBox.setModel(new DefaultComboBoxModel<String>(selectActivePeerComboBoxList));
		selectActiveComboBox.revalidate();
		controlPanel.revalidate();
	}

}
