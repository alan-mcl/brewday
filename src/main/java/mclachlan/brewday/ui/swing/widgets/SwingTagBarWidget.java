package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Compact Swing tag editor: chips and editable combo in one flow row (picker last).
 */
public class SwingTagBarWidget extends JPanel
{
	private static final String REMOVE_MARK = "\u00d7";
	private static final int VIEWPORT_HEIGHT = SwingIcons.TREE_ROW_HEIGHT * 2;

	private final DefaultComboBoxModel<String> pickerModel = new DefaultComboBoxModel<>();
	private final JComboBox<String> picker = new JComboBox<>(pickerModel);
	private final JPanel flowHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
	private Consumer<String> onAdd;
	private Consumer<String> onRemove;
	private final Set<String> currentTags = new HashSet<>();
	private List<String> knownSuggestions = List.of();
	private boolean suppressPickerAction;

	public SwingTagBarWidget()
	{
		super(new BorderLayout());
		picker.setEditable(true);
		picker.addActionListener(e -> tryAddFromPicker());

		flowHost.add(picker);

		JScrollPane scroll = new JScrollPane(flowHost);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(0, VIEWPORT_HEIGHT));
		scroll.setBorder(null);
		add(scroll, BorderLayout.CENTER);

		Component editor = picker.getEditor().getEditorComponent();
		if (editor instanceof JTextField tf)
		{
			tf.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						tryAddFromPicker();
						e.consume();
					}
				}
			});
		}
	}

	public void setAddButtonText(String text)
	{
		// Kept for callers; picker has no separate Add button.
	}

	public void setAddButtonTooltip(String text)
	{
		if (text != null && !text.isBlank())
		{
			String existing = picker.getToolTipText();
			if (existing == null || existing.isBlank())
			{
				picker.setToolTipText(text);
			}
		}
	}

	public void setInputTooltip(String text)
	{
		picker.setToolTipText(text);
		Component editor = picker.getEditor().getEditorComponent();
		if (editor instanceof JComponent jc)
		{
			jc.setToolTipText(text);
		}
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
		this.knownSuggestions = knownSuggestions != null ? knownSuggestions : List.of();
		clearChips();
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
		refreshPickerModel();
		flowHost.revalidate();
		flowHost.repaint();
	}

	private void clearChips()
	{
		for (int i = flowHost.getComponentCount() - 1; i >= 0; i--)
		{
			if (flowHost.getComponent(i) != picker)
			{
				flowHost.remove(i);
			}
		}
	}

	private void tryAddFromPicker()
	{
		if (suppressPickerAction)
		{
			return;
		}
		String raw = pickerEditorText();
		if (raw == null && picker.getSelectedItem() != null)
		{
			raw = String.valueOf(picker.getSelectedItem());
		}
		tryAdd(raw);
	}

	private void tryAdd(String raw)
	{
		if (raw == null)
		{
			return;
		}
		String t = raw.trim();
		if (t.isEmpty() || currentTags.contains(t))
		{
			clearPickerEditor();
			return;
		}
		if (onAdd != null)
		{
			onAdd.accept(t);
		}
		currentTags.add(t);
		addChip(t);
		refreshPickerModel();
		flowHost.revalidate();
		flowHost.repaint();
	}

	private String pickerEditorText()
	{
		Component editor = picker.getEditor().getEditorComponent();
		if (editor instanceof JTextField tf)
		{
			return tf.getText();
		}
		Object item = picker.getEditor().getItem();
		return item == null ? null : String.valueOf(item);
	}

	private void clearPickerEditor()
	{
		picker.setSelectedIndex(-1);
		Component editor = picker.getEditor().getEditorComponent();
		if (editor instanceof JTextField tf)
		{
			tf.setText("");
		}
	}

	private void refreshPickerModel()
	{
		suppressPickerAction = true;
		try
		{
			pickerModel.removeAllElements();
			TreeSet<String> sorted = new TreeSet<>();
			for (String s : knownSuggestions)
			{
				if (s != null && !s.isBlank())
				{
					String t = s.trim();
					if (!currentTags.contains(t))
					{
						sorted.add(t);
					}
				}
			}
			for (String t : sorted)
			{
				pickerModel.addElement(t);
			}
			clearPickerEditor();
		}
		finally
		{
			suppressPickerAction = false;
		}
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for {@link SwingTagBarWidgetTest}. */

	JTextField getInputFieldForTest()
	{
		Component editor = picker.getEditor().getEditorComponent();
		return editor instanceof JTextField tf ? tf : null;
	}

	void triggerAddForTest()
	{
		tryAddFromPicker();
	}

	void selectPickerItemForTest(String tag)
	{
		picker.setSelectedItem(tag);
		tryAddFromPicker();
	}

	int pickerItemCountForTest()
	{
		return pickerModel.getSize();
	}

	boolean pickerContainsForTest(String tag)
	{
		for (int i = 0; i < pickerModel.getSize(); i++)
		{
			if (tag.equals(pickerModel.getElementAt(i)))
			{
				return true;
			}
		}
		return false;
	}

	void clickFirstRemoveForTest()
	{
		for (Component c : flowHost.getComponents())
		{
			if (c == picker)
			{
				continue;
			}
			if (c instanceof JPanel chip)
			{
				for (Component b : chip.getComponents())
				{
					if (b instanceof JButton jb && REMOVE_MARK.equals(jb.getText()))
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
		chip.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(chip.getForeground()),
			new EmptyBorder(2, 6, 2, 2)));
		JLabel label = new JLabel(tag);
		JButton remove = new JButton(REMOVE_MARK);
		remove.setMargin(new java.awt.Insets(0, 2, 0, 2));
		remove.setBorderPainted(false);
		remove.setContentAreaFilled(false);
		remove.setFocusable(false);
		remove.setToolTipText(getUiString("recipe.tag.remove.tooltip"));
		remove.addActionListener(e ->
		{
			currentTags.remove(tag);
			flowHost.remove(chip);
			if (onRemove != null)
			{
				onRemove.accept(tag);
			}
			refreshPickerModel();
			flowHost.revalidate();
			flowHost.repaint();
		});
		chip.add(label);
		chip.add(remove);
		flowHost.add(chip, flowHost.getComponentCount() - 1);
	}
}
