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
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingDuplicateStepDialog extends JDialog
{
	private ProcessStep output;
	private final JTextField nameField;
	private final JLabel warningLabel;
	private final JButton okButton;
	private final Recipe draft;
	private final ProcessStep current;

	public SwingDuplicateStepDialog(JFrame parent, Recipe draft, ProcessStep current)
	{
		super(parent, getUiString("process.step.duplicate"), true);
		this.draft = draft;
		this.current = current;

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		nameField = new JTextField("", 24);
		warningLabel = new JLabel(getUiString("process.step.dialog.not.empty"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("process.step.name") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(nameField, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		content.add(warningLabel, gbc);

		okButton = new JButton(getUiString("ui.ok"));
		JButton cancelButton = new JButton(getUiString("ui.cancel"));
		DialogButtonTooltips.wireOkCancel(okButton, cancelButton);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(okButton);
		buttons.add(cancelButton);

		getContentPane().add(content, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);

		Runnable updateOk = this::updateOkState;
		nameField.getDocument().addDocumentListener(new DocumentListener()
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

		ActionHotkeySupport.bind(getRootPane(), javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dupStep.cancel", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "dupStep.ok", new AbstractAction()
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
		String newValue = nameField.getText();
		boolean empty = newValue == null || newValue.trim().isEmpty();
		String trimmed = empty ? "" : newValue.trim();
		boolean exists = !empty && draft.containsStepWithName(trimmed);
		if (empty)
		{
			warningLabel.setText(getUiString("process.step.dialog.not.empty"));
		}
		else if (exists)
		{
			warningLabel.setText(getUiString("process.step.dialog.already.exists"));
		}
		else
		{
			warningLabel.setText(" ");
		}
		okButton.setEnabled(!empty && !exists);
	}

	private void onOk()
	{
		String newName = nameField.getText().trim();
		output = current.clone(newName);
		output.setName(newName);
		dispose();
	}

	public ProcessStep getResult()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for tests (no modal display). */

	void setNameFieldForTest(String s)
	{
		nameField.setText(s);
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
