package org.freeplane.features.note;

import java.awt.Color;
import java.awt.Font;

import org.freeplane.core.ui.components.html.CssRuleBuilder;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeSizeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.styles.MapStyleModel;

public class NoteStyleAccessor {
	final private String rule;
	final private Color noteForeground;
	private Color noteBackground;
	public NoteStyleAccessor(ModeController modeController, NodeModel node, float zoom, boolean asHtmlFragment) {
		final Controller controller = modeController.getController();
		MapModel map = controller.getMap();
		if(map != null){
			final MapStyleModel model = MapStyleModel.getExtension(map);
			final NodeModel noteStyleNode = model.getStyleNodeSafe(MapStyleModel.NOTE_STYLE);
			final NodeStyleController style = Controller.getCurrentModeController().getExtension(
				NodeStyleController.class);
			final Font noteFont = style.getFont(noteStyleNode);
			this.noteBackground = style.getBackgroundColor(noteStyleNode);
			this.noteForeground = style.getColor(noteStyleNode);
			final int alignment = style.getHorizontalTextAlignment(noteStyleNode).swingConstant;
			final CssRuleBuilder cssRuleBuilder = new CssRuleBuilder();
			if(asHtmlFragment)
				cssRuleBuilder.withHTMLFont(noteFont);
			else
				cssRuleBuilder.withCSSFont(noteFont);
			cssRuleBuilder.withColor(noteForeground)
			.withBackground((noteBackground != null ? noteBackground : //
				controller.getMapViewManager().getMapViewComponent().getBackground()))
			.withAlignment(alignment);
			if(asHtmlFragment)
				cssRuleBuilder.withMaxWidthAsPt(zoom, NodeSizeModel.getMaxNodeWidth(noteStyleNode), style.getMaxWidth(node));
			this.rule = cssRuleBuilder.toString();
		}
		else {
			this.rule = "";
			this.noteForeground = null;
			this.noteBackground = null;
		}

	}
	public String getNoteCSSStyle() {
		return rule;
	}
	public Color getNoteForeground() {
		return noteForeground;
	}
	public Color getNoteBackground() {
		return noteBackground;
	}


}