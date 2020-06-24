/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brewday.  If not, see <https://www.gnu.org/licenses/>.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ButtonType.*;

/**
 *
 */
public class PackagePane extends ProcessStepPane<PackageStep>
{
	private ComboBox<String> style;
	private TextField outputName;

	public PackagePane(TrackDirty parent, RecipeTreeViewModel stepsTreeModel)
	{
		super(parent, stepsTreeModel);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(ADD_FERMENTABLE, ADD_HOP, ADD_MISC, DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			PackageStep::getInputVolume,
			PackageStep::setInputVolume,
			Volume.Type.BEER);

		style = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString("recipe.style")));
		this.add(style, "wrap");

		getControlUtils().addVolumeUnitControl(this, "package.loss", PackageStep::getPackagingLoss, PackageStep::setPackagingLoss, Quantity.Unit.LITRES);

		outputName = new TextField();
		this.add(new Label(StringUtils.getUiString("package.beer.name")));
		this.add(outputName, "wrap");

		addComputedVolumePane("volumes.out", PackageStep::getOutputVolume);

		// -----
		style.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					getStep().setStyleId(newValue);
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

		outputName.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					getStep().setOutputVolume(newValue);
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});
	}

	@Override
	protected void refreshInternal(PackageStep step, Recipe recipe)
	{
		List<String> styles = new ArrayList<>(Database.getInstance().getStyles().keySet());
		styles.sort(String::compareTo);
		style.setItems(FXCollections.observableList(styles));

		if (step != null)
		{
			style.getSelectionModel().select(step.getStyleId());
			outputName.setText(step.getOutputVolume());
		}
	}
}
