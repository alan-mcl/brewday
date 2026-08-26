/*
 * This file is part of Brewday.
 *
 * Brewday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brewday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Brewday.  If not, see https://www.gnu.org/licenses.
 */

package mclachlan.brewday.ui.swing.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import mclachlan.brewday.db.Database;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.recommend.BrewRecommendationEngine;
import mclachlan.brewday.recommend.Recommendation;
import mclachlan.brewday.recommend.RecommendationContext;
import mclachlan.brewday.recommend.RecommendationGroup;
import mclachlan.brewday.recommend.RecommendationResult;
import mclachlan.brewday.recommend.RecommendationTag;
import mclachlan.brewday.recommend.RecommendationUiSupport;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Tools &gt; What Should I Brew? recommendation panel.
 */
public class SwingWhatShouldIBrewPanel extends JPanel
{
	private static final int NAME_ICON_GAP_PX = 6;

	private final Consumer<String> openRecipeHandler;
	private final Consumer<String> newBatchHandler;

	private final JPanel contentPanel;
	private final JLabel emptyLabel;
	private final BrewRecommendationEngine engine = new BrewRecommendationEngine();
	private final Map<String, List<Icon>> beerIconsByRecipeName = new HashMap<>();

	public SwingWhatShouldIBrewPanel(
		Consumer<String> openRecipeHandler,
		Consumer<String> newBatchHandler)
	{
		super(new BorderLayout(0, 8));
		this.openRecipeHandler = openRecipeHandler;
		this.newBatchHandler = newBatchHandler;

		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		JLabel intro = new JLabel(getUiString("tools.what.should.i.brew.intro"));
		intro.setFont(intro.getFont().deriveFont(Font.ITALIC));
		header.add(intro);
		JButton refresh = new JButton(getUiString("tools.what.should.i.brew.refresh"));
		refresh.addActionListener(e -> refreshRecommendations());
		header.add(refresh);
		add(header, BorderLayout.NORTH);

		contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		emptyLabel = new JLabel(getUiString("tools.what.should.i.brew.empty"));
		emptyLabel.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
		emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		contentPanel.add(emptyLabel);

		add(contentPanel, BorderLayout.CENTER);
	}

	public void refreshRecommendations()
	{
		RecommendationContext context = RecommendationContext.fromDatabase(LocalDate.now());
		RecommendationResult result = engine.recommend(context);
		context.persistRecentShown(result.getShownRecipeNames());
		displayResult(result);
	}

	private void displayResult(RecommendationResult result)
	{
		beerIconsByRecipeName.clear();
		for (RecommendationGroup group : result.getGroups())
		{
			for (Recommendation rec : group.getRecommendations())
			{
				beerIconsByRecipeName.computeIfAbsent(rec.getRecipeName(), this::loadBeerIcons);
			}
		}

		contentPanel.removeAll();
		if (result.isEmpty())
		{
			contentPanel.add(emptyLabel);
		}
		else
		{
			for (RecommendationGroup group : result.getGroups())
			{
				contentPanel.add(buildGroupPanel(group));
				contentPanel.add(Box.createVerticalStrut(8));
			}
		}
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private List<Icon> loadBeerIcons(String recipeName)
	{
		Recipe recipe = Database.getInstance().getRecipes().get(recipeName);
		return RecipeTableBeerIcons.iconsForRecipe(recipe, 3);
	}

	private JPanel buildGroupPanel(RecommendationGroup group)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			RecommendationUiSupport.groupTitle(group.getKind()),
			TitledBorder.LEADING,
			TitledBorder.TOP));

		for (Recommendation rec : group.getRecommendations())
		{
			panel.add(buildRecommendationRow(rec));
		}
		return panel;
	}

	private JPanel buildRecommendationRow(Recommendation rec)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, row.getBackground().darker()),
			BorderFactory.createEmptyBorder(2, 8, 2, 8)));

		JPanel textColumn = new JPanel();
		textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
		textColumn.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel titleBar = new JPanel(new BorderLayout(8, 0));
		titleBar.setOpaque(false);
		titleBar.setAlignmentX(Component.LEFT_ALIGNMENT);
		titleBar.add(buildNameWithIcons(rec.getRecipeName()), BorderLayout.WEST);
		titleBar.add(buildActions(rec), BorderLayout.EAST);

		textColumn.add(titleBar);
		textColumn.add(leftLabel(metaLine(rec)));
		textColumn.add(leftLabel("<html>" + escapeHtml(rec.getExplanation()) + "</html>"));

		String extras = buildExtrasLine(rec);
		if (!extras.isBlank())
		{
			textColumn.add(leftLabel(extras));
		}

		row.add(textColumn, BorderLayout.CENTER);
		return row;
	}

	private JPanel buildNameWithIcons(String recipeName)
	{
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, NAME_ICON_GAP_PX, 0));
		panel.setOpaque(false);

		List<Icon> icons = beerIconsByRecipeName.getOrDefault(recipeName, List.of());
		if (!icons.isEmpty())
		{
			JPanel iconStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			iconStrip.setOpaque(false);
			for (Icon icon : icons)
			{
				if (icon != null)
				{
					iconStrip.add(new JLabel(icon));
				}
			}
			if (iconStrip.getComponentCount() > 0)
			{
				panel.add(iconStrip);
			}
		}

		JLabel title = new JLabel(recipeName);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		panel.add(title);
		return panel;
	}

	private JPanel buildActions(Recommendation rec)
	{
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		JButton openRecipe = new JButton(getUiString("tools.what.should.i.brew.open.recipe"));
		openRecipe.addActionListener(e -> openRecipeHandler.accept(rec.getRecipeName()));
		actions.add(openRecipe);
		JButton newBatch = new JButton(getUiString("tools.what.should.i.brew.new.batch"));
		newBatch.addActionListener(e -> newBatchHandler.accept(rec.getRecipeName()));
		actions.add(newBatch);
		return actions;
	}

	private static JLabel leftLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static String metaLine(Recommendation rec)
	{
		return getUiString("tools.what.should.i.brew.style") + ": " + blank(rec.getStyleDisplay())
			+ "    "
			+ getUiString("tools.what.should.i.brew.match") + ": " + rec.getInventoryMatchPercent() + "%";
	}

	private static String buildExtrasLine(Recommendation rec)
	{
		StringBuilder sb = new StringBuilder();
		if (!rec.getTags().isEmpty())
		{
			sb.append(formatTags(rec.getTags()));
		}
		for (String detail : rec.getDetailLines())
		{
			if (sb.length() > 0)
			{
				sb.append("  •  ");
			}
			sb.append(detail);
		}
		return sb.toString();
	}

	private static String blank(String value)
	{
		return value == null || value.isBlank() ? "—" : value;
	}

	private static String formatTags(java.util.List<RecommendationTag> tags)
	{
		StringBuilder sb = new StringBuilder();
		for (RecommendationTag tag : tags)
		{
			if (sb.length() > 0)
			{
				sb.append("  •  ");
			}
			sb.append('[').append(RecommendationUiSupport.tagLabel(tag)).append(']');
		}
		return sb.toString();
	}

	private static String escapeHtml(String text)
	{
		if (text == null)
		{
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
