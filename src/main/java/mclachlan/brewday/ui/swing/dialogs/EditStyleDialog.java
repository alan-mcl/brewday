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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import mclachlan.brewday.math.BitternessUnit;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.ColourUnit;
import mclachlan.brewday.math.DensityUnit;
import mclachlan.brewday.math.PercentageUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.style.Style;
import mclachlan.brewday.ui.swing.app.ActionHotkeySupport;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;
import mclachlan.brewday.ui.swing.widgets.SwingQuantityEditWidget;

import static mclachlan.brewday.util.StringUtils.getUiString;

public class EditStyleDialog extends JDialog
{
	private final boolean createMode;
	private final JTextField nameField;
	private final JTextField displayNameField;
	private final JTextField styleGuideField;
	private final JTextField categoryNumberField;
	private final JTextField categoryField;
	private final JTextField styleLetterField;
	private final JTextField styleGuideNameField;
	private final JComboBox<Style.Type> typeField;
	private final SwingQuantityEditWidget<DensityUnit> ogMinField;
	private final SwingQuantityEditWidget<DensityUnit> ogMaxField;
	private final SwingQuantityEditWidget<DensityUnit> fgMinField;
	private final SwingQuantityEditWidget<DensityUnit> fgMaxField;
	private final SwingQuantityEditWidget<BitternessUnit> ibuMinField;
	private final SwingQuantityEditWidget<BitternessUnit> ibuMaxField;
	private final SwingQuantityEditWidget<ColourUnit> colourMinField;
	private final SwingQuantityEditWidget<ColourUnit> colourMaxField;
	private final SwingQuantityEditWidget<CarbonationUnit> carbMinField;
	private final SwingQuantityEditWidget<CarbonationUnit> carbMaxField;
	private final SwingQuantityEditWidget<PercentageUnit> abvMinField;
	private final SwingQuantityEditWidget<PercentageUnit> abvMaxField;
	private final JTextArea notesArea;
	private final JTextArea profileArea;
	private final JTextArea ingredientsArea;
	private final JTextArea examplesArea;
	private Style result;

