package mclachlan.brewday.ui.swing.screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import mclachlan.brewday.ui.swing.app.NavTooltipSupport;
import mclachlan.brewday.ui.swing.app.ScreenKey;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingScreen;

/**
 * Hub screen for parent navigation nodes: large icon+text buttons jump to child screens.
 */
public class NavLandingScreen extends JPanel implements SwingScreen
{
	/** Tiles per row; laid out left-to-right, rows left-aligned. */
	private static final int TILE_COLUMNS = 4;

	/**
	 * Square edge length (px) for every landing tile on every hub screen.
	 * Tune if localized strings need more space than the English bundle.
	 */
	public static final int UNIFORM_TILE_SIDE_PX = 180;

	public record Destination(ScreenKey key, String label)
	{
	}

	private final Consumer<ScreenKey> navigate;

	public NavLandingScreen(Consumer<ScreenKey> navigate, String sectionTitle, Destination... destinations)
	{
		super(new BorderLayout());
		this.navigate = navigate;
		setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

		JLabel heading = new JLabel(sectionTitle, SwingConstants.LEADING);
		Font base = heading.getFont();
		if (base != null)
		{
			heading.setFont(base.deriveFont(Font.BOLD, base.getSize2D() + 4f));
		}
		heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
		add(heading, BorderLayout.NORTH);

		List<JButton> tiles = new ArrayList<>(destinations.length);
		for (Destination d : destinations)
		{
			tiles.add(makeTileButton(d));
		}

		Dimension square = new Dimension(UNIFORM_TILE_SIDE_PX, UNIFORM_TILE_SIDE_PX);
		for (JButton b : tiles)
		{
			b.setPreferredSize(square);
			b.setMinimumSize(square);
			b.setMaximumSize(square);
		}

		JPanel grid = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(6, 6, 6, 6);
		int col = 0;
		int rowIdx = 0;
		for (JButton b : tiles)
		{
			gbc.gridx = col;
			gbc.gridy = rowIdx;
			grid.add(b, gbc);
			col++;
			if (col >= TILE_COLUMNS)
			{
				col = 0;
				rowIdx++;
			}
		}

		JPanel leftAlign = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		leftAlign.add(grid);

		add(new JScrollPane(leftAlign), BorderLayout.CENTER);
	}

	private JButton makeTileButton(Destination d)
	{
		JButton b = new JButton(d.label(),
			SwingIcons.icon(SwingIcons.navKey(d.key()), SwingIcons.LANDING_NAV_ICON_SIZE));
		b.setName("nav.landing." + d.key().name());
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);
		b.setIconTextGap(8);
		b.setMargin(new Insets(10, 10, 10, 10));
		b.setHorizontalAlignment(SwingConstants.CENTER);
		b.setFocusPainted(true);
		b.setToolTipText(NavTooltipSupport.tooltipFor(d.key()));
		b.addActionListener(e -> navigate.accept(d.key()));
		var ac = b.getAccessibleContext();
		ac.setAccessibleName(d.label());
		ac.setAccessibleDescription(d.label());
		return b;
	}
}
