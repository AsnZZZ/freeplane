/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.layout.mindmapmode;

import org.freeplane.api.ChildNodesLayout;
import org.freeplane.core.undo.IActor;
import org.freeplane.features.layout.LayoutController;
import org.freeplane.features.layout.LayoutModel;
import org.freeplane.features.map.IExtensionCopier;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.styles.LogicalStyleKeys;
/**
 * @author Dimitry Polivaev
 */
public class MLayoutController extends LayoutController {

	private static class StyleCopier implements IExtensionCopier {
		@Override
        public void copy(Object key, NodeModel from, NodeModel to) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
			LayoutModel source = from.getExtension(LayoutModel.class);
			if(source != null){
				LayoutModel layoutModel = LayoutModel.createLayoutModel(to);
				layoutModel.setChildNodesLayout(layoutModel.getChildNodesLayout());
			}
		}

		@Override
        public void remove(Object key, NodeModel from) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
			LayoutModel target = from.getExtension(LayoutModel.class);
			if(target != null){
                target.setChildNodesLayout(ChildNodesLayout.NOT_SET);
			}
		}

		@Override
        public void remove(Object key, NodeModel from, NodeModel which) {
			if (!key.equals(LogicalStyleKeys.NODE_STYLE)) {
				return;
			}
			LayoutModel model = which.getExtension(LayoutModel.class);
            if(model == null)
                return;
			LayoutModel target = from.getExtension(LayoutModel.class);
            if(target == null)
                return;
            if(model.getChildNodesLayout() != ChildNodesLayout.NOT_SET ){
                target.setChildNodesLayout(ChildNodesLayout.NOT_SET);
            }
		}
	}

	public MLayoutController() {
		super();
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.registerExtensionCopier(new StyleCopier());
	}

    public void setChildNodesLayout(NodeModel node, ChildNodesLayout sides){
        if(node != null){
            final IActor actor = new ChangeChildNodesLayoutActor(node, sides != null ? sides : ChildNodesLayout.NOT_SET);
            Controller.getCurrentModeController().execute(actor, node.getMap());
        }
    }
}
