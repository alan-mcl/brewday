package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Modal dialog that lets the user rename an output volume produced by a
 * process step. Mirrors {@link SwingRenameStepDialog} in structure: a single
 * pre-populated {@link JTextField}, a live validation label, and an OK/Cancel
 * button row. OK is disabled while validation fails.
 */
public class SwingRenameOutputVolumeDialog extends JDialog
{
	private final Recipe draft;
	private final String originalName;
	private final JTextField nameField;
	private final JLabel warningLabel;
	private final JButton okButton;

	private String output;

	public SwingRenameOutputVolumeDialog(Window parent, Recipe draft, String currentVolumeName)
	{
		super(parent, getUiString("volumes.rename.title"), ModalityType.APPLICATION_MODAL);
		this.draft = draft;
		this.originalName = currentVolumeName == null ? "" : currentVolumeName;

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		nameField = new JTextField(originalName, 24);
		nameField.selectAll();
		warningLabel = new JLabel(" ");

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("volumes.rename.label") + ":"), gbc);
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

		nameField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateOkState();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateOkState();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateOkState();
			}
		});
		updateOkState();

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(okButton);

		ActionHotkeySupport.bind(getRootPane(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"renameOutputVolume.cancel", new AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					dispose();
				}
			});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER),
			"renameOutputVolume.ok", new AbstractAction()
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

	/*-------------------------------------------------------------------------*/
	private void updateOkState()
	{
		String newValue = nameField.getText();
		boolean empty = newValue == null || newValue.trim().isEmpty();
		String trimmed = empty ? "" : newValue.trim();
		boolean unchanged = !empty && trimmed.equals(originalName);
		boolean duplicate = false;
		if (!empty && !unchanged)
		{
			for (String existing : draft.getAllVolumeNames())
			{
				if (existing != null && existing.equals(trimmed))
				{
					duplicate = true;
					break;
				}
			}
		}

		if (empty)
		{
			warningLabel.setText(getUiString("volumes.rename.validation.blank"));
		}
		else if (duplicate)
		{
			warningLabel.setText(getUiString("volumes.rename.validation.duplicate"));
		}
		else
		{
			warningLabel.setText(" ");
		}
		okButton.setEnabled(!empty && !unchanged && !duplicate);
	}

	/*-------------------------------------------------------------------------*/
	private void onOk()
	{
		output = nameField.getText().trim();
		dispose();
	}

	/*-------------------------------------------------------------------------*/
	public String getResult()
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
