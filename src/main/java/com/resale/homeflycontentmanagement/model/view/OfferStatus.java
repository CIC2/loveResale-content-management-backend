package com.resale.homeflycontentmanagement.model.view;

public enum OfferStatus {

    NO_OFFER_CREATED(0, "No Offer Created"),
    NOT_COMPLETED(1, "Not Completed"),
    COMPLETED(2, "Completed");

    private final int id;
    private final String label;

    OfferStatus(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static OfferStatus fromId(Integer id) {
        if (id == null) {
            return null;
        }

        for (OfferStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "Invalid offerStatus. Allowed values: 0=No Offer Created, 1=Not Completed, 2=Completed"
        );
    }
}


