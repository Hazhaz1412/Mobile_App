package com.example.ok.model;

// TODO: Bổ sung các trường cho notification
public class NotificationMessage {
    private String type; // MESSAGE, OFFER, LISTING_UPDATE, PROMO
    private String title;
    private String body;
    private Long relatedId; // roomId, listingId, ...
    // ...getter/setter...
}
