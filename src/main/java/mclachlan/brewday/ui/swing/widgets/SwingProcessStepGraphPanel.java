package mclachlan.brewday.ui.swing.widgets;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import mclachlan.brewday.Brewday;
import mclachlan.brewday.process.ProcessLog;
import mclachlan.brewday.process.ProcessStep;
import mclachlan.brewday.process.Volume;
import mclachlan.brewday.recipe.Recipe;
import mclachlan.brewday.ui.swing.app.SwingIcons;
import mclachlan.brewday.ui.swing.app.SwingUiErrors;

import org.jgrapht.graph.DirectedAcyclicGraph;

import static mclachlan.brewday.util.StringUtils.getUiString;

/**
 * Constrained-force DAG renderer.
 * <p>
 * Evolution of the original layered renderer:
 * <p>
 * - preserves top-to-bottom flow - preserves median ordering - preserves
 * world/camera rendering - preserves obstacle-aware edge routing
 * <p>
 * But replaces: - rigid grid layout with: - constrained force-relaxed layout
 */
public class SwingProcessStepGraphPanel extends JPanel
{
	private static final double WORLD_MARGIN = 60.0;
	private static final double ANNOTATION_GUTTER = 420.0;

	private static final double INITIAL_LAYER_GAP = 180.0;
	private static final double INITIAL_COL_GAP = 220.0;

	private static final double NODE_H = 58.0;
	private static final double NODE_MIN_W = 170.0;
	private static final double NODE_MAX_W = 340.0;

	private static final double REPULSION = 180000.0;
	private static final double SPRING = 0.010;
	private static final double DAMPING = 0.84;

	private static final double TARGET_EDGE_LEN = 180.0;

	private static final double LAYER_GRAVITY = 0.05;
	private static final double FLOW_GRAVITY = 0.22;

	private static final double MIN_VERTICAL_GAP = 90.0;

	private static final int ITERATIONS = 240;

	private static final double MAX_VELOCITY = 16.0;

	private static final double EDGE_HIT_PX = 24.0;

	private static final double ARROW_LEN = 9;
	private static final double ARROW_WING = 4;

	private static final double OBSTACLE_INFLATE = 3.0;

	private static final int ICON_PX = SwingIcons.TOOLBAR_ICON_SIZE;

	private static final int MEDIAN_PASSES = 8;

	private static final double ZOOM_BUTTON_FACTOR = 1.15;
	private static final int EXPORT_IMAGE_SCALE = 2;

	private Recipe recipe;
	private DirectedAcyclicGraph<ProcessStep, String> graph;

	private boolean graphOk;
	private String bannerMessage;

	private final List<LayoutNode> layoutNodes = new ArrayList<>();
	private final List<LayoutEdge> layoutEdges = new ArrayList<>();
	private final List<EdgeDraw> edgeDraws = new ArrayList<>();

	private GraphCamera camera = new GraphCamera();

	private Rectangle2D.Double worldBounds;

	private int lastDragX;
	private int lastDragY;
	private boolean dragging;

	private String layoutTopologySignature;
	private boolean layoutStale;
	private final Map<ProcessStep, String> nodeTooltipCache = new HashMap<>();

	private static final int MIN_VIEWPORT_FIT = 64;

	private boolean pendingViewportFit;

	public SwingProcessStepGraphPanel()
	{
		setBackground(Color.WHITE);
		setOpaque(true);
		setDoubleBuffered(true);

		setMinimumSize(new Dimension(120, 80));

		ToolTipManager.sharedInstance().registerComponent(this);

		installInteractionHandlers();
	}

	/*----------------------------------------------------------------------*/

	public void refresh(Recipe r)
	{
		relayout(r);
	}

	/*----------------------------------------------------------------------*/

	public void updateAfterRun(Recipe r)
	{
		this.recipe = r;
		nodeTooltipCache.clear();

		if (r == null || r.getSteps().isEmpty())
		{
			clearLayoutState();
			bannerMessage = getUiString("recipe.process.graph.empty");
			repaint();
			return;
		}

		DirectedAcyclicGraph<ProcessStep, String> g =
			new DirectedAcyclicGraph<>(String.class);

		ProcessLog log = new ProcessLog();

		if (!r.buildProcessStepDag(g, log))
		{
			clearLayoutState();
			bannerMessage = getUiString("recipe.process.graph.cycle");
			repaint();
			return;
		}

		bannerMessage = null;
		this.graph = g;

		String sig = computeTopologySignature(r, g);
		if (hasLayout() && layoutTopologySignature != null && !layoutTopologySignature.equals(sig))
		{
			layoutStale = true;
		}

		if (hasLayout())
		{
			graphOk = true;
			refreshEdgeLabels();
			repaint();
			return;
		}

		graphOk = false;
		repaint();
	}

	/*----------------------------------------------------------------------*/

	public void relayout(Recipe r)
	{
		this.recipe = r;
		nodeTooltipCache.clear();
		layoutStale = false;

		clearLayoutState();

		if (r == null || r.getSteps().isEmpty())
		{
			bannerMessage = getUiString("recipe.process.graph.empty");
			repaint();
			return;
		}

		DirectedAcyclicGraph<ProcessStep, String> g =
			new DirectedAcyclicGraph<>(String.class);

		ProcessLog log = new ProcessLog();

		if (!r.buildProcessStepDag(g, log))
		{
			bannerMessage = getUiString("recipe.process.graph.cycle");
			repaint();
			return;
		}

		this.graph = g;
		this.graphOk = true;
		layoutTopologySignature = computeTopologySignature(r, g);

		layoutGraph(g);

		repaint();

		scheduleViewportFit();
	}

	/*----------------------------------------------------------------------*/

	public boolean hasLayout()
	{
		return graphOk && !layoutNodes.isEmpty();
	}

	/*----------------------------------------------------------------------*/

	public boolean isLayoutStale()
	{
		return layoutStale;
	}

