package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Water;

import static mclachlan.brewday.StringUtils.getUiString;

/**
 *
 */
public class WaterTable extends VBox
{

	private final TableView table;

	public WaterTable()
	{
		table = new TableView();

		TableColumn<String, Water> name = getStringWaterTableColumn("water.name", "name");
		name.setSortType(TableColumn.SortType.ASCENDING);
		table.getColumns().add(name);

		table.getColumns().add(getStringWaterTableColumn("water.calcium", "calcium"));
		table.getColumns().add(getStringWaterTableColumn("water.bicarbonate", "bicarbonate"));
		table.getColumns().add(getStringWaterTableColumn("water.sulfate", "sulfate"));
		table.getColumns().add(getStringWaterTableColumn("water.chloride", "chloride"));
		table.getColumns().add(getStringWaterTableColumn("water.sodium", "sodium"));
		table.getColumns().add(getStringWaterTableColumn("water.magnesium", "magnesium"));
		table.getColumns().add(getStringWaterTableColumn("water.ph", "ph"));

		this.getChildren().add(table);
	}

	public void refresh(Database database)
	{
		Map<String, Water> waters = database.getWaters();

		table.getItems().clear();
		ArrayList<Water> list = new ArrayList<>(waters.values());
		list.sort(Comparator.comparing(Water::getName));
		table.getItems().addAll(list);

	}

	private TableColumn<String, Water> getStringWaterTableColumn(String heading, String property)
	{
		TableColumn<String, Water> name = new TableColumn<>(getUiString(heading));
		name.setCellValueFactory(new PropertyValueFactory<>(property));
		return name;
	}
}
