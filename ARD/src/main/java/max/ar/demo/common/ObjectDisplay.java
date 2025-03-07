/*
 * Copyright 2023. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package max.ar.demo.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.view.MotionEvent;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Mtl;
import de.javagl.obj.MtlReader;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjData;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Draw a virtual object based on the specified parameters.
 */
public class ObjectDisplay {
    private static final String TAG = "ObjectDisplay";

    /**
     * Set the default light direction.
     */
    private static final float[] LIGHT_DIRECTIONS = new float[]{0.0f, 1.0f, 0.0f, 0.0f};

    private static final int FLOAT_BYTE_SIZE = 4;

    private static final int INDEX_COUNT_RATIO = 2;

    private static final int MATRIX_SIZE = 16;

    private static final int MAX_OBJ_KIND = 4;

    /**
     * Light direction (x, y, z, w).
     */
    private float[] mViewLightDirections = new float[4];

    private int []mTexCoordsBaseAddress = new int[MAX_OBJ_KIND];

    private int []mNormalsBaseAddress = new int[MAX_OBJ_KIND];

    private int []mVertexBufferId = new int[MAX_OBJ_KIND];

    private int mIndexCount;

    private int mGlProgram;

    private int mGlProgram2;

    private int []mIndexBufferId = new int[MAX_OBJ_KIND];

    private int[] mTextures = new int[MAX_OBJ_KIND];

    private int mModelViewUniform;

    private int mModelViewProjectionUniform;

    private int mPositionAttribute;

    private int mNormalAttribute;

    private int mTexCoordAttribute;

    private int mTextureUniform;

    private int mLightingParametersUniform;

    private int mColorUniform;

    private float[] mModelMatrixs = new float[MATRIX_SIZE];

    private float[] mModelViewMatrixs = new float[MATRIX_SIZE];
    private float[] mModelViewMatrixs2 = new float[MATRIX_SIZE];
    private float[] mModelViewProjectionMatrixs = new float[MATRIX_SIZE];

    /**
     * The largest bounding box of a virtual object, represented by two diagonals of a cube.
     */
    private float[] mBoundingBoxs = new float[6];

    private float mWidth;

    private float mHeight;

    private ObjectData[] objectDatas = new ObjectData[MAX_OBJ_KIND];

    private int mPositionAttribute2;
    private int mNormalAttribute2;
    private int mTexCoordAttribute2;
    private int mTextureUniform2;
    private int mLightingParametersUniform2;
    private int mColorUniform2;
    private int mModelViewUniform2;
    private int mModelViewProjectionUniform2;
    private float[] mModelMatrixs2 = new float[MATRIX_SIZE];

    private int mKai;
    private int mKdi;
    private int mKsi;

    private String[][] mtlNames = new String[MAX_OBJ_KIND][];
    public ObjectDisplay() {
    }

