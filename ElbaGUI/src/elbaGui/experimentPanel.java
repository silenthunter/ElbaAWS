package elbaGui;

import javax.swing.JPanel;

import elbaEC2.experiments.Experiment;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;

public class experimentPanel extends JPanel
{
	
	Experiment experiment;
	private JTextField txtConfigurationFile;
	private JTextField txtExperiment;

	/**
	 * Create the panel.
	 */
	public experimentPanel(Experiment experiment)
	{
		this.experiment = experiment;
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{177, 91, 1, 0};
		gridBagLayout.rowHeights = new int[]{14, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblExperimentName = new JLabel("Experiment Name: ");
		GridBagConstraints gbc_lblExperimentName = new GridBagConstraints();
		gbc_lblExperimentName.anchor = GridBagConstraints.NORTH;
		gbc_lblExperimentName.insets = new Insets(0, 0, 5, 5);
		gbc_lblExperimentName.gridx = 0;
		gbc_lblExperimentName.gridy = 1;
		add(lblExperimentName, gbc_lblExperimentName);
		
		txtExperiment = new JTextField();
		GridBagConstraints gbc_txtExperiment = new GridBagConstraints();
		gbc_txtExperiment.anchor = GridBagConstraints.NORTH;
		gbc_txtExperiment.insets = new Insets(0, 0, 5, 5);
		gbc_txtExperiment.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtExperiment.gridx = 1;
		gbc_txtExperiment.gridy = 1;
		add(txtExperiment, gbc_txtExperiment);
		txtExperiment.setColumns(10);
		
		JLabel lblConfigurationFile = new JLabel("Configuration File: ");
		GridBagConstraints gbc_lblConfigurationFile = new GridBagConstraints();
		gbc_lblConfigurationFile.insets = new Insets(0, 0, 0, 5);
		gbc_lblConfigurationFile.gridx = 0;
		gbc_lblConfigurationFile.gridy = 2;
		add(lblConfigurationFile, gbc_lblConfigurationFile);
		
		txtConfigurationFile = new JTextField();
		GridBagConstraints gbc_txtConfigurationFile = new GridBagConstraints();
		gbc_txtConfigurationFile.insets = new Insets(0, 0, 0, 5);
		gbc_txtConfigurationFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtConfigurationFile.gridx = 1;
		gbc_txtConfigurationFile.gridy = 2;
		add(txtConfigurationFile, gbc_txtConfigurationFile);
		txtConfigurationFile.setColumns(255);
	}

}
