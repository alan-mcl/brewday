
/*
 * This file is part of brewday.
 *
 * brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.jfx;

import java.util.*;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class TargetMashTempDialog extends Dialog<Boolean>
{
	private final TargetMashTempPane targetMashTempPane;
	private boolean output;

	/*-------------------------------------------------------------------------*/
	public TargetMashTempDialog(PhUnit currentPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill,
		TemperatureUnit grainTemp)
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(Icons.temperatureIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("tools.mash.temp"));
		this.setGraphic(JfxUi.getImageView(Icons.temperatureIcon, 32));

		MigPane content = new MigPane();

		targetMashTempPane = new TargetMashTempPane(currentPh, mashWater, grainBill, grainTemp);

		content.add(targetMashTempPane);

		this.getDialogPane().setContent(content);

		output = false;

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = true;
		});
	}

	/*-------------------------------------------------------------------------*/

	public TemperatureUnit getTemp()
	{
		return targetMashTempPane.getTemp();
	}

	/*-------------------------------------------------------------------------*/

	public boolean getOutput()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	private static class TargetMashTempPane extends MigPane
	{
		private QuantityEditWidget<TemperatureUnit> waterTemp, targetTemp;

		/*-------------------------------------------------------------------------*/
		public TargetMashTempPane(
			PhUnit currentPh,
			WaterAddition mashWater,
			List<FermentableAddition> grainBill,
			TemperatureUnit grainTemp)
		{
			targetTemp = new QuantityEditWidget<>(Quantity.Unit.CELSIUS);
			waterTemp = new QuantityEditWidget<>(Quantity.Unit.CELSIUS);
			waterTemp.setDisable(true);

			this.add(new Label(StringUtils.getUiString("tools.mash.temp.target")));
			this.add(targetTemp, "wrap");

			this.add(new Label(StringUtils.getUiString("tools.mash.temp.water.temp")));
			this.add(waterTemp, "wrap");

			// -----

			targetTemp.addListener((observableValue, oldValue, newValue) ->
			{
				recalc(mashWater, grainBill, grainTemp);
			});
		}

		/*-------------------------------------------------------------------------*/
		private void recalc(
			WaterAddition mashWater,
			List<FermentableAddition> grainBill,
			TemperatureUnit grainTemp)
		{
			if (targetTemp.getQuantity() != null)
			{
				TemperatureUnit temp = Equations.calcWaterTemp(
					Equations.calcTotalGrainWeight(grainBill),
					mashWater,
					grainTemp,
					targetTemp.getQuantity());

				waterTemp.refresh(temp);
			}
		}

		/*-------------------------------------------------------------------------*/
		public TemperatureUnit getTemp()
		{
			return waterTemp.getQuantity();
		}
	}
}
