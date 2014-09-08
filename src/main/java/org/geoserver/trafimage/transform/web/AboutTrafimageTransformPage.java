package org.geoserver.trafimage.transform.web;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.trafimage.transform.TrafimageTransformInfo;
import org.geoserver.web.GeoServerBasePage;

public class AboutTrafimageTransformPage extends GeoServerBasePage {

	public AboutTrafimageTransformPage() {
		TrafimageTransformInfo info = new TrafimageTransformInfo();
		add(new Label("trafimageTransformVersion", info.getVersion()));
		add(new Label("trafimageTransformGitVersion", info.getGitVersion()));
	}

	public String getAjaxIndicatorMarkupId() {
		return "ajaxFeedback";
	}

}
