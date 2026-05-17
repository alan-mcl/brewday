package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.DialogButtonTooltips;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class SwingNewStepDialog extends JDialog
{
	private ProcessStep.Type result;
	private final JComboBox<ProcessStep.Type> stepTypeCombo;
	private final JTextArea stepDesc;
	private final JButton okButton;

	public SwingNewStepDialog(JFrame parent)
	{
		super(parent, getUiString("recipe.add.step"), true);

		JPanel content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		List<ProcessStep.Type> stepTypes = new ArrayList<>(List.of(ProcessStep.Type.values()));
		stepTypes.sort(Comparator.comparingInt(ProcessStep.Type::getSortOrder));
		stepTypeCombo = new JComboBox<>(stepTypes.toArray(new ProcessStep.Type[0]));

		stepDesc = new JTextArea(4, 32);
		stepDesc.setEditable(false);
		stepDesc.setLineWrap(true);
		stepDesc.setWrapStyleWord(true);
		updateDescription();

		gbc.gridx = 0;
		gbc.gridy = 0;
		content.add(new JLabel(getUiString("process.step.type") + ":"), gbc);
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		content.add(stepTypeCombo, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1.0;
		content.add(new javax.swing.JScrollPane(stepDesc), gbc);

		stepTypeCombo.addActionListener(e -> updateDescription());

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

		okButton.addActionListener(e -> onOk());
		cancelButton.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(okButton);

		ActionHotkeySupport.bind(getRootPane(), javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "newStep.cancel", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		ActionHotkeySupport.bind(getRootPane(), ActionHotkeySupport.ctrlOrCmd(KeyEvent.VK_ENTER), "newStep.ok", new AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				onOk();
			}
		});
	}

	private void updateDescription()
	{
		ProcessStep.Type t = (ProcessStep.Type)stepTypeCombo.getSelectedItem();
		if (t != null)
		{
			stepDesc.setText(getUiString(t.getDescKey()));
		}
	}

	private void onOk()
	{
		result = (ProcessStep.Type)stepTypeCombo.getSelectedItem();
		dispose();
	}

	public ProcessStep.Type getResult()
	{
		return result;
	}

	/*-------------------------------------------------------------------------*/
	/** Package-local hooks for {@link SwingNewStepDialogTest} (no modal display). */

	ProcessStep.Type peekSelectedStepType()
	{
		return (ProcessStep.Type)stepTypeCombo.getSelectedItem();
	}

	String peekDescriptionText()
	{
		return stepDesc.getText();
	}

	boolean isOkEnabled()
	{
		return okButton.isEnabled();
	}

	void confirmForTest()
	{
		onOk();
	}

	void cancelForTest()
	{
		dispose();
	}

	javax.swing.JComboBox<ProcessStep.Type> getStepTypeComboForTest()
	{
		return stepTypeCombo;
	}
}
