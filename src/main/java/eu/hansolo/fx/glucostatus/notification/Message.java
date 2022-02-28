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

package eu.hansolo.fx.glucostatus.notification;

import com.gluonhq.attach.localnotifications.LocalNotificationsService;
import com.gluonhq.attach.localnotifications.Notification;
import com.gluonhq.charm.glisten.control.Alert;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.ZonedDateTime;
import java.util.UUID;


public class Message {
    public static final Image  INFO_ICON    = new Image(Notifier.class.getResourceAsStream("info.png"));
    public static final Image  WARNING_ICON = new Image(Notifier.class.getResourceAsStream("warning.png"));
    public static final Image  SUCCESS_ICON = new Image(Notifier.class.getResourceAsStream("success.png"));
    public static final Image  ERROR_ICON   = new Image(Notifier.class.getResourceAsStream("error.png"));
    public final        String title;
    public final        String message;
    public final        Image  image;


    // ******************** Constructors **************************************
    public Message(final String title, final String message) {
        this(title, message, null);
    }
    public Message(final String message, final Image image) {
        this("", message, image);
    }
    public Message(final String title, final String message, final Image image) {
        this.title   = title;
        this.message = message;
        this.image   = image;
    }

    
    // ******************** Inner Classes *************************************
    public enum Notifier {
        INSTANCE;

        private static final double ICON_WIDTH  = 46;
        private static final double ICON_HEIGHT = 46;


        // ******************** Constructors **************************************
        Notifier() {}


        // ******************** Methods *******************************************
        /**
         * Show the given Notification on the screen
         * @param notification
         */
        public void notify(final Message notification) {
            showNotification(notification);
        }

        /**
         * Show a Notification with the given parameters on the screen
         * @param title
         * @param message
         * @param image
         */
        public void notify(final String title, final String message, final Image image) {
            notify(new Message(title, message, image));
        }

        /**
         * Show a Notification with the given title and message and an Info icon
         * @param title
         * @param message
         */
        public void notifyInfo(final String title, final String message) {
            notify(new Message(title, message, Message.INFO_ICON));
        }

        /**
         * Show a Notification with the given title and message and a Warning icon
         * @param title
         * @param message
         */
        public void notifyWarning(final String title, final String message) {
            notify(new Message(title, message, Message.WARNING_ICON));
        }

        /**
         * Show a Notification with the given title and message and a Checkmark icon
         * @param title
         * @param message
         */
        public void notifySuccess(final String title, final String message) {
            notify(new Message(title, message, Message.SUCCESS_ICON));
        }

        /**
         * Show a Notification with the given title and message and an Error icon
         * @param title
         * @param message
         */
        public void notifyError(final String title, final String message) {
            notify(new Message(title, message, Message.ERROR_ICON));
        }

        /**
         * Creates and shows a popup with the data from the given Notification object
         * @param notification
         */
        private void showNotification(final Message notification) {
            ImageView icon = new ImageView(notification.image);
            icon.setFitWidth(ICON_WIDTH);
            icon.setFitHeight(ICON_HEIGHT);

            LocalNotificationsService.create().ifPresent(service -> {
                service.getNotifications().add(new Notification(UUID.randomUUID().toString(), notification.title, notification.message, ZonedDateTime.now(), () -> {
                    Alert alert = new Alert(AlertType.INFORMATION, "You have been notified");
                    Platform.runLater(() -> alert.showAndWait());
                }));
            });
        }
    }
}
