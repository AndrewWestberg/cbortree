/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.iot.cbor;

/** Unchecked exception for CborTree-specific runtime errors. */
public class CborRuntimeException extends RuntimeException {
    /**
     * Constructs a new {@link CborRuntimeException}.
     */
    public CborRuntimeException() {}

    /**
     * Constructs a new {@link CborRuntimeException} with the given explanation.
     * @param explain the explanation
     */
    public CborRuntimeException(String explain) {
        super(explain);
    }

    /**
     * Constructs a new {@link CborRuntimeException} with the given explanation and cause.
     * @param explain the explanation
     * @param t the cause
     */
    public CborRuntimeException(String explain, Throwable t) {
        super(explain, t);
    }

    /**
     * Constructs a new {@link CborRuntimeException} with the given cause.
     * @param t the cause
     */
    public CborRuntimeException(Throwable t) {
        super(t);
    }
}
