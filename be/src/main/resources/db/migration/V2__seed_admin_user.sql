INSERT INTO users (id, first_name, last_name, email, password, role, created_at, updated_at)
VALUES (gen_random_uuid(),
        'Ng',
        'Ph',
        'ngph@gmail.com',
        '$2a$12$AKO5G534vP3IJIPWMRLg1eXfs8H92mkpm1qomYPDNDBYxRljLcM/2', --ngph1234
        'ADMIN',
        now(),
        now()),
       (gen_random_uuid(),
        'Ph',
        'Ng',
        'phng@gmail.com',
        '$2a$12$IB5IPy6deCfFJmb1ZBwgnuTJ.xnj0TY58qI8q4VnuHI3h/8nhGIZS', --phng1234
        'USER',
        now(),
        now())
ON CONFLICT (email) DO NOTHING;