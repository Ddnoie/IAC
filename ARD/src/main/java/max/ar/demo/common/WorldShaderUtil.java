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

/**
 * This class provides code and program for the rendering shader related to the world scene.
 */
public class WorldShaderUtil {
    private static final String LS = System.lineSeparator();

    private static final String OBJECT_VERTEX =
        "uniform mat4 inMVPMatrix;" + LS
        + "uniform mat4 inViewMatrix;" + LS
        + "attribute vec3 inObjectNormalVector;" + LS
        + "attribute vec4 inObjectPosition;" + LS
        + "attribute vec2 inTexCoordinate;" + LS
        + "varying vec3 varCameraNormalVector;" + LS
        + "varying vec2 varTexCoordinate;" + LS
        + "varying vec3 varCameraPos;" + LS
        + "void main() {" + LS
        + "    gl_Position = inMVPMatrix * inObjectPosition;" + LS
        + "    varCameraNormalVector = (inViewMatrix * vec4(inObjectNormalVector, 0.0)).xyz;" + LS
        + "    varTexCoordinate = inTexCoordinate;" + LS
        + "    varCameraPos = (inViewMatrix * inObjectPosition).xyz;" + LS
        + "}";

    private static final String OBJECT_FRAGMENT =
        "precision mediump float;" + LS
        + " uniform vec4 inLight;" + LS
        + "uniform vec4 inObjectColor;" + LS
        + "uniform sampler2D inObjectTexture;" + LS
        + "varying vec3 varCameraPos;" + LS
        + "varying vec3 varCameraNormalVector;" + LS
        + "varying vec2 varTexCoordinate;" + LS
        + "void main() {" + LS
        + "    vec4 objectColor;" + LS
                +" vec4 texColor = texture2D(inObjectTexture, varTexCoordinate);" +LS
        + "    objectColor = inObjectColor/ 255.0;" + LS
                + "    objectColor.rgb = objectColor.rgb *objectColor.a + (1.0-objectColor.a) * texColor.rgb;" + LS
        + "    vec3 viewNormal = normalize(varCameraNormalVector);" + LS
        + "    vec3 reflectedLightDirection = reflect(inLight.xyz, viewNormal);" + LS
        + "    vec3 normalCameraPos = normalize(varCameraPos);" + LS
        + "    float specularStrength = max(0.0, dot(normalCameraPos, reflectedLightDirection));" + LS
        + "    gl_FragColor.a = 1.0;" + LS
        + "    float diffuse = inLight.w * 3.5 *" + LS
        + "        0.5 * (dot(viewNormal, inLight.xyz) + 1.0);" + LS
        + "    float specular = inLight.w *" + LS
        + "        pow(specularStrength, 6.0);" + LS
        + "    gl_FragColor.rgb = objectColor.rgb * + diffuse + specular;" + LS
        + "}";

    private static final String POINTCLOUD_VERTEX =
        "uniform mat4 u_ModelViewProjection;" + LS
            + "uniform vec4 u_Color;" + LS
            + "uniform float u_PointSize;" + LS
            + "attribute vec4 a_Position;" + LS
            + "varying vec4 v_Color;" + LS
            + "void main() {" + LS
            + "   v_Color = u_Color;" + LS
            + "   gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);" + LS
            + "   gl_PointSize = u_PointSize;" + LS
            + "}";


    private static final String POINTCLOUD_FRAGMENT =
        "precision mediump float;" + LS
            + "varying vec4 v_Color;" + LS
            + "void main() {" + LS
            + "    gl_FragColor = v_Color;" + LS
            + "}";

    private static final String MTL_VERTEX=
            "uniform mat4 inMVPMatrix;" + LS
                    + "uniform mat4 inViewMatrix;" + LS
                    + "attribute vec3 inObjectNormalVector;" + LS
                    + "attribute vec4 inObjectPosition;" + LS
                    + "attribute vec2 inTexCoordinate;" + LS
                    + " uniform vec4 inLight;" + LS
                    + "varying vec3 varCameraNormalVector;" + LS
                    + "varying vec2 varTexCoordinate;" + LS
                    + "varying vec3 varCameraPos;" + LS
                    + "uniform vec3 vKa;" + LS
                    + "uniform vec3 vKd;" + LS
                    + "uniform vec3 vKs;" + LS
                    + "varying vec4 vDiffuse;" + LS          //用于传递给片元着色器的散射光最终强度
                    + "varying vec4 vAmbient;" + LS          //用于传递给片元着色器的环境光最终强度
                    + "varying vec4 vSpecular;" + LS          //用于传递给片元着色器的镜面光最终强度
                    + "void main() {" + LS
                    + "float shininess=10.0; " + LS
                    + "    gl_Position = inMVPMatrix * inObjectPosition;" + LS
                    + "    varCameraNormalVector = (inViewMatrix * vec4(inObjectNormalVector, 0.0)).xyz;" + LS
                    + "    varTexCoordinate = inTexCoordinate;" + LS
                    + "    varCameraPos = (inViewMatrix * inObjectPosition).xyz;" + LS
                    + "vDiffuse=vec4(vKd,1.0);" + LS
                    + "vSpecular=vec4(vKs,1.0);" + LS
                    + "vAmbient=vec4(vKa,1.0);" + LS
                    + "}";
            ;
    private static final String MTL_FRAGMENT=
            "precision mediump float;" + LS
                    + "uniform sampler2D inObjectTexture;" + LS
                    + "varying vec2 varTexCoordinate;" + LS
                    + "varying vec4 vDiffuse;" + LS          //接收从顶点着色器过来的散射光分量
                    + "varying vec4 vAmbient;" + LS         //接收传递给片元着色器的环境光分量
                    + "varying vec4 vSpecular;" + LS        //接收传递给片元着色器的镜面光分量
                    + "void main() {" + LS
                    + "vec4 finalColor=texture2D(inObjectTexture,varTexCoordinate);" + LS
                    + " gl_FragColor=finalColor*vAmbient+finalColor*vSpecular+finalColor*vDiffuse;" + LS
                    + "}";


    /**
     * Shader label program generator.
     *
     * @return int Program handle.
     */
    public static int getLabelProgram() {
        return ShaderUtil.createGlProgram(ShaderUtil.LABEL_VERTEX, ShaderUtil.LABEL_FRAGMENT);
    }

    /**
     * Shader point cloud program generator.
     *
     * @return int Program handle.
     */
    public static int getPointCloudProgram() {
        return ShaderUtil.createGlProgram(POINTCLOUD_VERTEX, POINTCLOUD_FRAGMENT);
    }

    /**
     * Shader object program generator.
     *
     * @return int Program handle.
     */
    protected static int getObjectProgram() {
        return ShaderUtil.createGlProgram(OBJECT_VERTEX, OBJECT_FRAGMENT);
    }

    public static int getMtlProgram(){
        return  ShaderUtil.createGlProgram(MTL_VERTEX, MTL_FRAGMENT);
    }
}