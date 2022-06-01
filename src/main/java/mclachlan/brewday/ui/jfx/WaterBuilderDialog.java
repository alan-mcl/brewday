
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.util.StringUtils;
import mclachlan.brewday.math.TimeUnit;
import mclachlan.brewday.process.BatchSparge;
import mclachlan.brewday.process.Mash;
import mclachlan.brewday.process.MashInfusion;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.recipe.MiscAddition;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class WaterBuilderDialog extends Dialog<Boolean>
{
	private final WaterBuilderPane wbp;
	private ProcessStep step;
	private boolean output;

	public WaterBuilderDialog(ProcessStep step)
	{
		Scene scene = this.getDialogPane().getScene();
		JfxUi.styleScene(scene);
		Stage stage = (Stage)scene.getWindow();
		stage.getIcons().add(Icons.waterBuilderIcon);

		ButtonType okButtonType = new ButtonType(
			StringUtils.getUiString("ui.ok"), ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType(
			StringUtils.getUiString("ui.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
		this.getDialogPane().getButtonTypes().add(okButtonType);
		this.getDialogPane().getButtonTypes().add(cancelButtonType);

		this.setTitle(StringUtils.getUiString("tools.water.builder"));
//		this.setGraphic(JfxUi.getImageView(JfxUi.waterBuilderIcon, 32));

		MigPane content = new MigPane();

		wbp = new WaterBuilderPane(step);

		wbp.init(step.getWaterAdditions());

		content.add(wbp);

		this.getDialogPane().setContent(content);

		this.step = step;
		output = false;

		// -----

		final Button btOk = (Button)this.getDialogPane().lookupButton(okButtonType);
		btOk.addEventFilter(ActionEvent.ACTION, event ->
		{
			output = true;
		});
	}

	/*-------------------------------------------------------------------------*/

	public List<MiscAddition> getWaterAdditions()
	{
		List<MiscAddition> additions = wbp.getAdditions();

		for (MiscAddition ma : additions)
		{
			TimeUnit time;
			if (step instanceof Mash)
			{
				time = new TimeUnit(((Mash)step).getDuration());
			}
			else if (step instanceof BatchSparge)
			{
				time = new TimeUnit(0);
			}
			else if (step instanceof MashInfusion)
			{
				time = new TimeUnit(0);
			}
			else
			{
				throw new BrewdayException("invalid step type: "+step);
			}

			ma.setTime(time);
		}

		return additions;
	}

	/*-------------------------------------------------------------------------*/

	public boolean getOutput()
	{
		return output;
	}
}
