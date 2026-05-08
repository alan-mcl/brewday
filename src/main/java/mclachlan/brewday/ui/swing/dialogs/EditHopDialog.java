package mclachlan.brewday.ui.swing.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.ingredients.Hop;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditHopDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JComboBox<Hop.Type> typeField;
	private final JComboBox<Hop.Form> formField;
	private final JTextField originField;
	private final SwingQuantityEditWidget<PercentageUnit> alphaField;
	private final SwingQuantityEditWidget<PercentageUnit> betaField;
	private final SwingQuantityEditWidget<PercentageUnit> humuleneField;
	private final SwingQuantityEditWidget<PercentageUnit> caryophylleneField;
	private final SwingQuantityEditWidget<PercentageUnit> cohumuloneField;
	private final SwingQuantityEditWidget<PercentageUnit> myrceneField;
	private final SwingQuantityEditWidget<PercentageUnit> hopStorageIndexField;
	private final JTextField substitutesField;
	private final JTextArea descriptionArea;
	private Hop result;

	public EditHopDialog(JFrame parent, Hop hop, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(hop.getName());
		nameField.setEditable(createMode);
		typeField = new JComboBox<>(Hop.Type.values());
		typeField.setSelectedItem(hop.getType() == null ? Hop.Type.BOTH : hop.getType());
		formField = new JComboBox<>(Hop.Form.values());
		formField.setSelectedItem(hop.getForm() == null ? Hop.Form.PELLET : hop.getForm());
		originField = field(hop.getOrigin());
		alphaField = percentWidget(hop.getAlphaAcid());
		betaField = percentWidget(hop.getBetaAcid());
		humuleneField = percentWidget(hop.getHumulene());
		caryophylleneField = percentWidget(hop.getCaryophyllene());
		cohumuloneField = percentWidget(hop.getCohumulone());
		myrceneField = percentWidget(hop.getMyrcene());
		hopStorageIndexField = percentWidget(hop.getHopStorageIndex());
		substitutesField = field(hop.getSubstitutes());
		descriptionArea = new JTextArea(hop.getDescription() == null ? "" : hop.getDescription(), 14, 36);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);

		JPanel detailsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints detailsGbc = new GridBagConstraints();
		detailsGbc.insets = new Insets(4, 4, 4, 4);
		detailsGbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(detailsPanel, detailsGbc, 1);
		form.addFieldRow(getUiString("hop.name"), nameField);
		form.addSectionGap();
		form.addFieldRow(getUiString("hop.type"), typeField);
		form.addFieldRow(getUiString("hop.form"), formField);
		form.addFieldRow(getUiString("hop.origin"), originField);
		form.addSectionGap();
		form.addFieldRow(getUiString("hop.alpha"), alphaField);
		form.addFieldRow(getUiString("hop.beta"), betaField);
		form.addFieldRow(getUiString("hop.humulene"), humuleneField);
		form.addFieldRow(getUiString("hop.caryophyllene"), caryophylleneField);
		form.addFieldRow(getUiString("hop.cohumulone"), cohumuloneField);
		form.addFieldRow(getUiString("hop.myrcene"), myrceneField);
		form.addFieldRow(getUiString("hop.storage.index"), hopStorageIndexField);
		form.addSectionGap();
		form.addFieldRow(getUiString("hop.substitutes"), substitutesField);
		form.addVerticalGlue();

		JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
		descriptionScroll.setPreferredSize(new Dimension(360, 280));
		descriptionScroll.setMinimumSize(new Dimension(300, 220));
		JPanel descriptionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints descriptionGbc = new GridBagConstraints();
		descriptionGbc.insets = new Insets(4, 4, 4, 4);
		descriptionGbc.gridx = 0;
		descriptionGbc.gridy = 0;
		descriptionGbc.anchor = GridBagConstraints.NORTHWEST;
		descriptionPanel.add(new javax.swing.JLabel(getUiString("hop.desc") + ":"), descriptionGbc);
		descriptionGbc.gridy = 1;
		descriptionGbc.weightx = 1.0;
		descriptionGbc.weighty = 1.0;
		descriptionGbc.fill = GridBagConstraints.BOTH;
		descriptionPanel.add(descriptionScroll, descriptionGbc);

		mainGbc.gridx = 0;
		mainGbc.gridy = 0;
		mainGbc.weightx = 1.0;
		mainGbc.weighty = 1.0;
		mainGbc.fill = GridBagConstraints.BOTH;
		panel.add(detailsPanel, mainGbc);
		mainGbc.gridx = 1;
		mainGbc.weightx = 1.0;
		panel.add(descriptionPanel, mainGbc);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton ok = new JButton(getUiString("ui.ok"));
		ok.addActionListener(e -> onOk());
		JButton cancel = new JButton(getUiString("ui.cancel"));
		cancel.addActionListener(e -> dispose());
		buttons.add(ok);
		buttons.add(cancel);
		mainGbc.gridx = 0;
		mainGbc.gridy = 1;
		mainGbc.gridwidth = 2;
		mainGbc.weightx = 1.0;
		mainGbc.weighty = 0;
		mainGbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(buttons, mainGbc);

		setContentPane(panel);
		getRootPane().setDefaultButton(ok);
		ActionHotkeySupport.bind(this.getRootPane(),
			javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			"dialog.cancel",
			new javax.swing.AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					dispose();
				}
			});
		ActionHotkeySupport.bindFocused(descriptionArea,
			javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
			"dialog.commit.from.description",
			new javax.swing.AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					onOk();
				}
			});
		pack();
		setResizable(false);
		setLocationRelativeTo(parent);
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private SwingQuantityEditWidget<PercentageUnit> percentWidget(PercentageUnit value)
	{
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(value);
		return w;
	}

	private void onOk()
	{
		String name = nameField.getText().trim();
		if (name.isEmpty())
		{
			showValidationError(getUiString("ui.name"));
			focusForValidation(nameField);
			return;
		}

		Hop hop = new Hop(name);
		hop.setType((Hop.Type)typeField.getSelectedItem());
		hop.setForm((Hop.Form)formField.getSelectedItem());
		hop.setOrigin(originField.getText().trim());
		hop.setAlphaAcid(parsePercentOrShowError(alphaField));
		if (invalid(alphaField, hop.getAlphaAcid())) return;
		hop.setBetaAcid(parsePercentOrShowError(betaField));
		if (invalid(betaField, hop.getBetaAcid())) return;
		hop.setHumulene(parsePercentOrShowError(humuleneField));
		if (invalid(humuleneField, hop.getHumulene())) return;
		hop.setCaryophyllene(parsePercentOrShowError(caryophylleneField));
		if (invalid(caryophylleneField, hop.getCaryophyllene())) return;
		hop.setCohumulone(parsePercentOrShowError(cohumuloneField));
		if (invalid(cohumuloneField, hop.getCohumulone())) return;
		hop.setMyrcene(parsePercentOrShowError(myrceneField));
		if (invalid(myrceneField, hop.getMyrcene())) return;
		hop.setHopStorageIndex(parsePercentOrShowError(hopStorageIndexField));
		if (invalid(hopStorageIndexField, hop.getHopStorageIndex())) return;
		hop.setSubstitutes(substitutesField.getText().trim());
		hop.setDescription(descriptionArea.getText());
		result = hop;
		dispose();
	}

	private boolean invalid(SwingQuantityEditWidget<?> field, Object value)
	{
		return value == null && !field.isBlank();
	}

	private PercentageUnit parsePercentOrShowError(SwingQuantityEditWidget<PercentageUnit> field)
	{
		try
		{
			return field.parseOrNull();
		}
		catch (NumberFormatException e)
		{
			showValidationError(e.getMessage());
			focusForValidation(field);
			return null;
		}
	}

	protected void focusForValidation(Component field)
	{
		field.requestFocusInWindow();
		if (field instanceof JTextField jtf)
		{
			jtf.selectAll();
		}
		else if (field instanceof SwingQuantityEditWidget<?> w)
		{
			w.selectAll();
		}
	}

	protected void showValidationError(String message)
	{
		JOptionPane.showMessageDialog(this, message, getUiString("ui.error"), JOptionPane.ERROR_MESSAGE);
	}

	public Hop getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
