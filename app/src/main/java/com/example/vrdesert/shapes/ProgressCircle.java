package com.example.vrdesert.shapes;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ProgressCircle {
    private final FloatBuffer vertexBuffer;
    private static final int SAMPLES = 40;

    public ProgressCircle() {
        float[] vertices = new float[(SAMPLES + 1) * 3];
        for (int i = 0; i <= SAMPLES; i++) {
            float angle = (float) (i * 2 * Math.PI / SAMPLES);
            vertices[i * 3] = (float) Math.cos(angle);
            vertices[i * 3 + 1] = (float) Math.sin(angle);
            vertices[i * 3 + 2] = 0;
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    public void draw(int programId, float[] mvpMatrix, float progress) {
        GLES20.glUseProgram(programId);
        int posHandle = GLES20.glGetAttribLocation(programId, "vPosition");
        GLES20.glEnableVertexAttribArray(posHandle);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        int mvpHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        int colorHandle = GLES20.glGetUniformLocation(programId, "uColor");
        if (colorHandle != -1) {
            GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 1.0f, 0.8f);
        }

        // Draw the circle arc based on progress
        int count = (int) (progress * SAMPLES);
        if (count > 0) {
            GLES20.glLineWidth(5.0f);
            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, count + 1);
        }

        GLES20.glDisableVertexAttribArray(posHandle);
    }
}
