package org.thadeus.rtiviewer;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class RTISurfaceView extends GLSurfaceView {
	private RTIRenderer mRenderer;

	public RTISurfaceView(Context context, RTI rti) {
		super(context);

		// Create an OpenGL ES 2.0 context.
		setEGLContextClientVersion(2);

		// Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new RTIRenderer(rti) {
            @Override
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                super.onSurfaceCreated(unused, config);
                // Displays a default rendering rather than a black screen when the view loads
                updatePosition(getWidth() / 2, getHeight() / 2);
            }
        };
		setRenderer(mRenderer);

        // Render the view only when there is a change
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

    @Override 
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
			updatePosition(e.getX(), e.getY());
        }
        return true;
    }
    
    private void updatePosition(float x, float y) {
        float midx = (float)getWidth()/2.0f;
        float midy = (float)getHeight()/2.0f;
        float radius = min(midx,midy);

        // compute a new lighting angle depening on where the mouse was clicked.                                                                                                        
        // you can change this way of selecting the lighting direction                                                                                                                  
        // also keep in mind that the light vector (lx, ly, lz) must be normalized.                                                                                                     
        float lx = (x-midx)/(float)radius;
        float ly = -(y-midy)/(float)radius;
        float lz = (float) sqrt(1-lx*lx-ly*ly);
        updatePosition(lx, ly, lz);
    }
    
    public void updatePosition(float lx, float ly, float lz) {
    	// Normalize the vector
    	double len=Math.sqrt(lx*lx+ly*ly+lz*lz);
    	lx/=len;
    	ly/=len;
    	lz/=len;

        // Note that we donot prevent user from clicking outside the circle specified by 'radius'                                                                                       
        // This leads to undefinied lz values resulting in a blank image being displayed.                                                                                               
        // Clamp the coordinates appropriately.                                                                                                                                         

//        float width = this->width();                                                                                                                                                  
//        float height = this->height();                                                                                                                                                
//        x = x*2/width - 1;                                                                                                                                                            
//        y = y*2/height - 1;                                                                                                                                                           
//        float z = 1;                                                                                                                                                                  
//        float d = sqrt(x*x+y*y+z*z);                                                                                                                                                  
//        lx = x/d;                                                                                                                                                                     
//        ly = -y/d;                                                                                                                                                                    
//        lz = z/d;                                                                                                                                                                     

        // Computes the weights based on the lighting direction                                                                                                                         
        double phi = atan2(ly,lx);
        if(phi<0) {
            phi += 2 * PI;
        }
        double theta = acos(lz);
        float[] weights=mRenderer.weights;

        weights[0]  = (float) (1/sqrt(2*PI));
        weights[1]  = (float) (sqrt(6/PI)     *  (cos(phi)*sqrt(cos(theta)-cos(theta)*cos(theta))));
        weights[2]  = (float) (sqrt(3/(2*PI)) *  (-1 + 2*cos(theta)));
        weights[3]  = (float) (sqrt(6/PI)     *  (sqrt(cos(theta) - cos(theta)*cos(theta))*sin(phi)));

        weights[4]  = (float) (sqrt(30/PI)    *  (cos(2*phi)*(-cos(theta) + cos(theta)*cos(theta))));
        weights[5]  = (float) (sqrt(30/PI)    *  (cos(phi)*(-1 + 2*cos(theta))*sqrt(cos(theta) - cos(theta)*cos(theta))));
        weights[6]  = (float) (sqrt(5/(2*PI)) *  (1 - 6*cos(theta) + 6*cos(theta)*cos(theta)));
        weights[7]  = (float) (sqrt(30/PI)    *  ((-1 + 2*cos(theta))*sqrt(cos(theta) - cos(theta)*cos(theta))*sin(phi)));
        weights[8]  = (float) (sqrt(30/PI)    *  ((-cos(theta) + cos(theta)*cos(theta))*sin(2*phi)));

//        if(order > 3) {
//            weights[9]  = (float) (2*sqrt(35/PI)  *  cos(3*phi)*pow(cos(theta) - cos(theta)*cos(theta),(3/2)));
//            weights[10] = (float) (sqrt(210/PI)  *  cos(2*phi)*(-1 + 2*cos(theta))*(-cos(theta) + cos(theta)*cos(theta)));
//            weights[11] = (float) (2*sqrt(21/PI)  *  cos(phi)*sqrt(cos(theta) - cos(theta)*cos(theta))*(1 - 5*cos(theta) + 5*cos(theta)*cos(theta)));
//            weights[12] = (float) (sqrt(7/(2*PI)) *  (-1 + 12*cos(theta) - 30*cos(theta)*cos(theta) + 20*cos(theta)*cos(theta)*cos(theta)));
//            weights[13] = (float) (2*sqrt(21/PI)  *  sqrt(cos(theta) - cos(theta)*cos(theta))*(1 - 5*cos(theta) + 5*cos(theta)*cos(theta))*sin(phi));
//            weights[14] = (float) (sqrt(210/PI)  *  (-1 + 2*cos(theta))*(-cos(theta) + cos(theta)*cos(theta))*sin(2*phi));
//            weights[15] = (float) (2*sqrt(35/PI)  *  pow(cos(theta) - cos(theta)*cos(theta),(3/2))*sin(3*phi));
//        } else {
//            for(int i = 9; i < 16; i++) {
//                weights[i] = 0;
//            }
//        }

            requestRender();
       }
}
