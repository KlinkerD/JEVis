/**
 * Copyright (C) 2014 Envidatec GmbH <info@envidatec.com>
 *
 * This file is part of JEApplication.
 *
 * JEApplication is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation in version 3.
 *
 * JEApplication is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JEApplication. If not, see <http://www.gnu.org/licenses/>.
 *
 * JEApplication is part of the OpenJEVis project, further project information
 * are published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.application.resource;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 *
 * @author fs
 */
public class ResourceLoader {

    public static String getResource(String file) {
        System.out.println("get Resouce: " + file);
        return ResourceLoader.class.getResource("/styles/" + file).toExternalForm();
    }

    public static Image getImage(String icon) {
        try {
            return new Image(ResourceLoader.class.getResourceAsStream("/icons/" + icon));
        } catch (Exception ex) {
            System.out.println("Could not load icon: " + "/icons/   " + icon + " :" + ex);
            return new Image(ResourceLoader.class.getResourceAsStream("/icons/1393355905_image-missing.png"));
        }
    }

    /**
     *
     * @param icon
     * @param height
     * @param width
     * @return
     */
    public static ImageView getImage(String icon, double height, double width) {
        ImageView image = new ImageView(ResourceLoader.getImage(icon));
        image.fitHeightProperty().set(height);
        image.fitWidthProperty().set(width);
        return image;
    }
}
