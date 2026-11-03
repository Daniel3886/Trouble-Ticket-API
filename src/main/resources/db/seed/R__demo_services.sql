INSERT INTO services (id, tenant_id, active) VALUES
    (987654321, 'tenant-a', true),   -- matches the spec's example request
    (987654322, 'tenant-a', true),
    (987654323, 'tenant-a', false),  -- inactive: create returns 404 SERVICE_NOT_FOUND
    (123456789, 'tenant-b', true)    -- other tenant: invisible to tenant-a tokens
ON CONFLICT (id) DO NOTHING;
