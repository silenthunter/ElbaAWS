package elbaGui;

import java.awt.Component;
import java.awt.EventQueue;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JList;
import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import elbaEC2.experiments.Experiment;
import javax.swing.ListSelectionModel;
import javax.swing.AbstractListModel;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class MainWindow implements MainWindowInterface{

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	public void listSelectionChanged()
	{
		System.out.println("Item selected: " + experimentList.getSelectedValue());
		
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
		
		Experiment exp = Experiment.loadFromXML("I:/RUBBOS-221.xml");
		
		DefaultListModel<String> experimentModel = new DefaultListModel<String>();
		experimentModel.addElement("Exper1");
		experimentPanel expPanel = new experimentPanel(exp);
		experimentInfo.add(expPanel);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		final MainWindowInterface callback = this;
		
		frame = new JFrame();
		frame.setBounds(100, 100, 647, 526);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		mntmReloadExperiments = new JMenuItem("Reload Experiments");
		mnFile.add(mntmReloadExperiments);
		
		mntmExit = new JMenuItem("Exit");
		mnFile.add(mntmExit);
		
		experimentList = new JList<String>();
		experimentList.setBounds(10, 11, 100, 445);
		experimentList.setPreferredSize(new Dimension(100, 500));
		experimentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		experimentList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0)
			{
				callback.listSelectionChanged();
			}
		});
		frame.getContentPane().setLayout(null);
		frame.getContentPane().add(experimentList);
		
		experimentInfo = new JPanel();
		experimentInfo.setBounds(158, 11, 463, 445);
		frame.getContentPane().add(experimentInfo);
	}
	
	private JList<String> experimentList;
	private JPanel experimentInfo;
	private JMenu mnFile;
	private JMenuItem mntmReloadExperiments;
	private JMenuItem mntmExit;

}
