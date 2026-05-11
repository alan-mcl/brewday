package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mclachlan.brewday.ui.swing.app.SwingImportSupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingImportOptionsDialog extends JDialog
{
	private final BitSet options = new BitSet();
	private boolean approved;

	public SwingImportOptionsDialog(Frame owner, String title, List<SwingImportSupport.EntityOption> entityOptions)
	{
		super(owner, title, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel rows = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(2, 2, 2, 2);
		gbc.anchor = GridBagConstraints.WEST;

		rows.add(new JLabel(getUiString("tools.import.imported")), gbc);
		gbc.gridy++;

		List<JCheckBox> checkBoxes = new ArrayList<>();
		for (SwingImportSupport.EntityOption option : entityOptions)
		{
			JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
			line.add(new JLabel(option.getLabel() + ":"));

			JCheckBox addNew = new JCheckBox(
				getUiString("tools.import.imported." + uiKeySuffix(option.getLabel()) + ".new", option.getNewCount()),
				option.getNewCount() > 0);
			JCheckBox updateExisting = new JCheckBox(
				getUiString("tools.import.imported." + uiKeySuffix(option.getLabel()) + ".update", option.getUpdateCount()),
				false);

			addNew.addActionListener(e -> options.set(option.getNewBit().ordinal(), addNew.isSelected()));
			updateExisting.addActionListener(e -> options.set(option.getUpdateBit().ordinal(), updateExisting.isSelected()));

			options.set(option.getNewBit().ordinal(), addNew.isSelected());
			options.set(option.getUpdateBit().ordinal(), updateExisting.isSelected());
			line.add(addNew);
			line.add(updateExisting);
			checkBoxes.add(addNew);
			checkBoxes.add(updateExisting);

			rows.add(line, gbc);
			gbc.gridy++;
		}

		JScrollPane scroll = new JScrollPane(rows);
		add(scroll, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton ok = new JButton(getUiString("ui.ok"));
		JButton cancel = new JButton(getUiString("ui.cancel"));
		ok.addActionListener(e ->
		{
			approved = true;
			dispose();
		});
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);
		add(buttons, BorderLayout.SOUTH);

		setSize(760, 420);
		setLocationRelativeTo(owner);
	}

	public boolean isApproved()
	{
		return approved;
	}

	public BitSet getOptions()
	{
		return options;
	}

	private String uiKeySuffix(String label)
	{
		return switch (label)
		{
			case "Water" -> "water";
			case "Fermentables" -> "fermentable";
			case "Hops" -> "hop";
			case "Yeast" -> "yeast";
			case "Misc" -> "misc";
			case "Styles" -> "style";
			case "Equipment Profiles" -> "equipment";
			case "Water Parameters" -> "water.parameters";
			case "Process Templates" -> "process.template";
			case "Inventory" -> "inventory";
			case "Recipes" -> "recipe";
			case "Batches" -> "batch";
			default -> throw new IllegalArgumentException("Unsupported label: " + label);
		};
	}
}
