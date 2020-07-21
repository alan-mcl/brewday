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

import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import mclachlan.brewday.BrewdayException;
import mclachlan.brewday.Settings;
import mclachlan.brewday.StringUtils;
import mclachlan.brewday.db.Database;
import org.tbee.javafx.scene.layout.MigPane;

/**
 *
 */
public class UiSettingsPane extends MigPane
{
	private boolean detectDirty = true;

	private final RadioButton jMetroLight, jMetroDark;

	public UiSettingsPane()
	{
		this.add(new Label(StringUtils.getUiString("settings.ui.theme")), "wrap");

		jMetroLight = new RadioButton(StringUtils.getUiString("setting.ui.theme.jmetrolight"));
		jMetroDark = new RadioButton(StringUtils.getUiString("setting.ui.theme.jmetrodark"));
//		caspian = new RadioButton(StringUtils.getUiString("setting.ui.theme.caspian"));
//		modena = new RadioButton(StringUtils.getUiString("setting.ui.theme.modena"));

		ToggleGroup group = new ToggleGroup();

		jMetroLight.setToggleGroup(group);
		jMetroDark.setToggleGroup(group);
//		modena.setToggleGroup(group);
//		caspian.setToggleGroup(group);

		this.add(jMetroLight);
		this.add(jMetroDark, "wrap");
//		this.add(caspian);
//		this.add(modena, "wrap");

		// ----

		jMetroDark.setOnAction(event -> setThemeAndRestyleUi(Settings.JMETRO_DARK));
		jMetroLight.setOnAction(event -> setThemeAndRestyleUi(Settings.JMETRO_LIGHT));
//		caspian.setOnAction(event -> setThemeAndRestyleUi(Settings.CASPIAN));
//		modena.setOnAction(event -> setThemeAndRestyleUi(Settings.MODENA));
	}

	private void setThemeAndRestyleUi(String theme)
	{
		Database.getInstance().getSettings().set(Settings.UI_THEME, theme);
//		JfxUi.getInstance().restyle();
	}

	public void refresh(Database db)
	{
		detectDirty = false;

		Settings settings = db.getSettings();

		String theme = settings.get(Settings.UI_THEME);

		switch (theme)
		{
			case Settings.JMETRO_LIGHT:
				jMetroLight.setSelected(true);
				break;
			case Settings.JMETRO_DARK:
				jMetroLight.setSelected(true);
				break;
//			case Settings.CASPIAN:
//				caspian.setSelected(true);
//				break;
//			case Settings.MODENA:
//				modena.setSelected(true);
//				break;
			default:
				throw new BrewdayException("Invalid UI theme "+theme);
		}

		detectDirty = true;
	}
}
