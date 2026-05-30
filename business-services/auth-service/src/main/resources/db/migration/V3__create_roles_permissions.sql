-- V3: Create roles, permissions, and their join table
-- Roles are system-global (seeded once). Permissions map to resource:action strings.

CREATE TABLE auth_db.roles
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),

    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE auth_db.permissions
(
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,    -- e.g. inventory:read, sales:write
    description VARCHAR(255),

    CONSTRAINT uq_permissions_name UNIQUE (name)
);

CREATE TABLE auth_db.role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES auth_db.roles       (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES auth_db.permissions (id)
);

CREATE INDEX idx_role_permissions_role_id ON auth_db.role_permissions (role_id);
