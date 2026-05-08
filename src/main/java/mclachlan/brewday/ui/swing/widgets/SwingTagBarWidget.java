package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Minimal Swing tag editor (add text field + chip strip with remove).
 */
public class SwingTagBarWidget extends JPanel
{
	private final JTextField input = new JTextField(12);
	private final JButton addButton = new JButton();
	private final JPanel chipStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
	private Consumer<String> onAdd;
	private Consumer<String> onRemove;
	private final Set<String> currentTags = new HashSet<>();

	public SwingTagBarWidget()
	{
		super(new BorderLayout(4, 4));
		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		north.add(input);
		north.add(addButton);
		add(north, BorderLayout.NORTH);
		add(chipStrip, BorderLayout.CENTER);
		addButton.addActionListener(e -> tryAdd());
	}

	public void setAddButtonText(String text)
	{
		addButton.setText(text);
	}

	public void setOnAdd(Consumer<String> onAdd)
	{
		this.onAdd = onAdd;
	}

	public void setOnRemove(Consumer<String> onRemove)
	{
		this.onRemove = onRemove;
	}

	public void setTags(List<String> current, List<String> knownSuggestions)
	{
		chipStrip.removeAll();
		currentTags.clear();
		if (current != null)
		{
			for (String t : current)
			{
				if (t != null && !t.isBlank())
				{
					currentTags.add(t.trim());
					addChip(t.trim());
				}
			}
		}
		chipStrip.revalidate();
		chipStrip.repaint();
	}

	private void tryAdd()
	{
		String raw = input.getText();
		if (raw == null)
		{
			return;
		}
		String t = raw.trim();
		if (t.isEmpty() || currentTags.contains(t))
		{
			return;
		}
		if (onAdd != null)
		{
			onAdd.accept(t);
		}
		currentTags.add(t);
		addChip(t);
		input.setText("");
		chipStrip.revalidate();
		chipStrip.repaint();
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for {@link SwingTagBarWidgetTest}. */

	JTextField getInputFieldForTest()
	{
		return input;
	}

	void triggerAddForTest()
	{
		tryAdd();
	}

	void clickFirstRemoveForTest()
	{
		for (Component c : chipStrip.getComponents())
		{
			if (c instanceof JPanel chip)
			{
				for (Component b : chip.getComponents())
				{
					if (b instanceof JButton jb && "x".equals(jb.getText()))
					{
						jb.doClick();
						return;
					}
				}
			}
		}
	}

	private void addChip(String tag)
	{
		JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		JLabel label = new JLabel(tag);
		JButton remove = new JButton("x");
		remove.setMargin(new java.awt.Insets(0, 2, 0, 2));
		remove.setToolTipText(getUiString("recipe.tag.remove.tooltip"));
		remove.addActionListener(e ->
		{
			currentTags.remove(tag);
			chipStrip.remove(chip);
			if (onRemove != null)
			{
				onRemove.accept(tag);
			}
			chipStrip.revalidate();
			chipStrip.repaint();
		});
		chip.add(label);
		chip.add(remove);
		chipStrip.add(chip);
	}
}
