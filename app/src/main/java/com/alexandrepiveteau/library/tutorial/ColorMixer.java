/*
 * Copyright 2015 Alexandre Piveteau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alexandrepiveteau.library.tutorial;

/**
 * Created by Alexandre on 24.04.2015.
 */
public class ColorMixer {

    private int mFirstColor;
    private int mSecondColor;

    /**
     * A convenience method to get a mixed color.
     * @param transitionProgression The progression of the mix.
     * @return The mixed color.
     */
    public int getMixedColor(float transitionProgression) {
        if (transitionProgression > 1f) {
            transitionProgression = 1f;
        } else if (transitionProgression < 0f) {
            transitionProgression = 0f;
        }
        float iRatio = 1.0f - transitionProgression;

        int aA = (mFirstColor >> 24 & 0xff);
        int aR = ((mFirstColor & 0xff0000) >> 16);
        int aG = ((mFirstColor & 0xff00) >> 8);
        int aB = (mFirstColor & 0xff);

        int bA = (mSecondColor >> 24 & 0xff);
        int bR = ((mSecondColor & 0xff0000) >> 16);
        int bG = ((mSecondColor & 0xff00) >> 8);
        int bB = (mSecondColor & 0xff);

        int A = (int) ((aA * iRatio) + (bA * transitionProgression));
        int R = (int) ((aR * iRatio) + (bR * transitionProgression));
        int G = (int) ((aG * iRatio) + (bG * transitionProgression));
        int B = (int) ((aB * iRatio) + (bB * transitionProgression));

        return A << 24 | R << 16 | G << 8 | B;
    }

    public void setFirstColor(int firstColor) {
        mFirstColor = firstColor;
    }

    public void setSecondColor(int secondColor) {
        mSecondColor = secondColor;
    }
}
