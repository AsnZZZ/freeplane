/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
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
package org.freeplane.features.common.icon;

import org.freeplane.core.io.IAttributeHandler;
import org.freeplane.core.io.IElementDOMHandler;
import org.freeplane.core.io.IXMLElement;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.model.MindIcon;
import org.freeplane.core.model.NodeModel;

class IconBuilder implements IElementDOMHandler {
	static class IconProperties {
		String iconName;
	}

	public Object createElement(final Object parent, final String tag, final IXMLElement attributes) {
		if (tag.equals("icon")) {
			return new IconProperties();
		}
		return null;
	}

	public void endElement(final Object parent, final String tag, final Object userObject,
	                       final IXMLElement dom) {
		if (parent instanceof NodeModel && tag.equals("icon")) {
			final NodeModel node = (NodeModel) parent;
			final IconProperties ip = (IconProperties) userObject;
			node.addIcon(MindIcon.factory(ip.iconName), MindIcon.LAST);
			return;
		}
	}

	private void registerAttributeHandlers(final ReadManager reader) {
		reader.addAttributeHandler("icon", "BUILTIN", new IAttributeHandler() {
			public void setAttribute(final Object userObject, final String value) {
				final IconProperties ip = (IconProperties) userObject;
				ip.iconName = value.toString();
			}
		});
	}

	/**
	 */
	public void registerBy(final ReadManager reader) {
		reader.addElementHandler("icon", this);
		registerAttributeHandlers(reader);
	}

	public void setAttributes(final String tag, final Object node, final IXMLElement attributes) {
	}
}
