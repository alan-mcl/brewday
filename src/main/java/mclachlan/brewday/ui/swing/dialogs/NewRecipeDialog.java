package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;
import javax.swing.AbstractAction;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing equivalent of JFX {@code NewRecipeDialog}: name + process template, live validation.
 */
public class NewRecipeDialog extends JDialog
{
	private final JTextField recipeName;
	private final JComboBox<String> processTemplate;
	private final JLabel warningLabel;
	private final JButton okButton;
	private final JButton cancelButton;
	private Recipe result;

	public NewRecipeDialog(JFrame parent)
	{
		super(parent, getUiString("recipe.add"), true);

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		recipeName = new JTextField(24);
		processTemplate = new JComboBox<>();
		ArrayList<String> templates = new ArrayList<>(Database.getInstance().getProcessTemplates().keySet());
		templates.sort(String::compareTo);
		for (String t : templates)
		{
			processTemplate.addItem(t);
		}
		if (!templates.isEmpty())
		{
			processTemplate.setSelectedIndex(0);
		}

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("recipe.name") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(recipeName, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		content.add(new JLabel(getUiString("recipe.process.template") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(processTemplate, gbc);

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

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());
		updateOkState();

		getRootPane().setDefaultButton(okButton);
		ActionHotkeySupport.bind(getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "newRecipe.cancel", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "newRecipe.ok", new AbstractAction()
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

		recipeName.requestFocusInWindow();
	}

	private void updateOkState()
	{
		String name = recipeName.getText();
		boolean empty = name == null || name.trim().isEmpty();
		boolean exists = !empty && Database.getInstance().getRecipes().containsKey(name.trim());
		boolean noTemplate = processTemplate.getItemCount() == 0;

		if (empty)
		{
			warningLabel.setText(getUiString("recipe.new.dialog.not.empty"));
		}
		else if (exists)
		{
			warningLabel.setText(getUiString("recipe.new.dialog.already.exists"));
		}
		else if (noTemplate)
		{
			warningLabel.setText(getUiString("recipe.new.dialog.no.templates"));
		}
		else
		{
			warningLabel.setText(" ");
		}
		okButton.setEnabled(!empty && !exists && !noTemplate);
	}

	private void onOk()
	{
		String name = recipeName.getText().trim();
		if (name.isEmpty() || Database.getInstance().getRecipes().containsKey(name))
		{
			return;
		}
		String tpl = (String)processTemplate.getSelectedItem();
		if (tpl == null)
		{
			return;
		}
		result = Brewday.getInstance().createNewRecipe(name, tpl);
		dispose();
	}

	public Recipe getResult()
	{
		return result;
	}
}
