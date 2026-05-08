package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.StringUtils;

/**
 * Swing analogue of {@code mclachlan.brewday.ui.jfx.QuantitySelectAndEditWidget}: quantity
 * text field plus a unit selector. When the unit changes, the displayed value is converted
 * so the numeric meaning is preserved (unlike the JFX widget's known quirk).
 */
public class SwingQuantitySelectAndEditWidget extends JPanel
{
	private Quantity.Unit unit;
	private final JTextField textField;
	private final JComboBox<Quantity.Unit> unitCombo;
	private double lastValidDisplayValue;

	public SwingQuantitySelectAndEditWidget(Quantity.Unit initialUnit, Quantity.Type... typesAllowed)
	{
		super(new BorderLayout(4, 0));
		if (typesAllowed == null || typesAllowed.length == 0)
		{
			throw new BrewdayException("SwingQuantitySelectAndEditWidget requires at least one Quantity.Type");
		}
		this.unit = initialUnit;
		this.textField = new JTextField();
		this.textField.setPreferredSize(new Dimension(80, textField.getPreferredSize().height));
		this.unitCombo = new JComboBox<>();

		List<Quantity.Unit> options = QuantityUnitOptions.unitsForTypes(typesAllowed);
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

		unitCombo.addActionListener(e -> onUnitSelectionChanged());
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
		List<Quantity.Unit> options = QuantityUnitOptions.unitsForTypes(typesAllowed);
		unitCombo.setModel(new DefaultComboBoxModel<>(options.toArray(new Quantity.Unit[0])));
		unitCombo.setSelectedItem(selected);
		if (unitCombo.getSelectedItem() == null && !options.isEmpty())
		{
			unitCombo.setSelectedIndex(0);
		}
		this.unit = (Quantity.Unit)unitCombo.getSelectedItem();
		refreshDisplayFromLastValid();
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
			return Quantity.parseQuantity(text, unit);
		}
		catch (Exception e)
		{
			return Quantity.parseQuantity(String.valueOf(lastValidDisplayValue), unit);
		}
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
			Quantity parsed = text.isEmpty() ? null : Quantity.parseQuantity(text, previous);
			this.unit = newUnit;
			if (parsed == null)
			{
				textField.setText("");
				lastValidDisplayValue = 0D;
			}
			else
			{
				double v = parsed.get(newUnit);
				lastValidDisplayValue = v;
				textField.setText(StringUtils.format(v));
			}
		}
		catch (NumberFormatException ex)
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
			Quantity parsed = Quantity.parseQuantity(text, unit);
			double v = parsed.get(unit);
			lastValidDisplayValue = v;
			textField.setText(StringUtils.format(v));
		}
		catch (NumberFormatException ex)
		{
			refreshDisplayFromLastValid();
		}
	}

	private void refreshDisplayFromLastValid()
	{
		if (textField.isFocusOwner())
		{
			return;
		}
		textField.setText(StringUtils.format(lastValidDisplayValue));
	}
}
