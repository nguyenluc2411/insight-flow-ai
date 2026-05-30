-- V4: Create user_roles table
-- Tracks which role a user has within a specific tenant.
-- tenant_id is denormalized here for efficient tenant-scoped queries.

CREATE TABLE auth_db.user_roles
(
    user_id   UUID NOT NULL,
    role_id   UUID NOT NULL,
    tenant_id UUID NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user   FOREIGN KEY (user_id)   REFERENCES auth_db.users   (id),
    CONSTRAINT fk_ur_role   FOREIGN KEY (role_id)   REFERENCES auth_db.roles   (id),
    CONSTRAINT fk_ur_tenant FOREIGN KEY (tenant_id) REFERENCES auth_db.tenants (id)
);

CREATE INDEX idx_user_roles_user_id   ON auth_db.user_roles (user_id);
CREATE INDEX idx_user_roles_tenant_id ON auth_db.user_roles (tenant_id);
