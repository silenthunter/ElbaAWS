package elbaGui;

import javax.swing.JPanel;

import elbaEC2.experiments.ConfigurationFile;
import elbaEC2.experiments.ConfigurationFile.Param;
import elbaEC2.experiments.ConfigurationFile.Params;
import elbaEC2.experiments.Experiment;
import elbaEC2.experiments.Instance;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.GridLayout;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

public class experimentPanel extends JPanel implements TreeModelListener
{
	
	Experiment experiment;
	private JTextField txtExperiment;
	private JTree instancesTree;
	private JTabbedPane tabbedPane;

	/**
	 * Create the panel.
	 */
	public experimentPanel(Experiment experiment)
	{
		this.experiment = experiment;
		
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 91};
		gridBagLayout.rowHeights = new int[] {0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0};
		setLayout(gridBagLayout);
		
		JLabel lblExperimentName = new JLabel("Experiment Name: ");
		GridBagConstraints gbc_lblExperimentName = new GridBagConstraints();
		gbc_lblExperimentName.anchor = GridBagConstraints.NORTH;
		gbc_lblExperimentName.insets = new Insets(0, 0, 5, 5);
		gbc_lblExperimentName.gridx = 0;
		gbc_lblExperimentName.gridy = 0;
		add(lblExperimentName, gbc_lblExperimentName);
		
		txtExperiment = new JTextField();
		GridBagConstraints gbc_txtExperiment = new GridBagConstraints();
		gbc_txtExperiment.anchor = GridBagConstraints.NORTH;
		gbc_txtExperiment.insets = new Insets(0, 0, 5, 0);
		gbc_txtExperiment.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtExperiment.gridx = 1;
		gbc_txtExperiment.gridy = 0;
		add(txtExperiment, gbc_txtExperiment);
		txtExperiment.setColumns(10);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setPreferredSize(new Dimension(300, 320));
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.insets = new Insets(0, 0, 5, 0);
		gbc_tabbedPane.gridx = 1;
		gbc_tabbedPane.gridy = 1;
		add(tabbedPane, gbc_tabbedPane);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(300, 400));
		tabbedPane.addTab("New tab", null, scrollPane, null);
		
		instancesTree = new JTree();
		instancesTree.setPreferredSize(new Dimension(150, 64));
		scrollPane.setViewportView(instancesTree);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 2;
		add(panel, gbc_panel);
		panel.setLayout(new GridLayout(1, 0, 0, 0));
		
		JButton btnAddInstance = new JButton("Add Instance");
		panel.add(btnAddInstance);
		
		JButton btnDeleteInstance = new JButton("Delete Instance");
		panel.add(btnDeleteInstance);
		
		initPanel();
	}
	
	private void initPanel()
	{
		ConfigurationFile config = experiment.getConfigurationFile();
		
		txtExperiment.setText(config.name);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Instances");
		instancesTree.setModel(new DefaultTreeModel(root));
		instancesTree.setRootVisible(false);
		instancesTree.setShowsRootHandles(true);
		
		tabbedPane.setTitleAt(0, "Instances");

		//Add each instance to the tree
		List<Object> instances = config.instances;
		for(Object inst : instances)
		{
			if(inst instanceof Instance)
			{
				DefaultMutableTreeNode child = new DefaultMutableTreeNode(inst);
				root.add(child);
				
				//Add the actions to the instance node
				for(Instance.Action action : ((Instance)inst).actions)
				{
					DefaultMutableTreeNode actionChild = new DefaultMutableTreeNode(action);
					child.add(actionChild);
				}
			}
			else if(inst instanceof Params)
				addParams((Params)inst);
		}
		
		//Start with the root node expanded
		instancesTree.expandPath(new TreePath(root.getPath()));
	}
	
	private void addParams(Params params)
	{
		JTree paramTree = new JTree();
		paramTree.setEditable(true);
		
		JScrollPane scrollPane = new JScrollPane(paramTree);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tabbedPane.addTab("Env", scrollPane);
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for(Param param : params.env)
		{
			DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(param.name);
			root.add(newChild);
			
			DefaultMutableTreeNode valueChild = new DefaultMutableTreeNode(param.value);
			newChild.add(valueChild);
		}
		DefaultTreeModel treeModel = new DefaultTreeModel(root);
		treeModel.addTreeModelListener(this);
		paramTree.setModel(treeModel);
	}

	@Override
	public void treeNodesChanged(TreeModelEvent arg0) {
		DefaultMutableTreeNode[] children = (DefaultMutableTreeNode[])arg0.getChildren();
	}

	@Override
	public void treeNodesInserted(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeStructureChanged(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
