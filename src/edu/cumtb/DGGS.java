package edu.cumtb;

import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

/**
 * Created by tbpwang@gmail.com
 * 2016/8/28.
 */
public abstract class DGGS extends ApplicationTemplate implements Renderable {

    public abstract void drawOrderedRenderable(DrawContext drawContext, PickSupport pickSupport);

    public abstract void beginDrawing(DrawContext drawContext);

    public abstract void endDrawing(DrawContext drawContext);
}
