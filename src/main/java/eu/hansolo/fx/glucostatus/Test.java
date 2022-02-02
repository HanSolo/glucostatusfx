/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Gerrit Grunwald.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.glucostatus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


public class Test extends Application {
    private ThirtyDayView thirtyDayView;

    @Override public void init() {
        thirtyDayView = new ThirtyDayView();
    }

    @Override public void start(final Stage stage) {
        StackPane pane = new StackPane(thirtyDayView);
        pane.setPadding(new Insets(10));

        Scene scene = new Scene(pane);
        
        stage.setTitle("Last 30 days");
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();

        thirtyDayView.setDark(true);
    }

    @Override public void stop() {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
