package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Simple helper for consistent Swing dialog row layout.
 */
class SwingDialogFormBuilder
{
	private final JPanel panel;
	private final GridBagConstraints gbc;
	private final int valueColumnCount;
	private int row;

	SwingDialogFormBuilder(JPanel panel, GridBagConstraints gbc, int valueColumnCount)
	{
		this.panel = panel;
		this.gbc = gbc;
		this.valueColumnCount = valueColumnCount;
		this.row = 0;
	}

	void addFieldRow(String label, Component component)
	{
		addLabel(label);

		gbc.gridx = 1;
		gbc.gridy = row;
		gbc.gridwidth = valueColumnCount;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, gbc);

		nextRow();
	}

	void addLabel(int column, String text)
	{
		gbc.gridx = column;
		gbc.gridy = row;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		panel.add(new JLabel(text), gbc);
	}

	void addComponent(int column, int width, Component component)
	{
		gbc.gridx = column;
		gbc.gridy = row;
		gbc.gridwidth = width;
		gbc.weightx = width > 0 ? 1.0 : 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, gbc);
	}

	void addSectionGap()
	{
		gbc.insets = new Insets(10, 4, 4, 4);
	}

	void resetInsets()
	{
		gbc.insets = new Insets(4, 4, 4, 4);
	}

	void nextRow()
	{
		row++;
		resetInsets();
	}

	private void addLabel(String label)
	{
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		String text = label == null || label.isEmpty() ? " " : label + ":";
		panel.add(new JLabel(text), gbc);
	}
}
