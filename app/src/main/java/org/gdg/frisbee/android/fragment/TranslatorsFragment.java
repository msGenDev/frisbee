/*
 * Copyright 2013-2015 The GDG Frisbee Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gdg.frisbee.android.fragment;

import org.gdg.frisbee.android.api.model.Contributor;
import org.gdg.frisbee.android.crowdin.model.Translator;


public class TranslatorsFragment extends ContributorsFragment {

    private static final Contributor[] TRANSLATORS = new Contributor[]{
            new Translator("friedger", "https://www.gravatar.com/avatar/72c15c247727ba65f72de3c7c58c4a42?s=170&d=https%3A%2F%2Fcrowdin.com%2Fimages%2Fuser-picture.png", 103),
            new Translator("tasomaniac", "https://www.gravatar.com/avatar/67be1b058c66ed002ff45a1f9a22c0ff?s=150&d=https%3A%2F%2Fcrowdin.com%2Fimages%2Fuser-picture.png", 35),
            new Translator("mauimauer", "https://www.gravatar.com/avatar/b9236795d95774ca2137bb15d54da0a9?s=150&d=https%3A%2F%2Fcrowdin.com%2Fimages%2Fuser-picture.png", 13),
            new Translator("Andrulko", "", 2)
    };

    protected void loadContributors() {
        for (Contributor contributor : TRANSLATORS) {
            mAdapter.add(contributor);
        }
    }
}
