/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.layer.LayerDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Provides a filtered, sorted view over the catalog layers for adding edit URLs.
 */
@SuppressWarnings("serial")
public class AddEditUrlLayerProvider extends GeoServerDataProvider<LayerInfo> {
    static final Property<LayerInfo> TYPE = new BeanProperty<LayerInfo>("type",
            "type");

    static final Property<LayerInfo> WORKSPACE = new BeanProperty<LayerInfo>(
            "workspace", "resource.store.workspace.name");

    static final Property<LayerInfo> STORE = new BeanProperty<LayerInfo>(
            "store", "resource.store.name");

    static final Property<LayerInfo> NAME = new BeanProperty<LayerInfo>("name",
            "name");

    static final Property<LayerInfo> ADD = new PropertyPlaceholder<LayerInfo>(
            "add");
    
    static final List<Property<LayerInfo>> PROPERTIES = Arrays.asList(TYPE,
            WORKSPACE, STORE, NAME, ADD);

    @Override
    protected List<LayerInfo> getItems() {
        return getCatalog().getLayers();
    }

    @Override
    protected List<Property<LayerInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    public IModel newModel(Object object) {
        return new LayerDetachableModel((LayerInfo) object);
    }

    @Override
    protected Comparator<LayerInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }
}
