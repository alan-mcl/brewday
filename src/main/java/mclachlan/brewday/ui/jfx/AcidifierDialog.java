
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
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.ingredients.Misc;
import mclachlan.brewday.math.*;
import mclachlan.brewday.recipe.FermentableAddition;
import mclachlan.brewday.recipe.MiscAddition;
import mclachlan.brewday.recipe.WaterAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class AcidifierDialog extends Dialog<Boolean>
{
	private final AcidifierPane acidifierPane;
	private boolean output;

	public AcidifierDialog(PhUnit currentPh,
		WaterAddition mashWater,
		List<FermentableAddition> grainBill)
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(Icons.acidifierIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("tools.acidifier"));
		this.setGraphic(JfxUi.getImageView(Icons.acidifierIcon, 32));

		MigPane content = new MigPane();

		acidifierPane = new AcidifierPane(currentPh, mashWater, grainBill);

		content.add(acidifierPane);

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

	public List<MiscAddition> getAcidAdditions()
	{
		return acidifierPane.getAdditions();
	}

	/*-------------------------------------------------------------------------*/

	public boolean getOutput()
	{
		return output;
	}

	/*-------------------------------------------------------------------------*/
	private static class AcidifierPane extends MigPane
	{
		private QuantityEditWidget<PhUnit> currentMashPh, targetMashPh;
		private ComboBox<String> acid;
		private QuantityEditWidget<PercentageUnit> acidConcentration;
		private QuantityEditWidget<VolumeUnit> acidVolume;

		/*-------------------------------------------------------------------------*/
		public AcidifierPane(
			PhUnit currentPh,
			WaterAddition mashWater,
			List<FermentableAddition> grainBill)
		{
			currentMashPh = new QuantityEditWidget<>(Quantity.Unit.PH);
			targetMashPh = new QuantityEditWidget<>(Quantity.Unit.PH);

			currentMashPh.setDisable(true);
			currentMashPh.refresh(currentPh);

			this.add(new Label(StringUtils.getUiString("tools.acidifier.current.ph")));
			this.add(currentMashPh, "wrap");

			this.add(new Label(StringUtils.getUiString("tools.acidifier.target.ph")));
			this.add(targetMashPh, "wrap");

			acid = new ComboBox<>();
			List<String> acids = new ArrayList<>();
			for (Misc m : Database.getInstance().getMiscs().values())
			{
				if (m.getWaterAdditionFormula() == Misc.WaterAdditionFormula.LACTIC_ACID &&
					m.getAcidContent() != null && m.getAcidContent().get(Quantity.Unit.PERCENTAGE) > 0)
				{
					acids.add(m.getName());
				}
			}
			acids.sort(String::compareTo);
			acid.setItems(FXCollections.observableList(acids));

			this.add(new Label(StringUtils.getUiString("tools.acidifier.acid")));
			this.add(acid, "wrap");

			acidConcentration = new QuantityEditWidget<>(Quantity.Unit.PERCENTAGE_DISPLAY);
			acidConcentration.setDisable(true);

			this.add(new Label(StringUtils.getUiString("tools.acidifier.acid.concentration")));
			this.add(acidConcentration, "wrap");

			acidVolume = new QuantityEditWidget<>(Quantity.Unit.MILLILITRES);
			acidVolume.setDisable(true);

			this.add(new Label(StringUtils.getUiString("tools.acidifier.acid.volume")));
			this.add(acidVolume, "wrap");

			// -----

			targetMashPh.addListener((observableValue, oldValue, newValue) ->
			{
				recalc(mashWater, grainBill);
			});

			acid.getSelectionModel().selectedItemProperty().addListener((observableValue, oldV, newV) ->
			{
				if (newV != null && !newV.equals(oldV))
				{
					Misc misc = Database.getInstance().getMiscs().get(newV);

					acidConcentration.refresh(misc.getAcidContent());

					recalc(mashWater, grainBill);
				}
			});

			// --
			if (acids.size() > 0)
			{
				acid.getSelectionModel().select(0);
			}
		}

		/*-------------------------------------------------------------------------*/
		private void recalc(
			WaterAddition mashWater,
			List<FermentableAddition> grainBill)
		{
			Misc misc = Database.getInstance().getMiscs().get(acid.getSelectionModel().getSelectedItem());

			if (targetMashPh.getQuantity() != null && misc != null)
			{
				Settings.MashPhModel model = Settings.MashPhModel.valueOf(Database.getInstance().getSettings().get(Settings.MASH_PH_MODEL));
				switch (model)
				{
					case EZ_WATER:
						VolumeUnit vol = Equations.calcAcidAdditionEzWater(
							misc,
							targetMashPh.getQuantity(),
							mashWater,
							grainBill);
						acidVolume.refresh(vol);
						break;
					default:
						throw new IllegalStateException("Unexpected value: " + model);
				}
			}
		}

		/*-------------------------------------------------------------------------*/
		public List<MiscAddition> getAdditions()
		{
			Misc misc = Database.getInstance().getMiscs().get(acid.getSelectionModel().getSelectedItem());
			VolumeUnit vol = acidVolume.getQuantity();

			if (misc != null && vol != null && vol.get() > 0)
			{
				MiscAddition ma = new MiscAddition(misc, vol, acidVolume.getUnit(), new TimeUnit(0));

				return Collections.singletonList(ma);
			}
			else
			{
				return new ArrayList<>();
			}
		}
	}
}
