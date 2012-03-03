/*
    Copyright 2012 Adam Crume

    This file is part of RTIViewer.

    RTIViewer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    RTIViewer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RTIViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thadeus.rtiviewer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;

public class RTIRenderer implements Renderer {
	private static final int FLOAT_SIZE_BYTES = 4;

	private static final int NTERMS = 9;

	private static final int NTERMS_SHADER = 3;

	private FloatBuffer triangleVB;
	private FloatBuffer triangleTB;
	private FloatBuffer[] fb = new FloatBuffer[NTERMS];
	private int[] framebuffers;
	private int[] renderTex;
	private float[] weightsPartial = new float[NTERMS_SHADER];


	private final String vertexShaderCode = 
			"attribute highp vec4 vertex;\n" +
	        "attribute highp vec4 texCoord;\n" +
	        "uniform mediump mat4 matrix;\n" +
	        "varying highp vec4 texc;\n" +
	        "void main(void)\n" +
	        "{\n" +
	        "    gl_Position = vertex;\n" +
	        "    texc = texCoord;\n" +
	        "}\n";

	// Note: ERROR: 0:8: '[q]' : arrays of samplers may only be indexed by a constant integer expression
//    "    for(int q = 0 ; q < " + NTERMS + "; q++) {\n" +
//    "        color += texture2D(hshimage[q], texc.st) * weights[q];\n" +
//    "    }\n" +
        private final String fragmentShaderCode = 
        		"varying highp vec4 texc;\n" +
                "uniform highp float weights[" + NTERMS_SHADER + "];\n" +
                "uniform sampler2D hshimage[" + NTERMS_SHADER + "];\n" +
                "void main(void)\n" +
                "{\n" +
				"    highp vec4 color = texture2D(hshimage[0], texc.st) * weights[0];\n" +
                "    color += texture2D(hshimage[1], texc.st) * weights[1];\n" +
                "    color += texture2D(hshimage[2], texc.st) * weights[2];\n" +
                "    color.r = color.r < 0.0 ? 0.0 : color.r > 1.0 ? 1.0 : color.r;\n" +
                "    color.g = color.g < 0.0 ? 0.0 : color.g > 1.0 ? 1.0 : color.g;\n" +
                "    color.b = color.b < 0.0 ? 0.0 : color.b > 1.0 ? 1.0 : color.b;\n" +
                "    color.a = 1.0;\n" +
                "    gl_FragColor = color;\n" +
                "}\n";

	private int mProgram;
	private int vertexAttr;
	private int texCoordAttr;
	private int weightsUniform;
	private int[] hshimageUniforms = new int[NTERMS_SHADER];

    private int[] textures=new int[NTERMS];
    float[] weights=new float[NTERMS];

    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewHeight;

	private int rtiheight;

	private int rtiwidth;

	private RTI rti;

	public RTIRenderer(RTI rti) {
		this.rti = rti;
	}

	private void initShapes() {
		float afVerticesFull[] = { -1, -1, -1,
				1, -1, -1,
				1, 1, -1,
				-1, 1, -1 };

		float afTexCoordFull[] = { 0.0f, 0.0f,
				1.0f, 0.0f,
				1.0f, 1.0f,
				0.0f, 1.0f };
        
        // initialize vertex Buffer for triangle  
        ByteBuffer vbb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
        		afVerticesFull.length * 4); 
        vbb.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
        triangleVB = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
		triangleVB.put(afVerticesFull); // add the coordinates to the FloatBuffer
		triangleVB.position(0); // set the buffer to read the first coordinate

        vbb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
        		afTexCoordFull.length * 4); 
        vbb.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
        triangleTB = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
		triangleTB.put(afTexCoordFull); // add the coordinates to the FloatBuffer
		triangleTB.position(0); // set the buffer to read the first coordinate
	}

	private int loadShader(int type, String shaderCode) {
		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
		// or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
		int shader = GLES20.glCreateShader(type);
		check();

		// add the source code to the shader and compile it
		GLES20.glShaderSource(shader, shaderCode);
		check();
		GLES20.glCompileShader(shader);
		check();

		return shader;
	}

	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		check();
		// Set the background frame color
		GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
		check();
		
		initShapes();

	    int border = 0; // no texture border                                                                                                                                          
	    int mipmap_level = 0; // mipmap level is 0 for base image
	    // TODO: See what to use since we can't use GL_RGB32F_ARB
	    //int internal_format = GLES20.GL_RGB32F_ARB; // internal format opengl stores the image as, we want to use 32-bit floating point
	    int internal_format = GLES20.GL_RGB; // internal format opengl stores the image as, we want to use 32-bit floating point
		GLES20.glGenTextures(NTERMS, textures, 0);
		check();
		float[] allData = rti.hshpixels;
		rtiheight = rti.rtiheight;
		rtiwidth = rti.rtiwidth;
		//FloatBuffer pixelBuffer = FloatBuffer.allocateDirect(tex_width * tex_height * 3).order(ByteOrder.nativeOrder());
		//byte[] data3=new byte[tex_width*tex_height*3];
	    for(int i = 0; i < NTERMS; i++) {
//			float[] data3=new float[tex_width*tex_height*3];
//	    	System.out.println("Setting up texture "+i);
//	        for(int x = 0; x < tex_width; x++) {
//	            for(int y = 0; y < tex_height; y++) {
//	                for(int band = 0; band < 3; band++) {
//	                    int index = rti.getindex(y, x, band, i);
//						int ix = (y * tex_width + x) * 3 + band;
//	                    try {
//	                    //data3[ix] = (byte) (256 * allData[index]);
//						data3[ix] = allData[index];
//	                    } catch(ArrayIndexOutOfBoundsException e) {
//	                    	throw new RuntimeException("ix = "+ix+", data3.length = "+data3.length+", index = "+index+", allData.length = "+allData.length, e);
//	                    }
//	                }
//	            }
//	        }
			float[] data3=new float[256*256*3];
	    	System.out.println("Setting up texture "+i);
	        for(int xx = 0; xx < 256; xx++) {
	            for(int yy = 0; yy < 256; yy++) {
	            	int x=xx*rtiwidth/256;
	            	int y=yy*rtiheight/256;
	                for(int band = 0; band < 3; band++) {
	                    int index = rti.getindex(y, x, band, i);
						int ix = (yy * 256 + xx) * 3 + band;
	                    try {
	                    //data3[ix] = (byte) (256 * allData[index]);
						data3[ix] = allData[index];
	                    } catch(ArrayIndexOutOfBoundsException e) {
	                    	throw new RuntimeException("ix = "+ix+", data3.length = "+data3.length+", index = "+index+", allData.length = "+allData.length, e);
	                    }
	                }
	            }
	        }
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			check();
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
			check();
			
	        ByteBuffer vbb = ByteBuffer.allocateDirect(
	                // (# of coordinate values * 4 bytes per float)
	        		data3.length * 4); 
	        vbb.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
	        fb[i] = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
	        fb[i].put(data3);
	        fb[i].position(0);
//			fb[i] = FloatBuffer.wrap(data3);
//	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, mipmap_level, internal_format, tex_width, tex_height, border, GLES20.GL_RGB, GLES20.GL_FLOAT, fb[i]);
	        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, mipmap_level, internal_format, 256, 256, border, GLES20.GL_RGB, GLES20.GL_FLOAT, fb[i]);
			check();
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR );
			check();
	        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR );
			check();
	    }

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
		check();
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
		check();
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
		check();
		String shaderLog = GLES20.glGetShaderInfoLog(fragmentShader);
		System.out.println("shaderLog: "+shaderLog);
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL program executables
		check();
		String programLog = GLES20.glGetProgramInfoLog(mProgram);
		System.out.println("programLog: "+programLog);
		shaderLog = GLES20.glGetShaderInfoLog(fragmentShader);
		System.out.println("shaderLog: "+shaderLog);
		check();
        
		// get handle to the vertex shader's vPosition member
        vertexAttr = GLES20.glGetAttribLocation(mProgram, "vertex");
		check();
        texCoordAttr = GLES20.glGetAttribLocation(mProgram, "texCoord");
		check();
        weightsUniform = GLES20.glGetUniformLocation(mProgram, "weights");
		check();
		for(int i=0;i<NTERMS_SHADER;i++) {
			hshimageUniforms[i] = GLES20.glGetUniformLocation(mProgram, "hshimage["+i+"]");
			check();
		}

		createTempTextures();
	}


	private void createTempTextures() {
		framebuffers = new int[2];
		int[] renderbuffers = new int[2];
		renderTex = new int[2];
		GLES20.glGenFramebuffers(2, framebuffers, 0);
		check();
		GLES20.glGenRenderbuffers(2, renderbuffers, 0);
		check();
		GLES20.glGenTextures(2, renderTex, 0);
		check();
		for(int i=0;i<renderTex.length;i++) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[i]);
		check();

		// parameters - we have to make sure we clamp the textures to the edges
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		check();
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		check();
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		check();
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		check();

		int texW = 256;
		int texH = 256;
		int[] buf = new int[texW * texH];
		IntBuffer texBuffer = ByteBuffer.allocateDirect(buf.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();

		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, texW, texH, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, texBuffer);
		check();

		// create render buffer and bind 16-bit depth buffer
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderbuffers[i]);
		check();
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_RGBA4, texW, texH);
		check();
		}
	}

	private void paintTexturedRectangle(int step) {
		// 0 1 2
		// - 3 4
		// - 5 6
		// - 7 8
		check();
		if (step == 0) {
			System.arraycopy(weights, 0, weightsPartial, 0, NTERMS_SHADER);
		} else {
			weightsPartial[0] = 1;
			System.arraycopy(weights, step*2+1, weightsPartial, 1, NTERMS_SHADER - 1);
		}
		GLES20.glUniform1fv(weightsUniform, NTERMS_SHADER, weightsPartial, 0);
		check();

		GLES20.glVertexAttribPointer(vertexAttr, 3, GLES20.GL_FLOAT, false, 0, triangleVB);
		check();
		GLES20.glVertexAttribPointer(texCoordAttr, 2, GLES20.GL_FLOAT, false, 0, triangleTB);
		check();
		
		// Enable the vertex and the texture-coordinate arrays
		GLES20.glEnableVertexAttribArray(vertexAttr);
		check();
		GLES20.glEnableVertexAttribArray(texCoordAttr);
		check();

		for (int i = 0; i < NTERMS_SHADER; i++) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			check();
			if(step==0) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
			} else {
				int tex;
				if(i==0) {
					tex=renderTex[1-(step%2)];
				} else {
					tex=textures[step*2+i];
				}
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
			}
			check();

			GLES20.glUniform1i(hshimageUniforms[i], i);
			check();
		}

		// Draws a quad (made up of 2 triangles = 4 vertices)                                                                                                                           
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		check();

		GLES20.glDisableVertexAttribArray(vertexAttr);
		check();
		GLES20.glDisableVertexAttribArray(texCoordAttr);
		check();
	}

	public void onDrawFrame(GL10 unused) {
		int texW=256;
		int texH=256;
		// viewport should match texture size
		GLES20.glViewport(0, 0, texW, texH);

		// Add program to OpenGL environment
		GLES20.glUseProgram(mProgram);
		check();

		for(int step=0;step<3;step++) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[step%2]);
			check();
			// specify texture as color attachment
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTex[step%2], 0);
			check();
			int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
			if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				throw new RuntimeException("Invalid framebuffer status: " + status);
			}
	
			// Redraw background color
			GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
			check();
	
			paintTexturedRectangle(step);
		}

		GLES20.glViewport(viewX, viewY, viewWidth, viewHeight);
		// Bind the default framebuffer (to render to the screen) - indicated by '0'
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		check();
		GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
		check();
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		check();
		
		paintTexturedRectangle(3);
	}

	private void check() {
		int e = GLES20.glGetError();
		if (e != 0) {
			String msg;
			switch (e) {
			case GLES20.GL_INVALID_ENUM:
				msg = "GL_INVALID_ENUM";
				break;
			case GLES20.GL_INVALID_VALUE:
				msg = "GL_INVALID_VALUE";
				break;
			case GLES20.GL_INVALID_OPERATION:
				msg = "GL_INVALID_OPERATION";
				break;
			case GLES20.GL_OUT_OF_MEMORY:
				msg = "GL_OUT_OF_MEMORY";
				break;
			default:
				msg = "unknown error";
				break;
			}
			throw new RuntimeException("GL error: " + e + ": " + msg);
		}
	}

	public void onSurfaceChanged(GL10 unused, int width, int height) {
		float r = Math.min(width / (float) rtiwidth, height / (float) rtiheight);
		int w = (int) (r * rtiwidth);
		int h = (int) (r * rtiheight);
		this.viewWidth = w;
		this.viewHeight = h;
		this.viewX = (width - w) / 2;
		this.viewY = (height - h) / 2;
	}
}
