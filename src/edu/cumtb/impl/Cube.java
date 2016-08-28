package edu.cumtb.impl;

import edu.cumtb.DGGS;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.OGLUtil;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

import javax.media.opengl.GL2;
import java.awt.*;

/**
 * Created by tbpwang@gmail.com
 * 2016/8/28.
 */

/**
 * The simplest example of a custom renderable
 */
public class Cube extends DGGS {

    //Geographic position of the cube
    protected Position position;
    //Length of each face, in meters
    protected double size;

    public Cube(Position position, double sizeInMeters) {
        this.position = position;
        this.size = sizeInMeters;
    }

    public Cube() {
    }

    // Support object to help with pick resolution.
    protected PickSupport pickSupport = new PickSupport();

    // Determined each frame
    protected long frameTimeStamp = -1L;
    protected OrderedCube currentFramesOrderedCube;


    @Override
    public void render(DrawContext drawContext) {
        // TODO: 2016/8/28
        // called twice, once for picking and once for rendering.
        // In both cases an OrderedCube is added to the ordered renderable queue.
        OrderedCube orderedCube = this.makeOrderedRenderable(drawContext);

        if (orderedCube.extent != null) {
            if (!this.intersectFrustum(drawContext, orderedCube)) {
                return;
            }
            if (drawContext.isSmall(orderedCube.extent, 1)) {
                return;
            }
        }

        // Add the cube to the ordered renderable queue.
        // The SceneController sorts the ordered renderables by eye distance,
        // and then renders them back to front.
        drawContext.addOrderedRenderable(orderedCube);
    }

    private boolean intersectFrustum(DrawContext drawContext, OrderedCube orderedCube) {
        //Determines if the cube intersects the view frustum.

        if (drawContext.isPickingMode()) {
            return drawContext.getPickFrustums().intersectsAny(orderedCube.extent);
        }
        return drawContext.getView().getFrustumInModelCoordinates().intersects(orderedCube.extent);
    }

    @Override
    public void drawOrderedRenderable(DrawContext drawContext, PickSupport pickSupport) {
        // Set up drawing state, and draw it.
        // Called twice when the cube is rendered in ordered rendering mode.
        this.beginDrawing(drawContext);

        GL2 gl = drawContext.getGL().getGL2();
        if (drawContext.isPickingMode()) {
            Color color = drawContext.getUniquePickColor();
            pickSupport.addPickableObject(color.getRGB(), this, this.position);
            gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }

        // Render a unit cube and apply a scaling factor to scale the cube to the appropriate size.
        gl.glScaled(this.size, this.size, this.size);
        this.drawUnitCube(drawContext);


        this.endDrawing(drawContext);
    }

    private void drawUnitCube(DrawContext drawContext) {
        // Draw a unit cube, using the active modelview matrix to orient the shape.

        // Vertices of a unit cube, centered on origin
        float[][] v = {{-0.5f, 0.5f, -0.5f}, {-0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, 0.5f}, {0.5f, 0.5f, -0.5f}, {-0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, 0.5f}, {0.5f, -0.5f, -0.5f}, {-0.5f, -0.5f, -0.5f}};

        //  array to group vertices into faces
        int[][] faces = {{0, 1, 2, 3}, {2, 5, 6, 3}, {1, 4, 5, 2}, {0, 7, 4, 1}, {0, 7, 6, 3}, {4, 7, 6, 5}};
        //Normal vectors for each face
        float[][] n = {{0, 1, 0}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {0, -1, 0}};

        // Real applications should use vertex arrays or vertex buffer objects to achieve better performance.
        // Draw the cube in OpenGL immediate mode for simplicity.
        GL2 gl = drawContext.getGL().getGL2();
        gl.glBegin(GL2.GL_QUADS);
        for (int i = 0; i < faces.length; i++) {
            gl.glNormal3f(n[i][0], n[i][1], n[i][2]);
            for (int j = 0; j < faces[0].length; j++) {
                gl.glVertex3f(v[faces[i][j]][0], v[faces[i][j]][1], v[faces[i][j]][2]);
            }
        }

        gl.glEnd();

    }

