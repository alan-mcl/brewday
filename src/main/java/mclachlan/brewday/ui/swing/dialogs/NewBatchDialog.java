package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.batch.Batch;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.LocalDateModel;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing port of JFX {@code NewBatchDialog}: brew date + recipe, creates batch via {@link Brewday#createNewBatch}.
 */
public class NewBatchDialog extends JDialog
{
	private final JComboBox<String> recipeCombo;
	private final JDatePicker datePicker;
	private final JLabel warningLabel;
	private final JButton okButton;
	private final JButton cancelButton;
	private Batch result;

	public NewBatchDialog(JFrame parent)
	{
		this(parent, null);
	}

	public NewBatchDialog(JFrame parent, String preselectedRecipe)
	{
		super(parent, getUiString("batch.add"), true);

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		LocalDateModel dateModel = new LocalDateModel(LocalDate.now());
		datePicker = new JDatePicker(dateModel);
		datePicker.setTextfieldColumns(14);

		recipeCombo = new JComboBox<>();
		java.util.ArrayList<String> recipes = new java.util.ArrayList<>(Database.getInstance().getRecipes().keySet());
		recipes.sort(String::compareTo);
		for (String r : recipes)
		{
			recipeCombo.addItem(r);
		}
		if (!recipes.isEmpty())
		{
			recipeCombo.setSelectedIndex(0);
		}
		if (preselectedRecipe != null)
		{
			recipeCombo.setSelectedItem(preselectedRecipe);
		}

		datePicker.setToolTipText(getUiString("batch.tooltip.new.date"));
		recipeCombo.setToolTipText(getUiString("batch.tooltip.new.recipe"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("batch.date") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(datePicker, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		content.add(new JLabel(getUiString("batch.recipe") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(recipeCombo, gbc);

		warningLabel = new JLabel(" ");
		warningLabel.setForeground(java.awt.Color.RED.darker());
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		content.add(warningLabel, gbc);

		okButton = new JButton(getUiString("ui.ok"));
		cancelButton = new JButton(getUiString("ui.cancel"));
		DialogButtonTooltips.wireOkCancel(okButton, cancelButton);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(okButton);
		buttons.add(cancelButton);

		getContentPane().add(content, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);

		Runnable updateOk = this::updateOkState;
		dateModel.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				updateOk.run();
			}
		});
		recipeCombo.addActionListener(e -> updateOk.run());

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());
		updateOkState();

		getRootPane().setDefaultButton(okButton);
		ActionHotkeySupport.bind(getRootPane(), javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "newBatch.cancel", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "newBatch.ok", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				if (okButton.isEnabled())
				{
					onOk();
				}
			}
		});

		datePicker.requestFocusInWindow();
	}

	private void updateOkState()
	{
		boolean noRecipes = recipeCombo.getItemCount() == 0;
		LocalDate ld = getSelectedLocalDate();
		boolean noDate = ld == null;

		if (noRecipes)
		{
			warningLabel.setText(getUiString("batch.new.dialog.no.recipes"));
		}
		else if (noDate)
		{
			warningLabel.setText(getUiString("batch.new.dialog.not.empty"));
		}
		else
		{
			warningLabel.setText(" ");
		}
		okButton.setEnabled(!noRecipes && !noDate);
	}

	private LocalDate getSelectedLocalDate()
	{
		if (!(datePicker.getModel() instanceof LocalDateModel m))
		{
			return null;
		}
		return m.getValue();
	}

	private void onOk()
	{
		if (recipeCombo.getItemCount() == 0)
		{
			return;
		}
		LocalDate ld = getSelectedLocalDate();
		if (ld == null)
		{
			return;
		}
		String recipeName = (String)recipeCombo.getSelectedItem();
		if (recipeName == null)
		{
			return;
		}
		result = Brewday.getInstance().createNewBatch(recipeName, ld);
		dispose();
	}

	public Batch getResult()
	{
		return result;
	}
}
