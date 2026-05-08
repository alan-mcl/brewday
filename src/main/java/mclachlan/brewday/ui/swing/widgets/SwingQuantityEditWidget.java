package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.util.StringUtils;

/**
 * Swing analogue of {@code mclachlan.brewday.ui.jfx.QuantityEditWidget}: a text field
 * for editing a {@link Quantity} in a fixed {@link Quantity.Unit}, with optional unit label.
 */
public class SwingQuantityEditWidget<T extends Quantity> extends JPanel
{
	private Quantity.Unit unit;
	private final JTextField textField;
	private JLabel unitLabel;
	private T quantity;
	private final boolean displayUnit;
	private final List<Consumer<T>> listeners = new ArrayList<>();

	public SwingQuantityEditWidget(Quantity.Unit unit)
	{
		this(unit, true);
	}

	public SwingQuantityEditWidget(Quantity.Unit unit, boolean displayUnit)
	{
		super(new BorderLayout(4, 0));
		this.unit = unit;
		this.displayUnit = displayUnit;
		this.textField = new JTextField();
		this.textField.setPreferredSize(new Dimension(80, textField.getPreferredSize().height));

		add(this.textField, BorderLayout.CENTER);

		if (displayUnit)
		{
			this.unitLabel = new JLabel(" " + StringUtils.getUiString("unit." + unit.name()));
			add(this.unitLabel, BorderLayout.EAST);
		}

		this.textField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				commitFromTextField();
			}
		});
	}

	public Quantity.Unit getUnit()
	{
		return unit;
	}

	public void setUnit(Quantity.Unit newUnit)
	{
		this.unit = newUnit;
		if (displayUnit && unitLabel != null)
		{
			unitLabel.setText(" " + StringUtils.getUiString("unit." + newUnit.name()));
		}
		refreshDisplayFromQuantity();
	}

	public void setQuantity(T value)
	{
		this.quantity = value;
		if (value == null)
		{
			textField.setText("");
		}
		else if (!textField.isFocusOwner())
		{
			double v = value.get(unit);
			textField.setText(StringUtils.format(v));
		}
	}

	@SuppressWarnings("unchecked")
	public T getQuantity()
	{
		try
		{
			return parseOrNull();
		}
		catch (NumberFormatException e)
		{
			return quantity;
		}
	}

	@SuppressWarnings("unchecked")
	public T parseOrNull() throws NumberFormatException
	{
		String text = textField.getText().trim();
		if (text.isEmpty())
		{
			return null;
		}
		return (T)Quantity.parseQuantity(text, unit);
	}

	public boolean isBlank()
	{
		return textField.getText().trim().isEmpty();
	}

	public JTextField getTextField()
	{
		return textField;
	}

	@Override
	public void setToolTipText(String text)
	{
		super.setToolTipText(text);
		textField.setToolTipText(text);
	}

	public void setEditable(boolean b)
	{
		textField.setEditable(b);
	}

	@Override
	public boolean requestFocusInWindow()
	{
		return textField.requestFocusInWindow();
	}

	public void selectAll()
	{
		textField.selectAll();
	}

	public void addQuantityChangeListener(Consumer<T> listener)
	{
		listeners.add(listener);
	}

	private void commitFromTextField()
	{
		T old = quantity;
		try
		{
			T parsed = parseOrNull();
			if (parsed == null)
			{
				quantity = null;
				textField.setText("");
			}
			else
			{
				quantity = parsed;
				double v = parsed.get(unit);
				textField.setText(StringUtils.format(v));
			}
			if (!java.util.Objects.equals(old, quantity))
			{
				notifyListeners(old, quantity);
			}
		}
		catch (NumberFormatException ex)
		{
			refreshDisplayFromQuantity();
		}
	}

	private void refreshDisplayFromQuantity()
	{
		if (textField.isFocusOwner())
		{
			return;
		}
		if (quantity == null)
		{
			textField.setText("");
		}
		else
		{
			double v = quantity.get(unit);
			textField.setText(StringUtils.format(v));
		}
	}

	private void notifyListeners(T oldValue, T newValue)
	{
		for (Consumer<T> listener : listeners)
		{
			listener.accept(newValue);
		}
	}

	public String getText()
	{
		return textField.getText();
	}

	public void setText(String text)
	{
		textField.setText(text == null ? "" : text);
	}
}
