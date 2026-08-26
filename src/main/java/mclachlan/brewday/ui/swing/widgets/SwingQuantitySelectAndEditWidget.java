package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.StringUtils;

/**
 * Quantity text field plus a unit selector. When the unit changes, the displayed value is converted
 * so the numeric meaning is preserved.
 */
public class SwingQuantitySelectAndEditWidget extends JPanel
{
	private static final Pattern QUANTITY_TEXT =
		Pattern.compile("^([+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+))\\s*(.*)$");

	private Quantity.Unit unit;
	private Quantity.Type[] typesAllowed;
	private final JTextField textField;
	private final JComboBox<Quantity.Unit> unitCombo;
	private final ActionListener unitSelectionListener = e -> onUnitSelectionChanged();
	private double lastValidDisplayValue;
	private boolean suppressUnitSelectionHandler;

	public SwingQuantitySelectAndEditWidget(Quantity.Unit initialUnit, Quantity.Type... typesAllowed)
	{
		super(new BorderLayout(4, 0));
		if (typesAllowed == null || typesAllowed.length == 0)
		{
			throw new BrewdayException("SwingQuantitySelectAndEditWidget requires at least one Quantity.Type");
		}
		this.typesAllowed = typesAllowed.clone();
		this.unit = initialUnit;
		this.textField = new JTextField();
		this.textField.setPreferredSize(new Dimension(80, textField.getPreferredSize().height));
		this.unitCombo = new JComboBox<>();

		List<Quantity.Unit> options = QuantityUnitOptions.unitsForTypes(this.typesAllowed);
		if (options.isEmpty())
		{
			throw new BrewdayException("No units for given types");
		}
		unitCombo.setModel(new DefaultComboBoxModel<>(options.toArray(new Quantity.Unit[0])));
		unitCombo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
			{
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel label && value instanceof Quantity.Unit u)
				{
					label.setText(u.toString());
				}
				return c;
			}
		});

		add(textField, BorderLayout.CENTER);
		add(unitCombo, BorderLayout.EAST);

		unitCombo.setSelectedItem(initialUnit);
		if (unitCombo.getSelectedItem() == null && !options.isEmpty())
		{
			unitCombo.setSelectedIndex(0);
		}
		this.unit = (Quantity.Unit)unitCombo.getSelectedItem();

		this.textField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent ev)
			{
				commitFromTextField();
			}
		});

		unitCombo.addActionListener(unitSelectionListener);
	}

	public Quantity.Unit getUnit()
	{
		return unit;
	}

	public void setUnitOptions(Quantity.Unit selected, Quantity.Type... typesAllowed)
	{
		if (typesAllowed == null || typesAllowed.length == 0)
		{
			throw new BrewdayException("setUnitOptions requires at least one Quantity.Type");
		}
		this.typesAllowed = typesAllowed.clone();
		suppressUnitSelectionHandler = true;
		unitCombo.removeActionListener(unitSelectionListener);
		try
		{
			List<Quantity.Unit> options = QuantityUnitOptions.unitsForTypes(this.typesAllowed);
			unitCombo.setModel(new DefaultComboBoxModel<>(options.toArray(new Quantity.Unit[0])));
			unitCombo.setSelectedItem(selected);
			if (unitCombo.getSelectedItem() == null && !options.isEmpty())
			{
				unitCombo.setSelectedIndex(0);
			}
			this.unit = (Quantity.Unit)unitCombo.getSelectedItem();
			refreshDisplayFromLastValid();
		}
		finally
		{
			unitCombo.addActionListener(unitSelectionListener);
			suppressUnitSelectionHandler = false;
		}
	}

	public void setQuantity(Quantity quantity)
	{
		if (quantity == null)
		{
			textField.setText("");
			lastValidDisplayValue = 0D;
			return;
		}
		double v = quantity.get(unit);
		lastValidDisplayValue = v;
		if (!textField.isFocusOwner())
		{
			textField.setText(StringUtils.format(v));
		}
	}

	public Quantity getQuantity()
	{
		try
		{
			String text = textField.getText().trim();
			if (text.isEmpty())
			{
				return null;
			}
			ParseOutcome outcome = parseFreeText(text, unit, true);
			if (outcome == null)
			{
				return null;
			}
			applyDisplayUnit(outcome.displayUnit);
			return outcome.quantity;
		}
		catch (Exception e)
		{
			return Quantity.parseQuantity(String.valueOf(lastValidDisplayValue), unit);
		}
	}

	public Quantity parseOrNull() throws NumberFormatException
	{
		String text = textField.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		ParseOutcome outcome = parseFreeText(text, unit, true);
		if (outcome == null)
		{
			return null;
		}
		applyDisplayUnit(outcome.displayUnit);
		return outcome.quantity;
	}

	public JTextField getTextField()
	{
		return textField;
	}

	public JComboBox<Quantity.Unit> getUnitCombo()
	{
		return unitCombo;
	}

	private void onUnitSelectionChanged()
	{
		if (suppressUnitSelectionHandler)
		{
			return;
		}
		Quantity.Unit newUnit = (Quantity.Unit)unitCombo.getSelectedItem();
		if (newUnit == null)
		{
			return;
		}
		Quantity.Unit previous = this.unit;
		if (newUnit.equals(previous))
		{
			return;
		}
		try
		{
			String text = textField.getText().trim();
			this.unit = newUnit;
			if (text.isEmpty())
			{
				textField.setText("");
				lastValidDisplayValue = 0D;
				return;
			}
			if (previous.getType() == newUnit.getType())
			{
				Quantity parsed = parseFreeTextForUnit(text, previous);
				double v = parsed.get(newUnit);
				lastValidDisplayValue = v;
				textField.setText(StringUtils.format(v));
			}
			else
			{
				Quantity parsed = Brewday.getInstance().parseQuantity(text, newUnit.getType(), newUnit);
				if (parsed == null)
				{
					throw new NumberFormatException(text);
				}
				double v = parsed.get(newUnit);
				lastValidDisplayValue = v;
				textField.setText(StringUtils.format(v));
			}
		}
		catch (NumberFormatException | BrewdayException ex)
		{
			this.unit = newUnit;
			refreshDisplayFromLastValid();
		}
	}

	private void commitFromTextField()
	{
		try
		{
			String text = textField.getText().trim();
			if (text.isEmpty())
			{
				lastValidDisplayValue = 0D;
				return;
			}
			ParseOutcome outcome = parseFreeText(text, unit, true);
			applyDisplayUnit(outcome.displayUnit);
			double v = outcome.quantity.get(unit);
			lastValidDisplayValue = v;
			textField.setText(StringUtils.format(v));
		}
		catch (NumberFormatException ex)
		{
			refreshDisplayFromLastValid();
		}
	}

	private void applyDisplayUnit(Quantity.Unit displayUnit)
	{
		if (displayUnit == null || displayUnit.equals(unit))
		{
			return;
		}
		if (!comboContains(displayUnit))
		{
			return;
		}
		suppressUnitSelectionHandler = true;
		unitCombo.removeActionListener(unitSelectionListener);
		try
		{
			unitCombo.setSelectedItem(displayUnit);
			this.unit = displayUnit;
		}
		finally
		{
			unitCombo.addActionListener(unitSelectionListener);
			suppressUnitSelectionHandler = false;
		}
	}

	private ParseOutcome parseFreeText(String text, Quantity.Unit hintUnit, boolean allowComboSwitch)
		throws NumberFormatException
	{
		Matcher matcher = QUANTITY_TEXT.matcher(text.trim());
		if (!matcher.matches())
		{
			throw new NumberFormatException(text);
		}

		String suffix = matcher.group(2);
		boolean hasSuffix = suffix != null && !suffix.isBlank();

		if (!hasSuffix)
		{
			Quantity q = Brewday.getInstance().parseQuantity(text, hintUnit.getType(), hintUnit);
			if (q == null)
			{
				throw new NumberFormatException(text);
			}
			return new ParseOutcome(q, hintUnit);
		}

		Quantity.Unit resolvedUnit = null;
		Quantity.Type resolvedType = null;
		for (Quantity.Type type : typesAllowed)
		{
			Quantity.Unit u = Brewday.getInstance().resolveUnitSuffix(suffix, type);
			if (u != null)
			{
				resolvedUnit = u;
				resolvedType = type;
				break;
			}
		}

		if (resolvedUnit == null || resolvedType == null)
		{
			throw new NumberFormatException(text);
		}

		Quantity q = Brewday.getInstance().parseQuantity(text, resolvedType, resolvedUnit);
		if (q == null)
		{
			throw new NumberFormatException(text);
		}

		if (allowComboSwitch && comboContains(resolvedUnit))
		{
			return new ParseOutcome(q, resolvedUnit);
		}

		if (resolvedType == hintUnit.getType())
		{
			return new ParseOutcome(q, hintUnit);
		}

		throw new NumberFormatException(text);
	}

	private Quantity parseFreeTextForUnit(String text, Quantity.Unit hintUnit)
		throws NumberFormatException
	{
		ParseOutcome outcome = parseFreeText(text, hintUnit, false);
		return outcome.quantity;
	}

	private boolean comboContains(Quantity.Unit u)
	{
		for (int i = 0; i < unitCombo.getItemCount(); i++)
		{
			if (u.equals(unitCombo.getItemAt(i)))
			{
				return true;
			}
		}
		return false;
	}

	private void refreshDisplayFromLastValid()
	{
		if (textField.isFocusOwner())
		{
			return;
		}
		textField.setText(StringUtils.format(lastValidDisplayValue));
	}

	private static final class ParseOutcome
	{
		private final Quantity quantity;
		private final Quantity.Unit displayUnit;

		private ParseOutcome(Quantity quantity, Quantity.Unit displayUnit)
		{
			this.quantity = quantity;
			this.displayUnit = displayUnit;
		}
	}
}
