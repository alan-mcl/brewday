package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import mclachlan.brewday.recipe.IngredientAddition;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.UiUtils;
import mclachlan.brewday.ui.swing.app.SwingIcons;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Swing read-only port of JFX {@code RecipeTableView}: recipe ingredient bill of materials.
 */
public class SwingRecipeBillOfMaterialsPanel extends javax.swing.JPanel
{
	private final DefaultTableModel model;
	private final JTable table;

	public SwingRecipeBillOfMaterialsPanel()
	{
		super(new BorderLayout(0, 4));
		model = new DefaultTableModel(
			new String[] { "", getUiString("batch.tab.recipe.ingredient"), getUiString("batch.tab.recipe.quantity") }, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		table = new JTable(model);
		table.setRowHeight(22);
		table.getColumnModel().getColumn(0).setMaxWidth(28);
		table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column)
			{
				if (value instanceof Icon ic)
				{
					setIcon(ic);
					setText("");
				}
				else
				{
					setIcon(null);
					setText("");
				}
				setHorizontalAlignment(SwingConstants.CENTER);
				if (isSelected)
				{
					setBackground(t.getSelectionBackground());
				}
				else
				{
					setBackground(t.getBackground());
				}
				return this;
			}
		});
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	public void refresh(Recipe recipe)
	{
		model.setRowCount(0);
		if (recipe == null)
		{
			return;
		}
		Map<String, IngredientAddition> map = new HashMap<>();
		for (IngredientAddition ia : recipe.getIngredientsBillOfMaterials())
		{
			map.put(ia.getName(), ia);
		}
		List<IngredientAddition> list = new ArrayList<>(map.values());
		list.sort(UiUtils.getIngredientAdditionComparator());
		for (IngredientAddition ia : list)
		{
			model.addRow(new Object[] { iconFor(ia), ia.getName(), ia.describe() });
		}
	}

	private static Icon iconFor(IngredientAddition item)
	{
		return switch (item.getType())
		{
			case FERMENTABLES -> SwingIcons.toolbarIcon(SwingIcons.IconKey.FERMENTABLE);
			case HOPS -> SwingIcons.toolbarIcon(SwingIcons.IconKey.HOPS);
			case WATER -> SwingIcons.toolbarIcon(SwingIcons.IconKey.WATER);
			case YEAST -> SwingIcons.toolbarIcon(SwingIcons.IconKey.YEAST);
			case MISC -> SwingIcons.toolbarIcon(SwingIcons.IconKey.MISC);
		};
	}
}
