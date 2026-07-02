CREATE TABLE notifications (
    id                       BIGSERIAL PRIMARY KEY,
    recipient_auth_user_id   BIGINT NOT NULL,
    type                     VARCHAR(30) NOT NULL,
    message                  VARCHAR(500) NOT NULL,
    booking_id               BIGINT,
    read                     BOOLEAN NOT NULL DEFAULT false,
    created_at               TIMESTAMP NOT NULL DEFAULT now()
);

-- Index composite : la requête la plus fréquente est "mes notifications
-- non lues", triées par date, couvre à la fois le comptage (badge) et la liste complète.
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_auth_user_id, read);
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_auth_user_id, created_at DESC);