	public EditStyleDialog(JFrame parent, Style style, boolean createMode)
	{
		super(parent, getUiString(createMode ? "common.add" : "common.edit"), true);
		this.createMode = createMode;

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints mainGbc = new GridBagConstraints();
		mainGbc.insets = new Insets(4, 4, 4, 4);
		mainGbc.anchor = GridBagConstraints.NORTHWEST;

		nameField = field(style.getName());
		nameField.setEditable(createMode);
		displayNameField = field(style.getDisplayName());
		styleGuideField = field(style.getStyleGuide());
		categoryNumberField = field(style.getCategoryNumber());
		categoryField = field(style.getCategory());
		styleLetterField = field(style.getStyleLetter());
		styleGuideNameField = field(style.getStyleGuideName());
		typeField = new JComboBox<>(Style.Type.values());
		typeField.setSelectedItem(style.getType() == null ? Style.Type.ALE : style.getType());
		ogMinField = densityWidget(style.getOgMin());
		ogMaxField = densityWidget(style.getOgMax());
		fgMinField = densityWidget(style.getFgMin());
		fgMaxField = densityWidget(style.getFgMax());
		ibuMinField = ibuWidget(style.getIbuMin());
		ibuMaxField = ibuWidget(style.getIbuMax());
		colourMinField = srmWidget(style.getColourMin());
		colourMaxField = srmWidget(style.getColourMax());
		carbMinField = volsWidget(style.getCarbMin());
		carbMaxField = volsWidget(style.getCarbMax());
		abvMinField = percentWidget(style.getAbvMin());
		abvMaxField = percentWidget(style.getAbvMax());
		notesArea = new JTextArea(style.getNotes() == null ? "" : style.getNotes(), 6, 36);
		notesArea.setLineWrap(true);
		notesArea.setWrapStyleWord(true);
		profileArea = new JTextArea(style.getProfile() == null ? "" : style.getProfile(), 6, 36);
		profileArea.setLineWrap(true);
		profileArea.setWrapStyleWord(true);
		ingredientsArea = new JTextArea(style.getIngredients() == null ? "" : style.getIngredients(), 6, 36);
		ingredientsArea.setLineWrap(true);
		ingredientsArea.setWrapStyleWord(true);
		examplesArea = new JTextArea(style.getExamples() == null ? "" : style.getExamples(), 6, 36);
		examplesArea.setLineWrap(true);
		examplesArea.setWrapStyleWord(true);

		JPanel details = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		SwingDialogFormBuilder form = new SwingDialogFormBuilder(details, gbc, 1);
		form.addFieldRow(getUiString("style.name"), nameField);
		form.addFieldRow(getUiString("style.display.name"), displayNameField);
		form.addFieldRow(getUiString("style.guide"), styleGuideField);
		form.addFieldRow(getUiString("style.category.number"), categoryNumberField);
		form.addFieldRow(getUiString("style.category"), categoryField);
		form.addFieldRow(getUiString("style.letter"), styleLetterField);
		form.addFieldRow(getUiString("style.guide.name"), styleGuideNameField);
		form.addFieldRow(getUiString("style.type"), typeField);
		form.addSectionGap();
		form.addFieldRow(getUiString("style.og.min"), ogMinField);
		form.addFieldRow(getUiString("style.og.max"), ogMaxField);
		form.addFieldRow(getUiString("style.fg.min"), fgMinField);
		form.addFieldRow(getUiString("style.fg.max"), fgMaxField);
		form.addFieldRow(getUiString("style.ibu.min"), ibuMinField);
		form.addFieldRow(getUiString("style.ibu.max"), ibuMaxField);
		form.addFieldRow(getUiString("style.colour.min"), colourMinField);
		form.addFieldRow(getUiString("style.colour.max"), colourMaxField);
		form.addFieldRow(getUiString("style.carb.min"), carbMinField);
		form.addFieldRow(getUiString("style.carb.max"), carbMaxField);
		form.addFieldRow(getUiString("style.abv.min"), abvMinField);
		form.addFieldRow(getUiString("style.abv.max"), abvMaxField);
		form.addVerticalGlue();

		JPanel longText = new JPanel(new GridBagLayout());
		GridBagConstraints textGbc = new GridBagConstraints();
		textGbc.insets = new Insets(4, 4, 4, 4);
		textGbc.anchor = GridBagConstraints.NORTHWEST;
		addLongTextCell(longText, textGbc, 0, 0, "style.notes", notesArea);
		addLongTextCell(longText, textGbc, 1, 0, "style.profile", profileArea);
		addLongTextCell(longText, textGbc, 0, 2, "style.ingredients", ingredientsArea);
		addLongTextCell(longText, textGbc, 1, 2, "style.examples", examplesArea);
		longText.setPreferredSize(new Dimension(760, 420));

		mainGbc.gridx = 0;
		mainGbc.gridy = 0;
		mainGbc.fill = GridBagConstraints.BOTH;
		mainGbc.weightx = 1;
		mainGbc.weighty = 1;
		panel.add(details, mainGbc);
		mainGbc.gridx = 1;
		panel.add(longText, mainGbc);

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
		mainGbc.weighty = 0;
		mainGbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(buttons, mainGbc);

		setContentPane(panel);
		getRootPane().setDefaultButton(ok);
		ActionHotkeySupport.bind(this.getRootPane(), javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialog.cancel", new javax.swing.AbstractAction()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				dispose();
			}
		});
		bindCtrlEnterCommit(notesArea, "dialog.commit.from.notes");
		bindCtrlEnterCommit(profileArea, "dialog.commit.from.profile");
		bindCtrlEnterCommit(ingredientsArea, "dialog.commit.from.ingredients");
		bindCtrlEnterCommit(examplesArea, "dialog.commit.from.examples");
		pack();
		setResizable(false);
		setLocationRelativeTo(parent);
	}

	private JTextField field(String value)
	{
		return new JTextField(value == null ? "" : value);
	}

	private SwingQuantityEditWidget<DensityUnit> densityWidget(DensityUnit value)
	{
		SwingQuantityEditWidget<DensityUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.SPECIFIC_GRAVITY);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<BitternessUnit> ibuWidget(BitternessUnit value)
	{
		SwingQuantityEditWidget<BitternessUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.IBU);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<ColourUnit> srmWidget(ColourUnit value)
	{
		SwingQuantityEditWidget<ColourUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.SRM);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<CarbonationUnit> volsWidget(CarbonationUnit value)
	{
		SwingQuantityEditWidget<CarbonationUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.VOLUMES);
		w.setQuantity(value);
		return w;
	}

	private SwingQuantityEditWidget<PercentageUnit> percentWidget(PercentageUnit value)
	{
		SwingQuantityEditWidget<PercentageUnit> w = new SwingQuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
		w.setQuantity(value);
		return w;
	}

	private void bindCtrlEnterCommit(JTextArea area, String actionKey)
	{
		ActionHotkeySupport.bindFocused(area,
			javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
			actionKey,
			new javax.swing.AbstractAction()
			{
				@Override
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					onOk();
				}
			});
	}

	private void addLongTextCell(JPanel parent, GridBagConstraints c, int gridx, int gridy, String uiKey, JTextArea area)
	{
		c.gridx = gridx;
		c.gridy = gridy;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.weighty = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		parent.add(new javax.swing.JLabel(getUiString(uiKey) + ":"), c);

		c.gridy = gridy + 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		parent.add(new JScrollPane(area), c);
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
		Style style = new Style(name);
		style.setDisplayName(displayNameField.getText().trim());
		style.setStyleGuide(styleGuideField.getText().trim());
		style.setCategoryNumber(categoryNumberField.getText().trim());
		style.setCategory(categoryField.getText().trim());
		style.setStyleLetter(styleLetterField.getText().trim());
		style.setStyleGuideName(styleGuideNameField.getText().trim());
		style.setType((Style.Type)typeField.getSelectedItem());
		style.setOgMin(parseDensityOrShowError(ogMinField)); if (invalid(ogMinField, style.getOgMin())) return;
		style.setOgMax(parseDensityOrShowError(ogMaxField)); if (invalid(ogMaxField, style.getOgMax())) return;
		style.setFgMin(parseDensityOrShowError(fgMinField)); if (invalid(fgMinField, style.getFgMin())) return;
		style.setFgMax(parseDensityOrShowError(fgMaxField)); if (invalid(fgMaxField, style.getFgMax())) return;
		style.setIbuMin(parseIbuOrShowError(ibuMinField)); if (invalid(ibuMinField, style.getIbuMin())) return;
		style.setIbuMax(parseIbuOrShowError(ibuMaxField)); if (invalid(ibuMaxField, style.getIbuMax())) return;
		style.setColourMin(parseSrmOrShowError(colourMinField)); if (invalid(colourMinField, style.getColourMin())) return;
		style.setColourMax(parseSrmOrShowError(colourMaxField)); if (invalid(colourMaxField, style.getColourMax())) return;
		style.setCarbMin(parseVolsOrShowError(carbMinField)); if (invalid(carbMinField, style.getCarbMin())) return;
		style.setCarbMax(parseVolsOrShowError(carbMaxField)); if (invalid(carbMaxField, style.getCarbMax())) return;
		style.setAbvMin(parsePercentOrShowError(abvMinField)); if (invalid(abvMinField, style.getAbvMin())) return;
		style.setAbvMax(parsePercentOrShowError(abvMaxField)); if (invalid(abvMaxField, style.getAbvMax())) return;
		style.setNotes(notesArea.getText().trim());
		style.setProfile(profileArea.getText());
		style.setIngredients(ingredientsArea.getText());
		style.setExamples(examplesArea.getText().trim());
		result = style;
		dispose();
	}

	private boolean invalid(SwingQuantityEditWidget<?> field, Object value)
	{
		return value == null && !field.isBlank();
	}

	private DensityUnit parseDensityOrShowError(SwingQuantityEditWidget<DensityUnit> f)
	{
		return (DensityUnit)parseOrShowError(f);
	}

	private BitternessUnit parseIbuOrShowError(SwingQuantityEditWidget<BitternessUnit> f)
	{
		return (BitternessUnit)parseOrShowError(f);
	}

	private ColourUnit parseSrmOrShowError(SwingQuantityEditWidget<ColourUnit> f)
	{
		return (ColourUnit)parseOrShowError(f);
	}

	private CarbonationUnit parseVolsOrShowError(SwingQuantityEditWidget<CarbonationUnit> f)
	{
		return (CarbonationUnit)parseOrShowError(f);
	}

	private PercentageUnit parsePercentOrShowError(SwingQuantityEditWidget<PercentageUnit> f)
	{
		return (PercentageUnit)parseOrShowError(f);
	}

	private Quantity parseOrShowError(SwingQuantityEditWidget<?> field)
	{
		try
		{
			return field.parseOrNull();
		}
		catch (NumberFormatException e)
		{
			showValidationError(e);
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
		SwingUiErrors.showError(this, message, getUiString("ui.error"));
	}

	protected void showValidationError(Throwable t)
	{
		SwingUiErrors.showError(this, t, getUiString("ui.error"));
	}

	public Style getResult()
	{
		return result;
	}

	public boolean isCreateMode()
	{
		return createMode;
	}
}