    /**
     * If the surface size is changed, update the changed size of the record synchronously.
     *
     * @param width Surface's width.
     * @param height Surface's height.
     */
    public void setSize(float width, float height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Create a shader program to read the data of the virtual object.
     *
     * @param context Context.
     */
    public void init(Context context) {
        ShaderUtil.checkGlError(TAG, "Init start.");
        createProgram();
        createProgram2();

        // Coordinate and index.
        int[] buffers = new int[2*MAX_OBJ_KIND];
        GLES20.glGenBuffers(2*MAX_OBJ_KIND, IntBuffer.wrap(buffers));

        for(int k=0;k<MAX_OBJ_KIND;k++){
            mVertexBufferId[k] = buffers[(k+1)*2-2];
            mIndexBufferId[k] = buffers[(k+1)*2-1];
        }
        initGlTextureData(context);
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create program start.");
        mGlProgram = WorldShaderUtil.getObjectProgram();
        mModelViewUniform = GLES20.glGetUniformLocation(mGlProgram, "inViewMatrix");
        mModelViewProjectionUniform = GLES20.glGetUniformLocation(mGlProgram, "inMVPMatrix");
        mPositionAttribute = GLES20.glGetAttribLocation(mGlProgram, "inObjectPosition");
        mNormalAttribute = GLES20.glGetAttribLocation(mGlProgram, "inObjectNormalVector");
        mTexCoordAttribute = GLES20.glGetAttribLocation(mGlProgram, "inTexCoordinate");
        mTextureUniform = GLES20.glGetUniformLocation(mGlProgram, "inObjectTexture");
        mLightingParametersUniform = GLES20.glGetUniformLocation(mGlProgram, "inLight");
        mColorUniform = GLES20.glGetUniformLocation(mGlProgram, "inObjectColor");
        Matrix.setIdentityM(mModelMatrixs, 0);
        ShaderUtil.checkGlError(TAG, "Create program end.");
    }

    private void createProgram2(){
        ShaderUtil.checkGlError(TAG, "Create program2 start.");
        mGlProgram2 = WorldShaderUtil.getMtlProgram();
        mModelViewUniform2 = GLES20.glGetUniformLocation(mGlProgram2, "inViewMatrix");
        mModelViewProjectionUniform2 = GLES20.glGetUniformLocation(mGlProgram2, "inMVPMatrix");
        mPositionAttribute2 = GLES20.glGetAttribLocation(mGlProgram2, "inObjectPosition");
        mNormalAttribute2 = GLES20.glGetAttribLocation(mGlProgram2, "inObjectNormalVector");
        mTexCoordAttribute2 = GLES20.glGetAttribLocation(mGlProgram2, "inTexCoordinate");
        mTextureUniform2 = GLES20.glGetUniformLocation(mGlProgram2, "inObjectTexture");
        mKai = GLES20.glGetUniformLocation(mGlProgram2,"vKa");
        mKdi = GLES20.glGetUniformLocation(mGlProgram2,"vKd");
        mKsi = GLES20.glGetUniformLocation(mGlProgram2,"vKs");
        Matrix.setIdentityM(mModelMatrixs, 0);
        ShaderUtil.checkGlError(TAG, "Create program2 end.");
    }

    private void initGlTextureData(Context context) {
        GLES20.glGenTextures(mTextures.length, mTextures, 0);

        for(int k=0;k<MAX_OBJ_KIND; k++){
            ShaderUtil.checkGlError(TAG, "Init gl texture data start.");
            bindGlTexture(context, k);
            ShaderUtil.checkGlError(TAG, "Init gl texture data end.");
            initializeGlObjectData(context, k);
        }
    }

    private void initializeObjAndMtlData(Context context, int k){
        ObjectData objectData = null;
        Optional<ObjectData> objectDataOptional = readObjectAndMtl(context,k);
        if (objectDataOptional.isPresent()) {
            objectData = objectDataOptional.get();
        } else {
            LogUtil.error(TAG, "Read object error.");
            return;
        }
        objectDatas[k]=objectData;

        mTexCoordsBaseAddress[k] = FLOAT_BYTE_SIZE * objectData.mObjectIndices.limit();
        mNormalsBaseAddress[k] = mTexCoordsBaseAddress[k] + FLOAT_BYTE_SIZE * objectData.mTexCoords.limit();
        final int totalBytes = mNormalsBaseAddress[k] + FLOAT_BYTE_SIZE * objectData.mNormals.limit();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId[k]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,
                FLOAT_BYTE_SIZE * objectData.mObjectVertices.limit(), objectData.mObjectVertices);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress[k],
                FLOAT_BYTE_SIZE * objectData.mTexCoords.limit(), objectData.mTexCoords);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress[k],
                FLOAT_BYTE_SIZE * objectData.mNormals.limit(), objectData.mNormals);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId[k]);
        mIndexCount = objectData.mIndices.limit();
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, INDEX_COUNT_RATIO * mIndexCount,
                objectData.mIndices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "obj buffer load");
    }

    private Optional<ObjectData> readObjectAndMtl(Context context, int k){
        Obj obj;
        String s;
        List<Mtl> mtl_list;
        List<String> mtl_string_list;
        Map<String, Mtl> mtls;

        switch (k){
            case 3:{s="Cottage.obj"; break;}
            default: s = "AR_logo.obj";
        }
        try (InputStream objInputStream = context.getAssets().open(s)) {
            obj = ObjReader.read(objInputStream);
            obj = ObjUtils.convertToRenderable(obj);
            mtl_string_list = obj.getMtlFileNames();

        } catch (IllegalArgumentException | IOException e) {
            LogUtil.error(TAG, "Get data failed!");
            return Optional.empty();
        }

        /*Iterator<String> ite = mtl_string_list.listIterator();
        while(ite.hasNext()){
            try (InputStream mtlInputStream =  context.getAssets().open(ite.next()))
            {
                List<Mtl> mtlList = MtlReader.read(mtlInputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }*/
        try (InputStream mtlInputStream =  context.getAssets().open(mtl_string_list.get(0)))
        {
            mtl_list = MtlReader.read(mtlInputStream);
            mtls = mtl_list.stream().collect(
                    LinkedHashMap::new,
                    (map,mtl) -> map.put(mtl.getName(),mtl),
                    (map0,map1) -> map0.putAll(map1));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Every surface of an object has three vertices.
        IntBuffer objectIndices = ObjData.getFaceVertexIndices(obj, 3);
        FloatBuffer objectVertices = ObjData.getVertices(obj);

        calculateBoundingBox(objectVertices);

        // Size of the allocated buffer.
        ShortBuffer indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        while (objectIndices.hasRemaining()) {
            indices.put((short) objectIndices.get());
        }
        indices.rewind();

        // The dimension of the texture coordinate is 2.
        FloatBuffer texCoordinates = ObjData.getTexCoords(obj, 2);
        FloatBuffer normals = ObjData.getNormals(obj);

        return Optional.of(new ObjectData(objectIndices, objectVertices, indices, texCoordinates, normals,mtls));
    }

    private void bindGlTexture(Context context, int k){
        Bitmap textureBitmap;
        String name;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + k);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[k]);

        switch (k){
            case 0:{name = "AR_logo.png";
                break;
            }
            case 1:{name = "cup.bmp";
                break;
            }
            case 2:{name = "bulb.bmp";
                break;
            }
            case 3:{name = "Ball.jpg";
                break;
            }
            default:{name = "AR_logo.png";
            }
        }
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        try (InputStream inputStream = context.getAssets().open(name)) {
            textureBitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IllegalArgumentException | IOException exception) {
            LogUtil.error(TAG, "Get texture data error!");
            return;
        }
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void initializeGlObjectData(Context context,int k) {

        ObjectData objectData = null;
        Optional<ObjectData> objectDataOptional = readObject(context,k);
        if (objectDataOptional.isPresent()) {
            objectData = objectDataOptional.get();
        } else {
            LogUtil.error(TAG, "Read object error.");
            return;
        }
        objectDatas[k]=objectData;

        mTexCoordsBaseAddress[k] = FLOAT_BYTE_SIZE * objectData.mObjectIndices.limit();
        mNormalsBaseAddress[k] = mTexCoordsBaseAddress[k] + FLOAT_BYTE_SIZE * objectData.mTexCoords.limit();
        final int totalBytes = mNormalsBaseAddress[k] + FLOAT_BYTE_SIZE * objectData.mNormals.limit();
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId[k]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, totalBytes, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,
                FLOAT_BYTE_SIZE * objectData.mObjectVertices.limit(), objectData.mObjectVertices);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mTexCoordsBaseAddress[k],
                FLOAT_BYTE_SIZE * objectData.mTexCoords.limit(), objectData.mTexCoords);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, mNormalsBaseAddress[k],
                FLOAT_BYTE_SIZE * objectData.mNormals.limit(), objectData.mNormals);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId[k]);
        mIndexCount = objectData.mIndices.limit();
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, INDEX_COUNT_RATIO * mIndexCount,
                objectData.mIndices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "obj buffer load");
    }

    private Optional<ObjectData> readObject(Context context, int k) {
        Obj obj;
        String s;
        List<Mtl> mtl_list = null;

        switch (k){
            case 0:{s="AR_logo.obj"; break;}
            case 1:{s="Cup.obj"; break;}
            case 2:{s="bulb.obj"; break;}
            case 3:{s="Ball.obj"; break;}
            default: s = "AR_logo.obj";
        }
        try (InputStream objInputStream = context.getAssets().open(s)) {
            obj = ObjReader.read(objInputStream);
            obj = ObjUtils.convertToRenderable(obj);
        } catch (IllegalArgumentException | IOException e) {
            LogUtil.error(TAG, "Get data failed!");
            return Optional.empty();
        }

        // Every surface of an object has three vertices.
        IntBuffer objectIndices = ObjData.getFaceVertexIndices(obj, 3);
        FloatBuffer objectVertices = ObjData.getVertices(obj);

        calculateBoundingBox(objectVertices);

        // Size of the allocated buffer.
        ShortBuffer indices = ByteBuffer.allocateDirect(2 * objectIndices.limit())
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        while (objectIndices.hasRemaining()) {
            indices.put((short) objectIndices.get());
        }
        indices.rewind();

        // The dimension of the texture coordinate is 2.
        FloatBuffer texCoordinates = ObjData.getTexCoords(obj, 2);
        FloatBuffer normals = ObjData.getNormals(obj);

        return Optional.of(new ObjectData(objectIndices, objectVertices, indices, texCoordinates, normals));
    }

    /**
     * The virtual object data class.
     */
    private static class ObjectData {
        private IntBuffer mObjectIndices;

        private FloatBuffer mObjectVertices;

        private ShortBuffer mIndices;

        private FloatBuffer mTexCoords;

        private FloatBuffer mNormals;

        private Map<String, Mtl>mtls;

        ObjectData(IntBuffer objectIndices, FloatBuffer objectVertices, ShortBuffer indices, FloatBuffer texCoords,
                   FloatBuffer normals) {
            this.mObjectIndices = objectIndices;
            this.mObjectVertices = objectVertices;
            this.mIndices = indices;
            this.mTexCoords = texCoords;
            this.mNormals = normals;
            this.mtls = null;
        }

        ObjectData(IntBuffer objectIndices, FloatBuffer objectVertices, ShortBuffer indices, FloatBuffer texCoords,
                   FloatBuffer normals, Map<String, Mtl> mtls){
            this.mObjectIndices = objectIndices;
            this.mObjectVertices = objectVertices;
            this.mIndices = indices;
            this.mTexCoords = texCoords;
            this.mNormals = normals;
            this.mtls = mtls;
        }
    }

    /**
     * Draw a virtual object at a specific location on a specified plane.
     *
     * @param cameraView The viewMatrix is a 4 * 4 matrix.
     * @param cameraProjection The ProjectionMatrix is a 4 * 4 matrix.
     * @param lightIntensity The lighting intensity.
     * @param obj The virtual object.
     */
    public void onDrawFrame(float[] cameraView, float[] cameraProjection, float lightIntensity, VirtualObject obj) {
        ShaderUtil.checkGlError(TAG, "onDrawFrame start.");
        int k;
        switch (obj.getObjname()){
            case "AR_logo":{
                k=0;
                break;}
            case "Cup":{
                k=1;
                break;}
            case "bulb":{
                k=2;
                break;}
            case "Ball":{
                k=3;
                break;}
            default:
                k=0;
        }
        if(k>5){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            mModelMatrixs = obj.getModelArPoseMatrix();
            Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
            Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraProjection, 0, mModelViewMatrixs, 0);
            GLES20.glUseProgram(mGlProgram2);
            FloatTuple mKa[] = new FloatTuple[9];
            FloatTuple mKs[] = new FloatTuple[9];
            FloatTuple mKd[] = new FloatTuple[9];
            for(int i = 0;i<1;i++){
                String name = "a";
                mKa[i]= objectDatas[k].mtls.get(name).getKa();
                mKs[i]= objectDatas[k].mtls.get(name).getKs();
                mKd[i]= objectDatas[k].mtls.get(name).getKd();
            }
            GLES20.glUniform3f(
                    mKai,mKa[0].getX(),mKa[0].getY(),mKa[0].getZ());
            GLES20.glUniform3f(
                    mKsi,mKs[0].getX(),mKs[0].getY(),mKs[0].getZ());
            GLES20.glUniform3f(
                    mKdi,mKd[0].getX(),mKd[0].getY(),mKd[0].getZ());

            drawObject2(k);
            ShaderUtil.checkGlError(TAG, "onDrawFrame end.");
        }
        else {
            mModelMatrixs = obj.getModelArPoseMatrix();
            Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
            Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraProjection, 0, mModelViewMatrixs, 0);
            GLES20.glUseProgram(mGlProgram);
            Matrix.multiplyMV(mViewLightDirections, 0, mModelViewMatrixs, 0, LIGHT_DIRECTIONS, 0);
            MatrixUtil.normalizeVec3(mViewLightDirections);
            //System.out.println(mViewLightDirections);

            // Light direction.
            GLES20.glUniform4f(mLightingParametersUniform,
                    mViewLightDirections[0], mViewLightDirections[1], mViewLightDirections[2], lightIntensity);

            /*GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
*/
            float[] objColors;
            objColors = obj.getColor();
            objColors[3] = 30f;
            GLES20.glUniform4fv(mColorUniform, 1, objColors, 0);
            drawObject(k);
            ShaderUtil.checkGlError(TAG, "onDrawFrame end.");
        }
    }

    private void drawObject2(int k){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[k]);
        GLES20.glUniform1i(mTextureUniform2, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId[k]);

        // The coordinate dimension of the read virtual object is 3.
        GLES20.glVertexAttribPointer(
                mPositionAttribute2, 3, GLES20.GL_FLOAT, false, 0, 0);
        // The dimension of the normal vector is 3.
        GLES20.glVertexAttribPointer(
                mNormalAttribute2, 3, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress[k]);
        // The dimension of the texture coordinate is 2.
        GLES20.glVertexAttribPointer(
                mTexCoordAttribute2, 2, GLES20.GL_FLOAT, false, 0, mTexCoordsBaseAddress[k]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glUniformMatrix4fv(
                mModelViewUniform2, 1, false, mModelViewMatrixs, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform2, 1, false, mModelViewProjectionMatrixs, 0);

        GLES20.glEnableVertexAttribArray(mPositionAttribute2);
        GLES20.glEnableVertexAttribArray(mNormalAttribute2);
        GLES20.glEnableVertexAttribArray(mTexCoordAttribute2);

        ShaderUtil.checkGlError(TAG, "onDrawFrame glBindBuffer");
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId[k]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, objectDatas[k].mIndices.limit(), GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "onDrawFrame glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mPositionAttribute2);
        GLES20.glDisableVertexAttribArray(mNormalAttribute2);
        GLES20.glDisableVertexAttribArray(mTexCoordAttribute2);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    private void drawObject(int k){
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + k);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[k]);
        GLES20.glUniform1i(mTextureUniform, k);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferId[k]);

        // The coordinate dimension of the read virtual object is 3.
        GLES20.glVertexAttribPointer(
                mPositionAttribute, 3, GLES20.GL_FLOAT, false, 0, 0);
        // The dimension of the normal vector is 3.
        GLES20.glVertexAttribPointer(
                mNormalAttribute, 3, GLES20.GL_FLOAT, false, 0, mNormalsBaseAddress[k]);
        // The dimension of the texture coordinate is 2.
        GLES20.glVertexAttribPointer(
                mTexCoordAttribute, 2, GLES20.GL_FLOAT, false, 0, mTexCoordsBaseAddress[k]);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glUniformMatrix4fv(
                mModelViewUniform, 1, false, mModelViewMatrixs, 0);
        GLES20.glUniformMatrix4fv(
                mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrixs, 0);

        GLES20.glEnableVertexAttribArray(mPositionAttribute);
        GLES20.glEnableVertexAttribArray(mNormalAttribute);
        GLES20.glEnableVertexAttribArray(mTexCoordAttribute);

        ShaderUtil.checkGlError(TAG, "onDrawFrame glBindBuffer");
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferId[k]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, objectDatas[k].mIndices.limit(), GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "onDrawFrame glDisableVertexAttribArray");
        GLES20.glDisableVertexAttribArray(mPositionAttribute);
        GLES20.glDisableVertexAttribArray(mNormalAttribute);
        GLES20.glDisableVertexAttribArray(mTexCoordAttribute);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Check whether the virtual object is clicked.
     *
     * @param cameraView The viewMatrix 4 * 4.
     * @param cameraPerspective The ProjectionMatrix 4 * 4.
     * @param obj The virtual object data.
     * @param event The gesture event.
     * @return Return the click result for determining whether the input virtual object is clicked
     */
    public boolean hitTest(float[] cameraView, float[] cameraPerspective, VirtualObject obj, MotionEvent event) {
        mModelMatrixs = obj.getModelArPoseMatrix();
        Matrix.multiplyMM(mModelViewMatrixs, 0, cameraView, 0, mModelMatrixs, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrixs, 0, cameraPerspective, 0, mModelViewMatrixs, 0);

        // Calculate the coordinates of the smallest bounding box in the coordinate system of the device screen.
        float[] screenPos = calculateScreenPos(mBoundingBoxs[0], mBoundingBoxs[1], mBoundingBoxs[2]);

        // Record the largest bounding rectangle of an object (minX/minY/maxX/maxY).
        float[] boundarys = new float[4];
        boundarys[0] = screenPos[0];
        boundarys[1] = screenPos[0];
        boundarys[2] = screenPos[1];
        boundarys[3] = screenPos[1];

        // Determine whether a screen position corresponding to (maxX, maxY, maxZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{3, 4, 5});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (minX, minY, maxZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{0, 1, 5});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (minX, maxY, minZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{0, 4, 2});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (minX, maxY, maxZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{0, 4, 5});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (maxX, minY, minZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{3, 1, 2});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (maxX, minY, maxZ) is clicked.
        boundarys = findMaximum(boundarys, new int[]{3, 1, 5});
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }

        // Determine whether a screen position corresponding to (maxX, maxY, maxZ) is clicked.
        if (determineWhetherToClick(event, boundarys)) {
            return true;
        }
        return false;
    }

    /**
     * Determine whether the click happens within the valid boundary.
     *
     * @param event The gesture event.
     * @param boundarys Record the largest bounding rectangle of an object (minX/minY/maxX/maxY).
     * @return Determine whether the corresponding screen position is tapped.
     */
    private boolean determineWhetherToClick(MotionEvent event, float[] boundarys) {
        // The size of the click boundary rectangle array is 4.
        if (event == null || boundarys.length < 4) {
            return false;
        }
        // Determine whether the click point is within the valid boundary.
        if (((event.getX() > boundarys[0]) && (event.getX() < boundarys[1]))
                && ((event.getY() > boundarys[2]) && (event.getY() < boundarys[3]))) {
            return true;
        }
        return false;
    }

    /**
     * Obtain the AABB bounding box of a virtual object.
     *
     * @return AABB bounding box data (minX, minY, minZ, maxX, maxY, maxZ).
     */
    public float[] getBoundingBox() {
        return Arrays.copyOf(mBoundingBoxs, mBoundingBoxs.length);
    }

    // The size of minXmaxXminYmaxY is 4, and the size of index is 3.
    private float[] findMaximum(float[] minXmaxXminYmaxY, int[] index) {
        float[] screenPos = calculateScreenPos(mBoundingBoxs[index[0]],
                mBoundingBoxs[index[1]], mBoundingBoxs[index[2]]);
        if (screenPos[0] < minXmaxXminYmaxY[0]) {
            minXmaxXminYmaxY[0] = screenPos[0];
        }
        if (screenPos[0] > minXmaxXminYmaxY[1]) {
            minXmaxXminYmaxY[1] = screenPos[0];
        }
        if (screenPos[1] < minXmaxXminYmaxY[2]) {
            minXmaxXminYmaxY[2] = screenPos[1];
        }
        if (screenPos[1] > minXmaxXminYmaxY[3]) {
            minXmaxXminYmaxY[3] = screenPos[1];
        }
        return minXmaxXminYmaxY;
    }

    // Convert the input coordinates to the plane coordinate system.
    private float[] calculateScreenPos(float coordinateX, float coordinateY, float coordinateZ) {
        // The coordinates of the point are four-dimensional (x, y, z, w).
        float[] vecs = new float[4];
        vecs[0] = coordinateX;
        vecs[1] = coordinateY;
        vecs[2] = coordinateZ;
        vecs[3] = 1.0f;

        // Store the coordinate values in the clip coordinate system.
        float[] rets = new float[4];
        Matrix.multiplyMV(rets, 0, mModelViewProjectionMatrixs, 0, vecs, 0);

        // Divide by the w component of the coordinates.
        rets[0] /= rets[3];
        rets[1] /= rets[3];
        rets[2] /= rets[3];

        // In the current coordinate system, left is negative, right is positive, downward
        // is positive, and upward is negative.Adding 1 to the left of the X coordinate is
        // equivalent to moving the coordinate system leftwards. Such an operation on the Y
        // axis is equivalent to moving the coordinate system upwards.
        rets[0] += 1.0f;
        rets[1] = 1.0f - rets[1];

        // Convert to pixel coordinates.
        rets[0] *= mWidth;
        rets[1] *= mHeight;

        // When the w component is set to 1, the xy component caused by coordinate system
        // movement is eliminated and doubled.
        rets[3] = 1.0f;
        rets[0] /= 2.0f;
        rets[1] /= 2.0f;
        return rets;
    }

    // Bounding box [minX, minY, minZ, maxX, maxY, maxZ].
    private void calculateBoundingBox(FloatBuffer vertices) {
        if (vertices.limit() < 3) {
            mBoundingBoxs[0] = 0.0f;
            mBoundingBoxs[1] = 0.0f;
            mBoundingBoxs[2] = 0.0f;
            mBoundingBoxs[3] = 0.0f;
            mBoundingBoxs[4] = 0.0f;
            mBoundingBoxs[5] = 0.0f;
            return;
        } else {
            mBoundingBoxs[0] = vertices.get(0);
            mBoundingBoxs[1] = vertices.get(1);
            mBoundingBoxs[2] = vertices.get(2);
            mBoundingBoxs[3] = vertices.get(0);
            mBoundingBoxs[4] = vertices.get(1);
            mBoundingBoxs[5] = vertices.get(2);
        }

        // Use the first three pairs as the initial variables and get the three
        // maximum values and three minimum values.
        int index = 3;
        while (index < vertices.limit() - 2) {
            if (vertices.get(index) < mBoundingBoxs[0]) {
                mBoundingBoxs[0] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[3]) {
                mBoundingBoxs[3] = vertices.get(index);
            }
            index++;

            if (vertices.get(index) < mBoundingBoxs[1]) {
                mBoundingBoxs[1] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[4]) {
                mBoundingBoxs[4] = vertices.get(index);
            }
            index++;

            if (vertices.get(index) < mBoundingBoxs[2]) {
                mBoundingBoxs[2] = vertices.get(index);
            }
            if (vertices.get(index) > mBoundingBoxs[5]) {
                mBoundingBoxs[5] = vertices.get(index);
            }
            index++;
        }
    }
}