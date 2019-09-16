package mclachlan.brewday.ui.swing;

import java.awt.Component;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

class LabelIconRenderer extends DefaultTableCellRenderer
{
	public LabelIconRenderer()
	{
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object
		value, boolean isSelected, boolean hasFocus, int row, int col)
	{
		JLabel r = (JLabel)super.getTableCellRendererComponent(
			table, value, isSelected, hasFocus, row, col);
		setIcon(((LabelIcon)value).getIcon());
		setText(((LabelIcon)value).getLabel());
		return r;
	}
}
