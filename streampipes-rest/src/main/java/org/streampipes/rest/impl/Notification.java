package org.streampipes.rest.impl;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.streampipes.model.client.messages.Notifications;
import org.streampipes.rest.annotation.GsonWithIds;
import org.streampipes.rest.api.INotification;
import org.streampipes.storage.controller.StorageManager;

@Path("/v2/users/{username}/notifications")
public class Notification extends AbstractRestInterface implements INotification {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @GsonWithIds
    @Override
    public Response getNotifications() {
        return ok(StorageManager
                .INSTANCE
                .getNotificationStorageApi()
                .getAllNotifications());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/unread")
    @Override
    public Response getUnreadNotifications() {
        return ok(StorageManager
                .INSTANCE
                .getNotificationStorageApi()
                .getUnreadNotifications());
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{notificationId}")
    @Override
    public Response deleteNotification(@PathParam("notificationId") String notificationId) {
        boolean success = StorageManager
                .INSTANCE
                .getNotificationStorageApi()
                .deleteNotification(notificationId);
        if (success) {
            return ok(Notifications.success("Notification deleted"));
        } else {
            return ok(Notifications.error("Could not delete notification"));
        }

    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{notificationId}")
    @Override
    public Response modifyNotificationStatus(@PathParam("notificationId") String notificationId) {
        boolean success = StorageManager
                .INSTANCE
                .getNotificationStorageApi()
                .changeNotificationStatus(notificationId);
        if (success) {
            return ok(Notifications.success("Ok"));
        } else {
            return ok(Notifications.error("Error"));
        }
    }
}