package com.demo.outwindowvideo;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL 方法和流程
 * https://www.daimajiaoliu.com/daima/60cc7ca29954405
 */
public class AlphaVideoRenderer implements GLTextureView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private final @AlphaModel
    int alphaModel;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 4 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 2;


    private FloatBuffer triangleVertices;

    // 左右分布（包含左色彩右黑白、左黑白右色彩）的顶点坐标和纹理坐标，每四个中，前两位为顶点坐标，后两位为纹理坐标
    private static final float[] horizontalVerticesData = {
            -1.0f, 1.0f, 0.5f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.5f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
    };

    // 上下分布（包含上色彩下黑白、上黑白下色彩）的顶点坐标和纹理坐标
    private static final float[] verticalVerticesData = {
            -1.0f, 1.0f, 0.0f, 0.5f,
            1.0f, 1.0f, 1.0f, 0.5f,
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
    };

    private static final String vertexShader = "attribute vec2 a_position;\n"
            + "attribute vec2 a_texCoord;\n"
            + "varying vec2 v_texcoord;\n"
            + "void main(void) {\n"
            + "  gl_Position = vec4(a_position, 0.0, 1.0);\n"
            + "  v_texcoord = a_texCoord;\n"
            + "}\n";

    /**
     * 对应源视频要求为：左彩色、右黑白
     */
    private static final String rightAlphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord + vec2(-0.5, 0)).rgb, texture2D(sTexture, v_texcoord).r);\n"
            + "}\n";


    /**
     * 对应源视频要求为：左黑白、右彩色
     */
    private static final String leftAlphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord).rgb, texture2D(sTexture, v_texcoord + vec2(-0.5, 0)).r);\n"
            + "}\n";

    /**
     * 对应源视频要求为：上彩色、下黑白
     */
    private static final String bottomAlphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord + vec2(0, -0.5)).rgb, texture2D(sTexture, v_texcoord).r);\n"
            + "}\n";


    /**
     * 对应源视频要求为：上黑白、下彩色
     */
    private static final String topAlphaShader = "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "varying vec2 v_texcoord;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "void main() {\n"
            + "  gl_FragColor = vec4(texture2D(sTexture, v_texcoord).rgb, texture2D(sTexture, v_texcoord + vec2(0, -0.5)).r);\n"
            + "}\n";


    private int program;
    private int textureID;
    private int aPositionHandle;
    private int aTextureHandle;

    private SurfaceTexture surface;
    private boolean updateSurface = false;

    private OnSurfacePrepareListener onSurfacePrepareListener;

    public AlphaVideoRenderer(@AlphaModel int alphaModel) {
        this.alphaModel = alphaModel;
        float[] vertices = getAlphaShaderData();

        triangleVertices = ByteBuffer.allocateDirect(vertices.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(vertices).position(0);
    }

    private float[] getAlphaShaderData() {
        if (this.alphaModel == AlphaModel.VIDEO_TRANS_TOP_ALPHA || this.alphaModel == AlphaModel.VIDEO_TRANS_BOTTOM_ALPHA) {
            return verticalVerticesData;
        } else {
            return horizontalVerticesData;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        program = createProgram(vertexShader, getAlphaModel());
        if (program == 0) {
            return;
        }
        aPositionHandle = GLES20.glGetAttribLocation(program, "a_position");
        if (aPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        aTextureHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
        if (aTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        prepareSurface();
    }

    private String getAlphaModel() {
        String model;
        switch (alphaModel) {
            case AlphaModel.VIDEO_TRANS_TOP_ALPHA:
                model = topAlphaShader;
                break;
            case AlphaModel.VIDEO_TRANS_BOTTOM_ALPHA:
                model = bottomAlphaShader;
                break;
            case AlphaModel.VIDEO_TRANS_RIGHT_ALPHA:
                model = rightAlphaShader;
                break;
            default:
                model = leftAlphaShader;
                break;
        }
        return model;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private void prepareSurface() {
        int[] textures = new int[1];
        // 创建纹理
        GLES20.glGenTextures(1, textures, 0);

        textureID = textures[0];
        // 绑定
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);

        // 创建纹理
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surface = new SurfaceTexture(textureID);
        surface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(this.surface);
        onSurfacePrepareListener.surfacePrepared(surface);

        synchronized (this) {
            updateSurface = false;
        }
    }


    @Override
    public void onDrawFrame(GL10 glUnused) {
        synchronized (this) {
            if (updateSurface) {
                surface.updateTexImage();
                updateSurface = false;
            }
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0f, 0.0f, 0.0f, 0.0f);
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(aPositionHandle);

        triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, triangleVertices);
        GLES20.glEnableVertexAttribArray(aTextureHandle);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFinish();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    void setOnSurfacePrepareListener(OnSurfacePrepareListener onSurfacePrepareListener) {
        this.onSurfacePrepareListener = onSurfacePrepareListener;
    }

    interface OnSurfacePrepareListener {
        void surfacePrepared(Surface surface);
    }
}
