-- PostgreSQL: add account access to the audio entry/list; existing share links stay public.
-- Run against the website's portfolio database before deploying the updated backend.
BEGIN;
CREATE TABLE IF NOT EXISTS public.premium_listeners (
    user_id BIGINT PRIMARY KEY,
    granted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_premium_listeners_user FOREIGN KEY (user_id)
        REFERENCES public.users(id) ON DELETE CASCADE
);
COMMENT ON TABLE public.premium_listeners IS 'Accounts allowed to see the private audio entry and episode list; admins always have access';
COMMIT;

-- No usernames, passwords, roles, audio records or existing UUIDs are changed.
