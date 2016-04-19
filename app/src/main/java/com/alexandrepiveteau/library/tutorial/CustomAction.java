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

import android.app.PendingIntent;
import android.support.annotation.DrawableRes;

/**
 * Created by Alexandre on 15.07.2015.
 */
public interface CustomAction {

    /*
     * Since Android doesn't support static methods in interfaces for the moment, we use an inner class.
     */
    class Utils {
        static boolean areCustomActionsDrawingEqual(CustomAction customAction1, CustomAction customAction2) {
            if (customAction1.hasCustomIcon() == customAction2.hasCustomIcon()) {
                if(customAction1.getCustomActionIcon() == customAction2.getCustomActionIcon()) {
                    return true;
                }
            } else {
                if(customAction1.getCustomActionTitle().equals(customAction2.getCustomActionTitle())) {
                    return true;
                }
            }

            return false;
        }
    }

    class Builder {

        private static final int NO_ICON = 0;

        private int mIcon;
        private String mTitle;
        private PendingIntent mPendingIntent;

        public Builder(PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
            mIcon = NO_ICON;
            mTitle = "Custom Action";
        }

        public Builder setIcon(@DrawableRes int iconResource) {
            mIcon = iconResource;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public CustomAction build() {
            return new CustomAction() {
                @Override
                public PendingIntent getCustomActionPendingIntent() {
                    return mPendingIntent;
                }

                @Override
                public int getCustomActionIcon() {
                    return mIcon;
                }

                @Override
                public String getCustomActionTitle() {
                    return mTitle;
                }

                @Override
                public boolean isEnabled() {
                    return mPendingIntent != null;
                }

                @Override
                public boolean hasCustomIcon() {
                    return mIcon != NO_ICON;
                }
            };
        }
    }

    PendingIntent getCustomActionPendingIntent();
    int getCustomActionIcon();
    String getCustomActionTitle();
    boolean isEnabled();
    boolean hasCustomIcon();
}
