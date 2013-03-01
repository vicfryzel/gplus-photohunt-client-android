/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.plus.samples.photohunt.model;

/**
 * Represents the PhotoHunt theme.
 */
public class Theme {

    /**
     * Primary identifier of this Theme.
     */
    public Long id;

    /**
     * Display name of this Theme.
     */
    public String displayName;

    /**
     * Date that this Theme was created.
     */
    public String created;

    /**
     * Date that this Theme should start.
     */
    public String start;

    /**
     * ID of Photo to display as a preview of this Theme.
     */
    public Long previewPhotoId;

}
