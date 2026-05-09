package mclachlan.brewday.ui.swing.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import mclachlan.brewday.inventory.InventoryFacade;
import mclachlan.brewday.math.Quantity;
import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Confirmation + delta preview for consume or restore inventory (JFX {@code BatchEditor#popupDeltaDialog} parity).
 */
public class SwingBatchInventoryDeltaDialog extends JDialog
{
	private boolean accepted;

	public SwingBatchInventoryDeltaDialog(Window parent, String recipeName, boolean consume)
	{
		super(parent, getUiString("batch.consume.inventory"), ModalityType.APPLICATION_MODAL);

		java.util.List<InventoryFacade.InventoryLineItemDelta> deltas =
			InventoryFacade.getInventoryDelta(recipeName, true);

		DefaultTableModel tm = new DefaultTableModel(
			new String[] {
				getUiString("batch.consume.table.ingredient"),
				consume ? getUiString("batch.consume.table.consumed") : getUiString("batch.consume.table.restored"),
				getUiString("batch.consume.table.in.inventory")
			}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};

		for (InventoryFacade.InventoryLineItemDelta d : deltas)
		{
			tm.addRow(new Object[] {
				d.getInventoryId(),
				formatQty(d.getDelta(), d.getUnit()),
				formatQty(d.getInInventory(), d.getUnit())
			});
		}

		JTable table = new JTable(tm);
		table.setRowHeight(22);

		JPanel north = new JPanel(new BorderLayout(4, 4));
		north.add(new JLabel(getUiString("batch.consume.inventory.confirm")), BorderLayout.NORTH);
		north.add(new JLabel(getUiString("batch.consume.inventory.delta")), BorderLayout.SOUTH);

		JButton ok = new JButton(getUiString("ui.ok"));
		JButton cancel = new JButton(getUiString("ui.cancel"));
		ok.addActionListener(e -> {
			accepted = true;
			dispose();
		});
		cancel.addActionListener(e -> dispose());

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(ok);
		buttons.add(cancel);

		add(north, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(buttons, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(parent);
		setSize(Math.min(800, getWidth() + 100), 500);
	}

	public boolean isAccepted()
	{
		return accepted;
	}

	private static String formatQty(Quantity q, Quantity.Unit unit)
	{
		if (q == null)
		{
			return "";
		}
		return mclachlan.brewday.util.StringUtils.format(q.get(unit)) + " " + unit.abbr();
	}
}