	/*----------------------------------------------------------------------*/

	public void zoomIn()
	{
		zoomByButtonFactor(ZOOM_BUTTON_FACTOR);
	}

	/*----------------------------------------------------------------------*/

	public void zoomOut()
	{
		zoomByButtonFactor(1.0 / ZOOM_BUTTON_FACTOR);
	}

	/*----------------------------------------------------------------------*/

	public void exportToPng(Component parent)
	{
		if (!graphOk || recipe == null)
		{
			return;
		}

		String baseName = recipe.getName().replaceAll("[^a-zA-Z0-9._-]+", "_");
		if (baseName.isBlank())
		{
			baseName = "process-graph";
		}

		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(baseName + "-process-graph.png"));
		chooser.setFileFilter(new FileNameExtensionFilter("PNG", "png"));

		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File target = chooser.getSelectedFile();
		if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".png"))
		{
			target = new File(target.getParentFile(), target.getName() + ".png");
		}

		try
		{
			BufferedImage image = renderGraphImage();
			ImageIO.write(image, "png", target);
		}
		catch (IOException e)
		{
			SwingUiErrors.showError(parent, e, getUiString("recipe.process.graph.export"));
		}
	}

	/*----------------------------------------------------------------------*/

	private void clearLayoutState()
	{
		this.graph = null;
		this.graphOk = false;
		this.bannerMessage = null;
		layoutTopologySignature = null;
		layoutStale = false;
		pendingViewportFit = false;

		layoutNodes.clear();
		layoutEdges.clear();
		edgeDraws.clear();

		resetCamera();
	}

	/*----------------------------------------------------------------------*/

	private String computeTopologySignature(
		Recipe r,
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(r.getSteps().size());
		for (ProcessStep s : r.getSteps())
		{
			sb.append('|').append(s.getName()).append(':').append(s.getType());
		}
		for (String edge : g.edgeSet())
		{
			sb.append('|').append(edge);
		}
		return sb.toString();
	}

	/*----------------------------------------------------------------------*/

	private void refreshEdgeLabels()
	{
		if (!hasLayout() || recipe == null)
		{
			return;
		}

		Font labelFont = getFont().deriveFont(Font.PLAIN, 11f);
		FontMetrics fm = getFontMetrics(labelFont);

		for (LayoutEdge edge : layoutEdges)
		{
			if (edge.draw == null)
			{
				continue;
			}

			String display = edgeLabelDisplay(edge.volumeId);
			edge.draw.displayLabel = display;
			edge.draw.labelLines = wrapRoughSquare(display, fm);
		}
	}

	/*----------------------------------------------------------------------*/

	private void zoomByButtonFactor(double factor)
	{
		if (!graphOk || worldBounds == null)
		{
			return;
		}

		camera.zoomTowardPoint(
			getWidth() / 2.0,
			getHeight() / 2.0,
			factor);

		repaint();
	}

	/*----------------------------------------------------------------------*/

	public BufferedImage renderGraphImage()
	{
		int w = Math.max(1, getWidth());
		int h = Math.max(1, getHeight());
		int scale = EXPORT_IMAGE_SCALE;

		BufferedImage image =
			new BufferedImage(
				w * scale,
				h * scale,
				BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2 = image.createGraphics();
		g2.scale(scale, scale);
		paintGraph(g2);
		g2.dispose();

		return image;
	}

	/*----------------------------------------------------------------------*/

	private void layoutGraph(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		layoutNodes.clear();
		layoutEdges.clear();
		edgeDraws.clear();

		buildLayoutModel(g);

		assignLayers(g);

		initialPlacement(g);

		relaxLayout();

		updateNodeBounds();

		routeEdges();

		buildEdgeDraws();

		computeWorldBounds();
	}

	/*----------------------------------------------------------------------*/

	private void buildLayoutModel(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		Font font = getFont().deriveFont(Font.PLAIN, 12f);
		FontMetrics fm = getFontMetrics(font);

		Map<ProcessStep, LayoutNode> byStep = new HashMap<>();

		for (ProcessStep step : g.vertexSet())
		{
			LayoutNode node = new LayoutNode(step);

			int titleW = fm.stringWidth(step.getName());
			int subW = fm.stringWidth(step.getType().name());

			node.width = clamp(
				Math.max(titleW, subW) + 70,
				NODE_MIN_W,
				NODE_MAX_W);

			node.height = NODE_H;

			layoutNodes.add(node);

			byStep.put(step, node);
		}

		for (String edge : g.edgeSet())
		{
			layoutEdges.add(new LayoutEdge(
				byStep.get(g.getEdgeSource(edge)),
				byStep.get(g.getEdgeTarget(edge)),
				edge));
		}
	}

	/*----------------------------------------------------------------------*/

	private void assignLayers(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		Map<ProcessStep, Integer> layerOf =
			longestPathLayers(g);

		for (LayoutNode node : layoutNodes)
		{
			node.layer = layerOf.get(node.step);
		}
	}

	/*----------------------------------------------------------------------*/

	private void initialPlacement(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		Map<Integer, List<LayoutNode>> byLayer =
			layoutNodes.stream().collect(
				Collectors.groupingBy(n -> n.layer));

		List<List<ProcessStep>> orderedLayers =
			buildOrderedLayers(g);

		Map<ProcessStep, Integer> orderInLayer =
			new HashMap<>();

		for (List<ProcessStep> layer : orderedLayers)
		{
			for (int i = 0; i < layer.size(); i++)
			{
				orderInLayer.put(layer.get(i), i);
			}
		}

		for (Map.Entry<Integer, List<LayoutNode>> e : byLayer.entrySet())
		{
			int layer = e.getKey();

			List<LayoutNode> row = e.getValue();

			row.sort(Comparator.comparingInt(
				n -> orderInLayer.getOrDefault(n.step, 0)));

			double totalWidth =
				row.stream().mapToDouble(n -> n.width).sum() +
					(row.size() - 1) * INITIAL_COL_GAP;

			double x = -totalWidth / 2.0;

			for (LayoutNode node : row)
			{
				node.x = x;
				node.y = layer * INITIAL_LAYER_GAP;

				x += node.width + INITIAL_COL_GAP;
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private void relaxLayout()
	{
		Dimension viewport = getSize();

		for (int iter = 0; iter < ITERATIONS; iter++)
		{
			applyRepulsion();

			applySprings();

			applyLayerGravity();

			applyVerticalFlowConstraints();

			applyCollisionResolution();

			applyViewportCompaction(viewport);

			integrate();
		}
	}

	/*----------------------------------------------------------------------*/

	private void applyRepulsion()
	{
		for (int i = 0; i < layoutNodes.size(); i++)
		{
			LayoutNode a = layoutNodes.get(i);

			for (int j = i + 1; j < layoutNodes.size(); j++)
			{
				LayoutNode b = layoutNodes.get(j);

				double dx = b.x - a.x;
				double dy = b.y - a.y;

				double dist2 = dx * dx + dy * dy + 0.01;

				double force = REPULSION / dist2;

				double dist = Math.sqrt(dist2);

				double fx = force * dx / dist;
				double fy = force * dy / dist;

				a.vx -= fx;
				a.vy -= fy;

				b.vx += fx;
				b.vy += fy;
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private void applySprings()
	{
		for (LayoutEdge edge : layoutEdges)
		{
			LayoutNode a = edge.src;
			LayoutNode b = edge.dst;

			double dx = b.x - a.x;
			double dy = b.y - a.y;

			double dist = Math.max(0.01, Math.hypot(dx, dy));

			double force =
				SPRING * (dist - TARGET_EDGE_LEN);

			double fx = force * dx / dist;
			double fy = force * dy / dist;

			a.vx += fx;
			a.vy += fy;

			b.vx -= fx;
			b.vy -= fy;
		}
	}

	/*----------------------------------------------------------------------*/

	private void applyLayerGravity()
	{
		for (LayoutNode node : layoutNodes)
		{
			double targetY =
				node.layer * INITIAL_LAYER_GAP;

			node.vy +=
				(targetY - node.y) * LAYER_GRAVITY;
		}
	}

	/*----------------------------------------------------------------------*/

	private void applyVerticalFlowConstraints()
	{
		for (LayoutEdge edge : layoutEdges)
		{
			LayoutNode parent = edge.src;
			LayoutNode child = edge.dst;

			double actual = child.y - parent.y;

			if (actual < MIN_VERTICAL_GAP)
			{
				double correction =
					(MIN_VERTICAL_GAP - actual) *
						FLOW_GRAVITY;

				parent.vy -= correction;
				child.vy += correction;
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private void applyCollisionResolution()
	{
		for (int i = 0; i < layoutNodes.size(); i++)
		{
			LayoutNode a = layoutNodes.get(i);

			Rectangle2D.Double ra = rect(a);

			for (int j = i + 1; j < layoutNodes.size(); j++)
			{
				LayoutNode b = layoutNodes.get(j);

				Rectangle2D.Double rb = rect(b);

				if (!ra.intersects(rb))
				{
					continue;
				}

				double dx = a.x - b.x;
				double dy = a.y - b.y;

				double dist =
					Math.max(1.0, Math.hypot(dx, dy));

				double push = 6.0;

				double px = push * dx / dist;
				double py = push * dy / dist;

				a.vx += px;
				a.vy += py;

				b.vx -= px;
				b.vy -= py;
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private void applyViewportCompaction(
		Dimension viewport)
	{
		Rectangle2D.Double bounds =
			computeBounds();

		double viewportWidth =
			Math.max(100,
				viewport.getWidth() - ANNOTATION_GUTTER);

		double targetWidth =
			viewportWidth * 0.68;

		if (bounds.width > targetWidth)
		{
			for (LayoutNode node : layoutNodes)
			{
				node.vx *= 0.96;
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private void integrate()
	{
		for (LayoutNode node : layoutNodes)
		{
			node.vx *= DAMPING;
			node.vy *= DAMPING;

			node.vx = clamp(
				node.vx,
				-MAX_VELOCITY,
				MAX_VELOCITY);

			node.vy = clamp(
				node.vy,
				-MAX_VELOCITY,
				MAX_VELOCITY);

			node.x += node.vx;
			node.y += node.vy;
		}
	}

	/*----------------------------------------------------------------------*/

	private void updateNodeBounds()
	{
		for (LayoutNode node : layoutNodes)
		{
			node.bounds = rect(node);
		}
	}

	/*----------------------------------------------------------------------*/

	private void routeEdges()
	{
		Map<ProcessStep, List<LayoutEdge>> outgoing =
			new HashMap<>();

		Map<ProcessStep, List<LayoutEdge>> incoming =
			new HashMap<>();

		for (LayoutEdge edge : layoutEdges)
		{
			outgoing
				.computeIfAbsent(
					edge.src.step,
					k -> new ArrayList<>())
				.add(edge);

			incoming
				.computeIfAbsent(
					edge.dst.step,
					k -> new ArrayList<>())
				.add(edge);
		}

		// Stable ordering of ports helps layout stability.

		for (List<LayoutEdge> edges : outgoing.values())
		{
			edges.sort(
				Comparator.comparing(
					e -> e.dst.step.getName()));
		}

		for (List<LayoutEdge> edges : incoming.values())
		{
			edges.sort(
				Comparator.comparing(
					e -> e.src.step.getName()));
		}

		final double PORT_SPREAD = 22.0;

		for (LayoutEdge edge : layoutEdges)
		{
			LayoutNode src = edge.src;
			LayoutNode dst = edge.dst;

			List<LayoutEdge> outs =
				outgoing.get(src.step);

			List<LayoutEdge> ins =
				incoming.get(dst.step);

			int outIndex =
				outs.indexOf(edge);

			int inIndex =
				ins.indexOf(edge);

			double srcOffset =
				(outIndex - (outs.size() - 1) / 2.0)
					* PORT_SPREAD;

			double dstOffset =
				(inIndex - (ins.size() - 1) / 2.0)
					* PORT_SPREAD;

			// Ports.

			double x1 =
				src.x +
					src.width / 2.0 +
					srcOffset;

			double y1 =
				src.y +
					src.height;

			double x2 =
				dst.x +
					dst.width / 2.0 +
					dstOffset;

			double y2 =
				dst.y;

			List<Rectangle2D.Double> obstacles =
				new ArrayList<>();

			for (LayoutNode node : layoutNodes)
			{
				if (node == src || node == dst)
				{
					continue;
				}

				obstacles.add(
					inflateRect(
						node.bounds,
						OBSTACLE_INFLATE));
			}

			edge.polyline =
				routeOrthogonalVertical(
					x1,
					y1,
					x2,
					y2,
					obstacles);
		}
	}

	private static double[] routeOrthogonalVertical(
		double x1,
		double y1,
		double x2,
		double y2,
		List<Rectangle2D.Double> obstacles)
	{
		// Preferred topology:
		//
		// source
		//   |
		//   |
		//   +------+
		//          |
		//          |
		//       target

		double midY =
			(y1 + y2) / 2.0;

		double[] candidate =
			{
				x1, y1,
				x1, midY,
				x2, midY,
				x2, y2
			};

		if (!polylineHitsAnyObstacle(
			candidate,
			obstacles))
		{
			return candidate;
		}

		// Try alternative corridors.

		double[] offsets =
			{
				-120,
				120,
				-220,
				220,
				-320,
				320
			};

		for (double off : offsets)
		{
			double yy = midY + off;

			candidate = new double[]
				{
					x1, y1,
					x1, yy,
					x2, yy,
					x2, y2
				};

			if (!polylineHitsAnyObstacle(
				candidate,
				obstacles))
			{
				return candidate;
			}
		}

		// Try direct vertical then horizontal.

		candidate = new double[]
			{
				x1, y1,
				x1, y2,
				x2, y2
			};

		if (!polylineHitsAnyObstacle(
			candidate,
			obstacles))
		{
			return candidate;
		}

		// Last fallback.

		return new double[]
			{
				x1, y1,
				x2, y2
			};
	}

	/*----------------------------------------------------------------------*/

	private void buildEdgeDraws()
	{
		Font labelFont =
			getFont().deriveFont(Font.PLAIN, 11f);

		FontMetrics fm =
			getFontMetrics(labelFont);

		for (LayoutEdge edge : layoutEdges)
		{
			double[] labelPt =
				pointAtHalfLength(edge.polyline);

			String display =
				edgeLabelDisplay(edge.volumeId);

			List<String> labelLines =
				wrapRoughSquare(display, fm);

			edge.draw =
				new EdgeDraw(
					edge.volumeId,
					display,
					edge.polyline,
					labelLines,
					labelPt[0],
					labelPt[1]);

			edgeDraws.add(edge.draw);
		}
	}

	/*----------------------------------------------------------------------*/

	private void computeWorldBounds()
	{
		worldBounds = computeBounds();
	}

	/*----------------------------------------------------------------------*/

	private Rectangle2D.Double computeBounds()
	{
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;

		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;

		for (LayoutNode node : layoutNodes)
		{
			minX = Math.min(minX, node.x);
			minY = Math.min(minY, node.y);

			maxX = Math.max(
				maxX,
				node.x + node.width);

			maxY = Math.max(
				maxY,
				node.y + node.height);
		}

		if (layoutNodes.isEmpty())
		{
			return new Rectangle2D.Double(
				0,
				0,
				1,
				1);
		}

		return new Rectangle2D.Double(
			minX - WORLD_MARGIN,
			minY - WORLD_MARGIN,
			(maxX - minX) + WORLD_MARGIN * 2,
			(maxY - minY) + WORLD_MARGIN * 2);
	}

	/*----------------------------------------------------------------------*/

	private Rectangle2D.Double rect(LayoutNode node)
	{
		return new Rectangle2D.Double(
			node.x,
			node.y,
			node.width,
			node.height);
	}

	/*----------------------------------------------------------------------*/

	private void resetCamera()
	{
		camera = new GraphCamera();
		worldBounds = null;
	}

	/*----------------------------------------------------------------------*/

	private void installInteractionHandlers()
	{
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				onViewportGeometryChanged();
			}

			@Override
			public void componentShown(ComponentEvent e)
			{
				onViewportGeometryChanged();
			}
		});

		addMouseWheelListener(this::onMouseWheel);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent ev)
			{
				requestFocusInWindow();

				if (ev.getClickCount() == 2 &&
					graphOk &&
					worldBounds != null)
				{
					pendingViewportFit = false;
					fitToViewport();
					repaint();
					return;
				}

				if (ev.getButton() == MouseEvent.BUTTON1 &&
					graphOk)
				{
					dragging = true;

					lastDragX = ev.getX();
					lastDragY = ev.getY();

					setCursor(
						Cursor.getPredefinedCursor(
							Cursor.MOVE_CURSOR));
				}
			}

			@Override
			public void mouseReleased(MouseEvent ev)
			{
				if (ev.getButton() == MouseEvent.BUTTON1)
				{
					dragging = false;

					setCursor(
						Cursor.getDefaultCursor());
				}
			}
		});

		addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseDragged(MouseEvent ev)
			{
				if (!dragging || !graphOk)
				{
					return;
				}

				int x = ev.getX();
				int y = ev.getY();

				camera.panScreen(
					x - lastDragX,
					y - lastDragY);

				lastDragX = x;
				lastDragY = y;

				repaint();
			}
		});
	}

	/*----------------------------------------------------------------------*/

	private void onMouseWheel(MouseWheelEvent e)
	{
		if (!graphOk || worldBounds == null)
		{
			return;
		}

		double factor =
			Math.exp(
				-e.getPreciseWheelRotation() * 0.11);

		camera.zoomTowardPoint(
			e.getX(),
			e.getY(),
			factor);

		e.consume();

		repaint();
	}

	/*----------------------------------------------------------------------*/

	private void scheduleViewportFit()
	{
		pendingViewportFit = true;
		SwingUtilities.invokeLater(() ->
		{
			if (fitToViewport())
			{
				repaint();
			}
		});
	}

	/*----------------------------------------------------------------------*/

	private void onViewportGeometryChanged()
	{
		if (!graphOk || worldBounds == null)
		{
			return;
		}

		if (pendingViewportFit && fitToViewport())
		{
			repaint();
			return;
		}

		repaint();
	}

	/*----------------------------------------------------------------------*/

	/**
	 * @return true when the camera was fitted to the current viewport
	 */
	private boolean fitToViewport()
	{
		if (!graphOk || worldBounds == null)
		{
			return false;
		}

		Dimension ext = viewportExtentSize();
		int vw = ext.width;
		int vh = ext.height;

		if (vw < MIN_VIEWPORT_FIT || vh < MIN_VIEWPORT_FIT)
		{
			return false;
		}

		camera.fitWorld(
			worldBounds,
			vw,
			vh,
			0.90);

		pendingViewportFit = false;
		return true;
	}

	/*----------------------------------------------------------------------*/

	private Dimension viewportExtentSize()
	{
		Container parent = getParent();
		if (parent instanceof JViewport viewport)
		{
			return viewport.getExtentSize();
		}

		return new Dimension(
			getWidth(),
			getHeight());
	}

	/*----------------------------------------------------------------------*/

	private AffineTransform getWorldPaintTransform()
	{
		return new AffineTransform(
			camera.getWorldToComponent());
	}

	/*----------------------------------------------------------------------*/

	private double cameraScaleApprox()
	{
		return Math.max(
			0.05,
			Math.abs(
				camera.getWorldToComponent()
					.getScaleX()));
	}

	/*----------------------------------------------------------------------*/

	@Override
	public String getToolTipText(MouseEvent event)
	{
		if (!graphOk || graph == null || recipe == null)
		{
			return bannerMessage;
		}

		Point2D w =
			camera.componentToWorld(
				event.getX(),
				event.getY());

		double x = w.getX();
		double y = w.getY();

		double hitPx =
			EDGE_HIT_PX / cameraScaleApprox();

		for (LayoutNode node : layoutNodes)
		{
			if (node.bounds.contains(x, y))
			{
				return nodeTooltipCache.computeIfAbsent(
					node.step,
					s -> ProcessStepGraphTooltipBuilder.build(
						s,
						recipe.getVolumes()));
			}
		}

		for (EdgeDraw ed : edgeDraws)
		{
			if (ed.labelBounds != null &&
				ed.labelBounds.contains(x, y))
			{
				return edgeTooltipText(ed);
			}

			if (distanceToPolyline(
				x,
				y,
				ed.polyXy) <= hitPx)
			{
				return edgeTooltipText(ed);
			}
		}

		return null;
	}

	/*----------------------------------------------------------------------*/

	private String edgeTooltipText(EdgeDraw ed)
	{
		StringBuilder sb =
			new StringBuilder(ed.displayLabel);

		if (recipe.getVolumes() != null &&
			recipe.getVolumes().contains(ed.volumeId))
		{
			Volume v =
				recipe.getVolumes()
					.getVolume(ed.volumeId);

			if (v != null)
			{
				sb.append("\n")
					.append(v.describe());
			}
		}

		return sb.toString();
	}

	/*----------------------------------------------------------------------*/

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g.create();
		paintGraph(g2);
		g2.dispose();
	}

	/*----------------------------------------------------------------------*/

	private void paintGraph(Graphics2D g2)
	{
		g2.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (bannerMessage != null)
		{
			drawBanner(g2);
			return;
		}

		if (!graphOk)
		{
			return;
		}

		if (layoutStale)
		{
			drawStaleHint(g2);
		}

		g2.setTransform(
			getWorldPaintTransform());

		double invScale =
			1.0 / cameraScaleApprox();

		drawEdges(g2, invScale);

		drawNodes(g2, invScale);
	}

	/*----------------------------------------------------------------------*/

	private void drawStaleHint(Graphics2D g2)
	{
		String msg = getUiString("recipe.process.graph.stale");
		g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
		FontMetrics fm = g2.getFontMetrics();
		g2.setColor(new Color(120, 80, 0));
		g2.drawString(msg, 8, fm.getAscent() + 6);
	}

	/*----------------------------------------------------------------------*/

	private void drawBanner(Graphics2D g2)
	{
		g2.setColor(Color.DARK_GRAY);

		g2.setFont(
			getFont().deriveFont(
				Font.PLAIN,
				14f));

		FontMetrics fm =
			g2.getFontMetrics();

		int y = getHeight() / 2;

		for (String line :
			bannerMessage.split("\n"))
		{
			int x =
				(getWidth() -
					fm.stringWidth(line)) / 2;

			g2.drawString(line, x, y);

			y += fm.getHeight();
		}
	}

	/*----------------------------------------------------------------------*/

	private void drawEdges(
		Graphics2D g2,
		double invScale)
	{
		g2.setStroke(
			new BasicStroke(
				(float)(1.4 * invScale),
				BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));

		g2.setColor(
			new Color(75, 75, 85));

		for (EdgeDraw ed : edgeDraws)
		{
			Path2D path =
				new Path2D.Double();

			double[] xy = ed.polyXy;

			path.moveTo(xy[0], xy[1]);

			for (int i = 2; i < xy.length; i += 2)
			{
				path.lineTo(
					xy[i],
					xy[i + 1]);
			}

			g2.draw(path);
		}

		for (EdgeDraw ed : edgeDraws)
		{
			drawArrowHead(
				g2,
				ed.polyXy,
				invScale);

			drawEdgeLabel(
				g2,
				ed);
		}
	}

	/*----------------------------------------------------------------------*/

	private void drawNodes(
		Graphics2D g2,
		double invScale)
	{
		g2.setStroke(
			new BasicStroke(
				(float)(1.0 * invScale)));

		Font titleFont =
			getFont().deriveFont(Font.PLAIN, 12f);

		Font subFont =
			getFont().deriveFont(Font.PLAIN, 10f);

		for (LayoutNode node : layoutNodes)
		{
			Rectangle2D.Double r =
				node.bounds;

			RoundRectangle2D rr =
				new RoundRectangle2D.Double(
					r.x,
					r.y,
					r.width,
					r.height,
					10,
					10);

			g2.setColor(
				new Color(235, 242, 252));

			g2.fill(rr);

			g2.setColor(
				new Color(70, 110, 170));

			g2.draw(rr);

			ImageIcon icon =
				SwingIcons.toolbarIcon(
					SwingIcons.stepTypeIcon(
						node.step.getType()));

			int iy =
				(int)(r.y +
					(r.height - ICON_PX) / 2);

			if (icon != null &&
				icon.getIconWidth() > 0)
			{
				g2.drawImage(
					icon.getImage(),
					(int)(r.x + 8),
					iy,
					ICON_PX,
					ICON_PX,
					null);
			}

			int textPad =
				ICON_PX + 18;

			int tx =
				(int)(r.x + textPad);

			g2.setFont(titleFont);

			FontMetrics tfm =
				g2.getFontMetrics();

			String title =
				truncate(
					node.step.getName(),
					tfm,
					(int)r.width - textPad - 8);

			g2.setColor(Color.BLACK);

			g2.drawString(
				title,
				tx,
				(int)(r.y + 22));

			g2.setFont(subFont);

			FontMetrics sfm =
				g2.getFontMetrics();

			String sub =
				truncate(
					node.step.getType().name(),
					sfm,
					(int)r.width - textPad - 8);

			g2.setColor(Color.DARK_GRAY);

			g2.drawString(
				sub,
				tx,
				(int)(r.y + 40));
		}
	}

	/*----------------------------------------------------------------------*/

	private void drawEdgeLabel(
		Graphics2D g2,
		EdgeDraw ed)
	{
		Font labelFont =
			getFont().deriveFont(Font.PLAIN, 11f);

		g2.setFont(labelFont);

		FontMetrics fm =
			g2.getFontMetrics();

		int lineH = fm.getHeight();

		int maxTw = 0;

		for (String ln : ed.labelLines)
		{
			maxTw = Math.max(
				maxTw,
				fm.stringWidth(ln));
		}

		int totalH =
			lineH * ed.labelLines.size();

		int pad = 3;

		double left =
			ed.labelX - maxTw / 2.0 - pad;

		double top =
			ed.labelY - totalH / 2.0 - pad;

		RoundRectangle2D bg =
			new RoundRectangle2D.Double(
				left,
				top,
				maxTw + pad * 2,
				totalH + pad * 2,
				6,
				6);

		ed.labelBounds =
			new Rectangle2D.Double(
				left,
				top,
				maxTw + pad * 2,
				totalH + pad * 2);

		g2.setComposite(
			AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER,
				0.92f));

		g2.setColor(
			new Color(255, 255, 250));

		g2.fill(bg);

		g2.setComposite(
			AlphaComposite.SrcOver);

		g2.setColor(
			new Color(120, 120, 130));

		g2.draw(bg);

		g2.setColor(
			new Color(40, 40, 50));

		double textTop =
			top + pad + fm.getAscent();

		for (int i = 0; i < ed.labelLines.size(); i++)
		{
			String ln =
				ed.labelLines.get(i);

			double lx =
				ed.labelX -
					fm.stringWidth(ln) / 2.0;

			g2.drawString(
				ln,
				(float)lx,
				(float)(textTop + i * lineH));
		}
	}

	/*----------------------------------------------------------------------*/

	private String edgeLabelDisplay(String edgeLabel)
	{
		String display = edgeLabel;

		if (recipe.getVolumes() != null &&
			recipe.getVolumes().contains(edgeLabel))
		{
			Volume v =
				recipe.getVolumes()
					.getVolume(edgeLabel);

			if (v != null)
			{
				display =
					edgeLabel +
						" (" +
						v.getType().name() +
						")";
			}
		}

		return display;
	}

	/*----------------------------------------------------------------------*/

	private static void drawArrowHead(
		Graphics2D g2,
		double[] xy,
		double invScale)
	{
		int n = xy.length;

		double vx =
			xy[n - 2] - xy[n - 4];

		double vy =
			xy[n - 1] - xy[n - 3];

		double theta =
			Math.atan2(vy, vx);

		double al =
			ARROW_LEN * invScale;

		double aw =
			ARROW_WING * invScale;

		double c = Math.cos(theta);
		double s = Math.sin(theta);

		double bx =
			xy[n - 2] - al * c;

		double by =
			xy[n - 1] - al * s;

		double px = -aw * s;
		double py = aw * c;

		Path2D p =
			new Path2D.Double();

		p.moveTo(xy[n - 2], xy[n - 1]);

		p.lineTo(
			bx + px,
			by + py);

		p.lineTo(
			bx - px,
			by - py);

		p.closePath();

		g2.setColor(
			new Color(55, 55, 70));

		g2.fill(p);
	}

	/*----------------------------------------------------------------------*/

	private static double distanceToSegment(
		double px,
		double py,
		double x1,
		double y1,
		double x2,
		double y2)
	{
		double vx = x2 - x1;
		double vy = y2 - y1;

		double len2 =
			vx * vx + vy * vy;

		if (len2 < 1e-12)
		{
			return Point2D.distance(
				px, py,
				x1, y1);
		}

		double t =
			((px - x1) * vx +
				(py - y1) * vy) / len2;

		t = Math.max(0.0, Math.min(1.0, t));

		double qx = x1 + t * vx;
		double qy = y1 + t * vy;

		return Point2D.distance(
			px, py,
			qx, qy);
	}

	/*----------------------------------------------------------------------*/

	private static double distanceToPolyline(
		double px,
		double py,
		double[] polyXy)
	{
		double best = Double.MAX_VALUE;

		for (int i = 0; i + 3 < polyXy.length; i += 2)
		{
			best = Math.min(
				best,
				distanceToSegment(
					px,
					py,
					polyXy[i],
					polyXy[i + 1],
					polyXy[i + 2],
					polyXy[i + 3]));
		}

		return best;
	}

	/*----------------------------------------------------------------------*/

	private static Rectangle2D.Double inflateRect(
		Rectangle2D.Double r,
		double d)
	{
		return new Rectangle2D.Double(
			r.x - d,
			r.y - d,
			r.width + d * 2,
			r.height + d * 2);
	}

	/*----------------------------------------------------------------------*/

	private static boolean segmentHitsAnyObstacle(
		double ax,
		double ay,
		double bx,
		double by,
		List<Rectangle2D.Double> obstacles)
	{
		Line2D seg =
			new Line2D.Double(
				ax, ay,
				bx, by);

		for (Rectangle2D.Double o : obstacles)
		{
			if (seg.intersects(o))
			{
				return true;
			}
		}

		return false;
	}

	/*----------------------------------------------------------------------*/

	private static double[] routePolyline(
		double x1,
		double y1,
		double x2,
		double y2,
		List<Rectangle2D.Double> obstacles)
	{
		double midY = (y1 + y2) / 2.0;

		// Preferred orthogonal route:
		//
		// source
		//   |
		//   |
		//   +------+
		//          |
		//          |
		//       target

		double[] candidate =
			{
				x1, y1,
				x1, midY,
				x2, midY,
				x2, y2
			};

		if (!polylineHitsAnyObstacle(
			candidate,
			obstacles))
		{
			return candidate;
		}

		// Try offset corridors.

		double[] offsets =
			{
				-160,
				160,
				-260,
				260,
				-360,
				360
			};

		for (double off : offsets)
		{
			double yy = midY + off;

			candidate = new double[]
				{
					x1, y1,
					x1, yy,
					x2, yy,
					x2, y2
				};

			if (!polylineHitsAnyObstacle(
				candidate,
				obstacles))
			{
				return candidate;
			}
		}

		// Last fallback:
		// purely vertical then horizontal.

		candidate = new double[]
			{
				x1, y1,
				x1, y2,
				x2, y2
			};

		if (!polylineHitsAnyObstacle(
			candidate,
			obstacles))
		{
			return candidate;
		}

		// Final fallback:
		// direct line.

		return new double[]
			{
				x1, y1,
				x2, y2
			};
	}

	private static boolean polylineHitsAnyObstacle(
		double[] poly,
		List<Rectangle2D.Double> obstacles)
	{
		for (int i = 0; i + 3 < poly.length; i += 2)
		{
			if (segmentHitsAnyObstacle(
				poly[i],
				poly[i + 1],
				poly[i + 2],
				poly[i + 3],
				obstacles))
			{
				return true;
			}
		}

		return false;
	}

	/*----------------------------------------------------------------------*/

	private static double polylineLength(
		double[] xy)
	{
		double len = 0;

		for (int i = 0; i + 3 < xy.length; i += 2)
		{
			len += Point2D.distance(
				xy[i],
				xy[i + 1],
				xy[i + 2],
				xy[i + 3]);
		}

		return len;
	}

	/*----------------------------------------------------------------------*/

	private static double[] pointAtHalfLength(
		double[] xy)
	{
		double half =
			polylineLength(xy) / 2.0;

		double acc = 0;

		for (int i = 0; i + 3 < xy.length; i += 2)
		{
			double sl =
				Point2D.distance(
					xy[i],
					xy[i + 1],
					xy[i + 2],
					xy[i + 3]);

			if (acc + sl >= half)
			{
				double t =
					(half - acc) / sl;

				return new double[]
					{
						xy[i] +
							t * (xy[i + 2] - xy[i]),

						xy[i + 1] +
							t * (xy[i + 3] - xy[i + 1])
					};
			}

			acc += sl;
		}

		return new double[]
			{
				xy[0],
				xy[1]
			};
	}

	/*----------------------------------------------------------------------*/

	private static List<String> wrapRoughSquare(
		String text,
		FontMetrics fm)
	{
		if (text == null || text.isEmpty())
		{
			return Collections.singletonList("");
		}

		return Collections.singletonList(text);
	}

	/*----------------------------------------------------------------------*/

	private static String truncate(
		String str,
		FontMetrics fm,
		int maxW)
	{
		if (str == null)
		{
			return "";
		}

		if (fm.stringWidth(str) <= maxW)
		{
			return str;
		}

		String ell = "…";

		for (int n = str.length() - 1; n > 0; n--)
		{
			String t =
				str.substring(0, n) + ell;

			if (fm.stringWidth(t) <= maxW)
			{
				return t;
			}
		}

		return ell;
	}

	/*----------------------------------------------------------------------*/

	private static Map<ProcessStep, Integer> longestPathLayers(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		Map<ProcessStep, Integer> lay =
			new HashMap<>();

		for (ProcessStep v : g.vertexSet())
		{
			lay.put(v, 0);
		}

		boolean changed = true;

		while (changed)
		{
			changed = false;

			for (String e : g.edgeSet())
			{
				ProcessStep u =
					g.getEdgeSource(e);

				ProcessStep v =
					g.getEdgeTarget(e);

				int nl =
					lay.get(u) + 1;

				if (nl > lay.get(v))
				{
					lay.put(v, nl);
					changed = true;
				}
			}
		}

		return lay;
	}

	/*----------------------------------------------------------------------*/

	private static List<List<ProcessStep>> buildOrderedLayers(
		DirectedAcyclicGraph<ProcessStep, String> g)
	{
		Map<ProcessStep, Integer> layerOf =
			longestPathLayers(g);

		int maxLayer =
			layerOf.values().stream()
				.max(Integer::compareTo)
				.orElse(0);

		List<List<ProcessStep>> layers =
			new ArrayList<>();

		for (int i = 0; i <= maxLayer; i++)
		{
			layers.add(new ArrayList<>());
		}

		for (ProcessStep v : g.vertexSet())
		{
			layers.get(layerOf.get(v))
				.add(v);
		}

		for (List<ProcessStep> row : layers)
		{
			row.sort(
				Comparator.comparing(
					ProcessStep::getName));
		}

		medianCrossingReduction(
			g,
			layers,
			layerOf);

		return layers;
	}

	/*----------------------------------------------------------------------*/

	private static void medianCrossingReduction(
		DirectedAcyclicGraph<ProcessStep, String> g,
		List<List<ProcessStep>> layers,
		Map<ProcessStep, Integer> layerOf)
	{
		int maxL = layers.size() - 1;

		for (int pass = 0; pass < MEDIAN_PASSES; pass++)
		{
			Map<ProcessStep, Integer> colOf =
				new HashMap<>();

			for (int L = 0; L < layers.size(); L++)
			{
				List<ProcessStep> row =
					layers.get(L);

				for (int i = 0; i < row.size(); i++)
				{
					colOf.put(row.get(i), i);
				}
			}

			boolean topDown =
				(pass & 1) == 0;

			if (topDown)
			{
				for (int L = 0; L <= maxL; L++)
				{
					List<ProcessStep> row =
						layers.get(L);

					final int layer = L;

					row.sort(
						Comparator
							.comparingDouble(
								(ProcessStep v) ->
									medianPredPositions(
										g,
										v,
										layer,
										layerOf,
										colOf))
							.thenComparing(
								ProcessStep::getName));
				}
			}
			else
			{
				for (int L = maxL; L >= 0; L--)
				{
					List<ProcessStep> row =
						layers.get(L);

					final int layer = L;

					row.sort(
						Comparator
							.comparingDouble(
								(ProcessStep v) ->
									medianSuccPositions(
										g,
										v,
										layer,
										layerOf,
										colOf))
							.thenComparing(
								ProcessStep::getName));
				}
			}
		}
	}

	/*----------------------------------------------------------------------*/

	private static double medianPredPositions(
		DirectedAcyclicGraph<ProcessStep, String> g,
		ProcessStep v,
		int layerL,
		Map<ProcessStep, Integer> layerOf,
		Map<ProcessStep, Integer> colOf)
	{
		List<Integer> xs =
			new ArrayList<>();

		for (String e : g.incomingEdgesOf(v))
		{
			ProcessStep u =
				g.getEdgeSource(e);

			if (layerOf.get(u) < layerL)
			{
				Integer ix =
					colOf.get(u);

				if (ix != null)
				{
					xs.add(ix);
				}
			}
		}

		return medianValue(xs);
	}

	/*----------------------------------------------------------------------*/

	private static double medianSuccPositions(
		DirectedAcyclicGraph<ProcessStep, String> g,
		ProcessStep v,
		int layerL,
		Map<ProcessStep, Integer> layerOf,
		Map<ProcessStep, Integer> colOf)
	{
		List<Integer> xs =
			new ArrayList<>();

		for (String e : g.outgoingEdgesOf(v))
		{
			ProcessStep w =
				g.getEdgeTarget(e);

			if (layerOf.get(w) > layerL)
			{
				Integer ix =
					colOf.get(w);

				if (ix != null)
				{
					xs.add(ix);
				}
			}
		}

		return medianValue(xs);
	}

	/*----------------------------------------------------------------------*/

	private static double medianValue(
		List<Integer> xs)
	{
		if (xs.isEmpty())
		{
			return -1e200;
		}

		Collections.sort(xs);

		int n = xs.size();

		if ((n & 1) == 1)
		{
			return xs.get(n / 2);
		}

		return
			(xs.get(n / 2 - 1) +
				xs.get(n / 2)) / 2.0;
	}

	/*----------------------------------------------------------------------*/

	private static double clamp(
		double v,
		double min,
		double max)
	{
		return Math.max(min, Math.min(max, v));
	}

	/*----------------------------------------------------------------------*/

	private static final class LayoutNode
	{
		final ProcessStep step;

		double x;
		double y;

		double vx;
		double vy;

		double width;
		double height;

		int layer;

		Rectangle2D.Double bounds;

		LayoutNode(ProcessStep step)
		{
			this.step = step;
		}
	}

	/*----------------------------------------------------------------------*/

	private static final class LayoutEdge
	{
		final LayoutNode src;
		final LayoutNode dst;

		final String volumeId;

		double[] polyline;

		EdgeDraw draw;

		LayoutEdge(
			LayoutNode src,
			LayoutNode dst,
			String volumeId)
		{
			this.src = src;
			this.dst = dst;
			this.volumeId = volumeId;
		}
	}

	/*----------------------------------------------------------------------*/

	private static final class EdgeDraw
	{
		final String volumeId;
		String displayLabel;

		final double[] polyXy;

		List<String> labelLines;

		final double labelX;
		final double labelY;

		Rectangle2D.Double labelBounds;

		EdgeDraw(
			String volumeId,
			String displayLabel,
			double[] polyXy,
			List<String> labelLines,
			double labelX,
			double labelY)
		{
			this.volumeId = volumeId;
			this.displayLabel = displayLabel;
			this.polyXy = polyXy;
			this.labelLines = labelLines;
			this.labelX = labelX;
			this.labelY = labelY;
		}
	}
}