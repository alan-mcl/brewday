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
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import alphanum.AlphanumComparator;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.math.CarbonationUnit;
import mclachlan.brewday.math.Quantity;
import mclachlan.brewday.process.PackageStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;

import static mclachlan.brewday.ui.jfx.ProcessStepPane.ToolbarButtonType.*;

/**
 *
 */
public class PackagePane extends ProcessStepPane<PackageStep>
{
	private ComboBox<String> style;
	private ComboBox<PackageStep.PackagingType> packagingType;
	private QuantityEditWidget<CarbonationUnit> forcedCarbonation;
	private TextField outputName;
	private Label outputNameValidationMessage;

	public PackagePane(TrackDirty parent, RecipeTreeView stepsTreeModel,
		boolean processTemplateMode)
	{
		super(parent, stepsTreeModel, processTemplateMode);
	}

	@Override
	protected void buildUiInternal()
	{
		addToolbar(new PackageStep().getSupportedIngredientAdditions(), RENAME_STEP, DUPLICATE, DELETE);

		addInputVolumeComboBox("volumes.in",
			PackageStep::getInputVolume,
			PackageStep::setInputVolume,
			Volume.Type.BEER);

		style = new ComboBox<>();
		this.add(new Label(StringUtils.getUiString("recipe.style")));
		this.add(style, "wrap");

		packagingType = new ComboBox<>(FXCollections.observableArrayList(PackageStep.PackagingType.values()));
		this.add(new Label(StringUtils.getUiString("package.type")));
		this.add(packagingType, "wrap");

		forcedCarbonation = new QuantityEditWidget<>(Quantity.Unit.VOLUMES);
		this.add(new Label(StringUtils.getUiString("package.forced.carbonation")));
		this.add(forcedCarbonation, "wrap");

		getUnitControlUtils().addVolumeUnitControl(
			this,
			"package.loss",
			PackageStep::getPackagingLoss,
			PackageStep::setPackagingLoss,
			Quantity.Unit.LITRES);

		outputName = new TextField();
		outputNameValidationMessage = new Label();
		this.add(new Label(StringUtils.getUiString("package.beer.name")));
		this.add(outputName, "wrap");
		this.add(new Label());
		this.add(outputNameValidationMessage, "wrap");

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

		packagingType.valueProperty().addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (newValue == PackageStep.PackagingType.BOTTLE)
				{
					forcedCarbonation.setDisable(true);
					forcedCarbonation.refresh(0);
				}
				else
				{
					forcedCarbonation.setDisable(false);
				}

				if (!refreshing)
				{
					getStep().setPackagingType(newValue);
				}

				if (detectDirty)
				{
					getParentTrackDirty().setDirty(getStep());
				}
			}
		});

		forcedCarbonation.addListener((observable, oldValue, newValue) ->
		{
			if (!refreshing)
			{
				getStep().setForcedCarbonation(forcedCarbonation.getQuantity());
			}

			if (detectDirty)
			{
				getParentTrackDirty().setDirty(getStep());
			}
		});

		outputName.textProperty().addListener((observable, oldValue, newValue) ->
		{
			if (getStep() != null && newValue != null)
			{
				if (!refreshing)
				{
					if (getStep().getRecipe().getVolumes().contains(newValue))
					{
						// prevent duplicate output volume names
						outputNameValidationMessage.setText(
							StringUtils.getUiString("package.beer.name.validation.duplicate"));
					}
					else
					{
						getStep().setOutputVolume(newValue);
						outputNameValidationMessage.setText("");
					}
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
		styles.sort(new AlphanumComparator());
		style.setItems(FXCollections.observableList(styles));

		if (step != null)
		{
			style.getSelectionModel().select(step.getStyleId());
			packagingType.getSelectionModel().select(step.getPackagingType());
			outputName.setText(step.getOutputVolume());

			if (step.getPackagingType() == PackageStep.PackagingType.BOTTLE)
			{
				forcedCarbonation.setDisable(true);
				forcedCarbonation.refresh(0);
			}
			else
			{
				forcedCarbonation.setDisable(false);
				forcedCarbonation.refresh(step.getForcedCarbonation());
			}
		}
	}
}
