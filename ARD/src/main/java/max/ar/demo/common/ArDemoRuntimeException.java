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
 * The exception class when the AR sample code runs. Sometimes,
 * you need to capture exceptions to keep your program running.
 */
public class ArDemoRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 6086295255946102629L;

    /**
     * Constructor.
     */
    public ArDemoRuntimeException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param message message
     */
    public ArDemoRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message message
     * @param cause cause
     */
    public ArDemoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}