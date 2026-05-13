package mclachlan.brewday.ui.swing.app;

import java.awt.Window;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import mclachlan.brewday.Brewday;
import mclachlan.brewday.Settings;

/**
 * Applies persisted {@link Settings#SWING_LOOK_AND_FEEL} (JSON {@code swing.laf}) to the Swing UI.
 * Independent of JavaFX {@link Settings#UI_THEME}.
 */
public final class SwingThemeSupport
{

	private SwingThemeSupport()
	{
	}

	public static String normalizeSwingLafKey(String stored)
	{
		if (stored == null || stored.isBlank())
		{
			return Settings.SWING_LAF_FLAT_LIGHT;
		}
		switch (stored)
		{
			case Settings.SWING_LAF_FLAT_LIGHT:
			case Settings.SWING_LAF_FLAT_DARK:
			case Settings.SWING_LAF_FLAT_DARCULA:
			case Settings.SWING_LAF_FLAT_INTELLIJ:
			case Settings.SWING_LAF_NIMBUS:
			case Settings.SWING_LAF_METAL:
			case Settings.SWING_LAF_SYSTEM:
				return stored;
			default:
				Brewday.getInstance().getLog().log("Unknown swing.laf \"" + stored + "\"; using "
					+ Settings.SWING_LAF_FLAT_LIGHT);
				return Settings.SWING_LAF_FLAT_LIGHT;
		}
	}

	public static void applySwingLafFromSettings(Settings settings)
	{
		String key = normalizeSwingLafKey(settings.get(Settings.SWING_LOOK_AND_FEEL));
		applySwingLaf(key);
	}

	public static void applySwingLaf(String normalizedSwingLafKey)
	{
		try
		{
			if (Settings.SWING_LAF_SYSTEM.equals(normalizedSwingLafKey))
			{
				String className = UIManager.getSystemLookAndFeelClassName();
				if (className != null && !className.isBlank())
				{
					UIManager.setLookAndFeel(className);
					return;
				}
				UIManager.setLookAndFeel(new FlatLightLaf());
				return;
			}
			UIManager.setLookAndFeel(lookAndFeelForToken(normalizedSwingLafKey));
		}
		catch (Exception e)
		{
			Brewday.getInstance().getLog().log(e);
			e.printStackTrace(System.out);
			fallbackFlatLight();
		}
	}

	/**
	 * Sets the LAF and refreshes existing top-level Swing windows/dialogs so change is visible without restart.
	 * Must run on the EDT.
	 */
	public static void applySwingLafLive(String normalizedSwingLafKey)
	{
		applySwingLaf(normalizedSwingLafKey);
		refreshUiOnAllSwingWindows();
	}

	/*-------------------------------------------------------------------------*/

	public static void refreshUiOnAllSwingWindows()
	{
		for (Window w : Window.getWindows())
		{
			if (w.isDisplayable())
			{
				SwingUtilities.updateComponentTreeUI(w);
			}
		}
	}

	private static LookAndFeel lookAndFeelForToken(String normalizedKey)
	{
		switch (normalizedKey)
		{
			case Settings.SWING_LAF_FLAT_LIGHT:
				return new FlatLightLaf();
			case Settings.SWING_LAF_FLAT_DARK:
				return new FlatDarkLaf();
			case Settings.SWING_LAF_FLAT_DARCULA:
				return new FlatDarculaLaf();
			case Settings.SWING_LAF_FLAT_INTELLIJ:
				return new FlatIntelliJLaf();
			case Settings.SWING_LAF_NIMBUS:
				return new NimbusLookAndFeel();
			case Settings.SWING_LAF_METAL:
				return new MetalLookAndFeel();
			default:
				return new FlatLightLaf();
		}
	}

	private static void fallbackFlatLight()
	{
		try
		{
			UIManager.setLookAndFeel(new FlatLightLaf());
		}
		catch (Exception e2)
		{
			Brewday.getInstance().getLog().log(e2);
			e2.printStackTrace(System.out);
		}
	}
}
