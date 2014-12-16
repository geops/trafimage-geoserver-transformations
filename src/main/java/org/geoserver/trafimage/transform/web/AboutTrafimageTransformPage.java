/* Copyright (c) 2014 geOps - www.geops.de. All rights reserved.
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.trafimage.transform.web;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.trafimage.transform.TrafimageTransformInfo;
import org.geoserver.web.GeoServerBasePage;

public class AboutTrafimageTransformPage extends GeoServerBasePage {

	public AboutTrafimageTransformPage() {
		TrafimageTransformInfo info = new TrafimageTransformInfo();
		add(new Label("trafimageTransformVersion", info.getVersion()));
		add(new Label("trafimageTransformGitVersion", info.getGitVersion()));
		add(new Label("readme", info.getReadmeHtml()).setEscapeModelStrings(false));
	}

	public String getAjaxIndicatorMarkupId() {
		return "ajaxFeedback";
	}

}
