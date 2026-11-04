-- The Web Push subscription of a mobile destination's app, if it has one: the push service's endpoint and the app's
-- keys (base64url) that the payload is encrypted for. It belongs to the device paired with the destination's key, so a
-- new key removes it.
alter table mobile_destinations
    add column push_endpoint varchar(2048),
    add column push_p256dh   varchar(100),
    add column push_auth     varchar(50),
    add constraint mobile_destinations_push_ck check (
        (push_endpoint is null and push_p256dh is null and push_auth is null)
            or (push_endpoint is not null and push_p256dh is not null and push_auth is not null));
