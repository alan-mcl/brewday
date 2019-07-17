package mclachlan.brewday.ui.swing;

import javax.swing.*;

class LabelIcon
{
	private Icon icon;
	private String label;

	public LabelIcon(Icon icon, String label)
	{
		this.icon = icon;
		this.label = label;
	}

	public Icon getIcon()
	{
		return icon;
	}

	public void setIcon(Icon icon)
	{
		this.icon = icon;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}
}
