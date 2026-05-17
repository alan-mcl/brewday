package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import mclachlan.brewday.db.Database;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing analogue of JFX {@code ApplyNewProcessTemplateDialog}.
 */
public class SwingApplyNewProcessTemplateDialog extends JDialog
{
	private String output;

	public SwingApplyNewProcessTemplateDialog(Window parent)
	{
		super(parent, getUiString("recipe.apply.process.template"), Dialog.ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		List<String> names = new ArrayList<>(Database.getInstance().getProcessTemplates().keySet());
		Collections.sort(names);

		JComboBox<String> combo = new JComboBox<>();
		combo.setModel(new DefaultComboBoxModel<>(names.toArray(String[]::new)));

		JLabel blurb = new JLabel("<html><body style='width:320px'>" + getUiString("recipe.apply.process.template.msg2") + "</body></html>");

		JPanel form = new JPanel(new GridBagLayout());
		GridBagConstraints gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = 0;
		gl.anchor = GridBagConstraints.NORTHWEST;
		gl.insets = new Insets(4, 8, 4, 8);
		form.add(new JLabel(getUiString("recipe.process.template") + ":"), gl);
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 1;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1.0;
		gc.insets = new Insets(4, 8, 4, 8);
		form.add(combo, gc);

		gl = new GridBagConstraints();
		gl.gridx = 0;
		gl.gridy = 1;
		gl.gridwidth = 2;
		gl.fill = GridBagConstraints.HORIZONTAL;
		gl.weightx = 1.0;
		gl.insets = new Insets(8, 8, 8, 8);
		form.add(blurb, gl);

		if (!names.isEmpty())
		{
			combo.setSelectedIndex(0);
		}

		JPanel south = new JPanel();
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.setEnabled(!names.isEmpty());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		mclachlan.brewday.ui.swing.app.DialogButtonTooltips.wireOkCancel(ok, cancel);
		south.add(ok);
		south.add(cancel);

		ok.addActionListener(e ->
		{
			output = (String)combo.getSelectedItem();
			dispose();
		});
		cancel.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(ok);

		setLayout(new BorderLayout());
		add(form, BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);
	}

	public String getOutput()
	{
		return output;
	}
}