    @Override
    public void beginDrawing(DrawContext drawContext) {
        // Setup drawing state in preparation for drawing.
        // State changed by this method must be restored in endDrawing.

        GL2 gl = drawContext.getGL().getGL2();
        int attrMask = GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT;

        gl.glPushAttrib(attrMask);

        if (!drawContext.isPickingMode()) {
            drawContext.beginStandardLighting();
            gl.glEnable(GL2.GL_BLEND);
            OGLUtil.applyBlending(gl, false);

            // Re-normalized before lighting is computed.
            gl.glEnable(GL2.GL_NORMALIZE);
        }

        // Multiply the modelview matrix by a surface orientation matrix to set up a local coordinate system
        // with the origin at the cube's center position,
        // the Y axis pointing North, the X axis pointing East, and the Z axis normal to the globe.
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        Matrix matrix = drawContext.getGlobe().computeSurfaceOrientationAtPosition(this.position);
        matrix = drawContext.getView().getModelviewMatrix().multiply(matrix);

        double[] matrixArray = new double[16];
        matrix.toArray(matrixArray, 0, false);
        gl.glLoadMatrixd(matrixArray, 0);

    }

    @Override
    public void endDrawing(DrawContext drawContext) {
        // Restore drawing state changed in beginDrawing to the default.

        GL2 gl = drawContext.getGL().getGL2();
        if (!drawContext.isPickingMode()) {
            drawContext.endStandardLighting();
        }
        gl.glPopAttrib();
    }

    private OrderedCube makeOrderedRenderable(DrawContext drawContext) {
        // Compute per-frame attributes, and add the ordered renderable to the ordered renderable list.

        // This method is called twice each frame: once during picking and once during rendering.
        // We only need to compute the placePoint, eye distance and extent once per frame,
        // so check the frame timestamp to see if this is a new frame.
        // However, 2D continuous globes cannot be used.

        if (drawContext.getFrameTimeStamp() != this.frameTimeStamp || drawContext.isContinuous2DGlobe()) {
            OrderedCube orderedCube = new OrderedCube();

            // Convert the cube's geographic position to a position in Cartesian coordinates.
            // If drawing to a 2D globe ignore the shape's altitude.
            if (drawContext.is2DGlobe()) {
                orderedCube.placePoint = drawContext.getGlobe().computePointFromPosition(this.position.getLatitude(), this.position.getLongitude(), 0);
            } else {
                orderedCube.placePoint = drawContext.getGlobe().computePointFromPosition(this.position);
            }

            // Compute the distance from the eye to the cube's position.
            orderedCube.eyeDistance = drawContext.getView().getEyePoint().distanceTo3(orderedCube.placePoint);

            // Compute a sphere that encloses the cube.
            // We'll this sphere for intersection calculations to determine if the cube is actually visible.
            orderedCube.extent = new Sphere(orderedCube.placePoint, Math.sqrt(3) * this.size / 2.0);

            // Keep track of the time stamp used to compute the ordered renderable
            this.frameTimeStamp = drawContext.getFrameTimeStamp();
            this.currentFramesOrderedCube = orderedCube;
        }
        return this.currentFramesOrderedCube;
    }

    private class OrderedCube implements OrderedRenderable {

        //Cartesian position of the cube, computed from
        protected Vec4 placePoint;
        //Distance from the eye point to the cube
        protected double eyeDistance;
        // The cube's Cartesian bounding extent
        protected Extent extent;

        @Override
        public double getDistanceFromEye() {
            return this.eyeDistance;
        }

        @Override
        public void pick(DrawContext drawContext, Point point) {
            // Use the same code for rendering and picking
            this.render(drawContext);
        }

        @Override
        public void render(DrawContext drawContext) {
            Cube.this.drawOrderedRenderable(drawContext, Cube.this.pickSupport);
        }
    }

    protected static class AppFrame extends ApplicationTemplate.AppFrame{
        public AppFrame() {
            super(true,true,true);
            RenderableLayer layer = new RenderableLayer();
            Cube cube = new Cube(Position.fromDegrees(40, 116, 3000), 1000);
            layer.addRenderable(cube);

            getWwd().getModel().getLayers().add(layer);

        }
    }

    public static void main(String[] args) {
        Configuration.setValue(AVKey.INITIAL_LATITUDE, 40);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, 116);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 15500);
        Configuration.setValue(AVKey.INITIAL_PITCH, 45);
        Configuration.setValue(AVKey.INITIAL_HEADING, 45);

        ApplicationTemplate.start("World Wind Cube.", AppFrame.class);
    }
}
