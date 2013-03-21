package elbaGui;

import java.awt.EventQueue;

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

public class MainWindow implements MainWindowInterface{

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Experiment.loadFromXML("I:/RUBBOS-221.xml");
		
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
		
		experimentList = new JList();
		
		experimentList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0)
			{
				callback.listSelectionChanged();
			}
		});
		frame.getContentPane().add(experimentList, BorderLayout.WEST);
		
		experimentInfo = new JPanel();
		frame.getContentPane().add(experimentInfo, BorderLayout.CENTER);
	}
	
	private JList experimentList;
	private JPanel experimentInfo;
	private JMenu mnFile;
	private JMenuItem mntmReloadExperiments;
	private JMenuItem mntmExit;

}
