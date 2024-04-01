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

/**
 * Checked exception superclass for CborTree-specific exceptions.
 *
 * @see CborParseException
 * @see CborConversionException
 */
@SuppressWarnings("WeakerAccess")
public class CborException extends Exception {
    /**
     * Constructs a new {@link CborException}.
     */
    public CborException() {}

    /**
     * Constructs a new {@link CborException} with the given explanation.
     * @param explain the explanation
     */
    public CborException(String explain) {
        super(explain);
    }

    /**
     * Constructs a new {@link CborException} with the given explanation and cause.
     * @param explain the explanation
     * @param t the cause
     */
    @SuppressWarnings("WeakerAccess")
    public CborException(String explain, Throwable t) {
        super(explain, t);
    }

    /**
     * Constructs a new {@link CborException} with the given cause.
     * @param t the cause
     */
    public CborException(Throwable t) {
        super(t);
    }
}
