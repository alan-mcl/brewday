package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingRenameRecipeDialog extends JDialog
{
	private String output;
	private final JTextField recipeName;
	private final JLabel warningLabel;
	private final JButton okButton;
	private final String originalName;

	public SwingRenameRecipeDialog(JFrame parent, Recipe current)
	{
		super(parent, getUiString("recipe.rename"), true);
		this.originalName = current.getName();

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		recipeName = new JTextField(current.getName(), 24);
		recipeName.selectAll();
		warningLabel = new JLabel(" ");

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("recipe.name") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(recipeName, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		content.add(warningLabel, gbc);

		okButton = new JButton(getUiString("ui.ok"));
		JButton cancelButton = new JButton(getUiString("ui.cancel"));
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(okButton);
		buttons.add(cancelButton);

		getContentPane().add(content, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);

		Runnable updateOk = this::updateOkState;
		recipeName.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateOk.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateOk.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateOk.run();
			}
		});
		updateOkState();

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(okButton);

		ActionHotkeySupport.bind(getRootPane(), javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "renameRecipe.cancel", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "renameRecipe.ok", new AbstractAction()
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
	}

	private void updateOkState()
	{
		String newValue = recipeName.getText();
		boolean empty = newValue == null || newValue.trim().isEmpty();
		String trimmed = empty ? "" : newValue.trim();
		boolean exists = !empty && Database.getInstance().getRecipes().containsKey(trimmed)
			&& !trimmed.equalsIgnoreCase(originalName);
		if (empty)
		{
			warningLabel.setText(getUiString("recipe.new.dialog.not.empty"));
		}
		else if (exists)
		{
			warningLabel.setText(getUiString("recipe.new.dialog.already.exists"));
		}
		else
		{
			warningLabel.setText(" ");
		}
		okButton.setEnabled(!empty && !exists);
	}

	private void onOk()
	{
		output = recipeName.getText().trim();
		dispose();
	}

	public String getResult()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for tests (no modal display). */

	void setNameFieldForTest(String s)
	{
		recipeName.setText(s);
	}

	boolean isOkEnabledForTest()
	{
		return okButton.isEnabled();
	}

	void confirmForTest()
	{
		onOk();
	}
}
