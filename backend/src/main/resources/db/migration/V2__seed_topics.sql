INSERT INTO topics (name, slug, description, color_hex, is_featured)
VALUES
    ('Java',           'java',           'Core Java, JVM, concurrency',                 '#7F77DD', TRUE),
    ('Spring Boot',    'spring-boot',    'Spring Boot, Spring Framework, microservices','#1D9E75', TRUE),
    ('C# / .NET',      'csharp-dotnet',  'C# language, .NET runtime, ASP.NET Core',     '#378ADD', TRUE),
    ('AWS',            'aws',            'AWS services, architecture, certifications', '#EF9F27', TRUE),
    ('Terraform',      'terraform',      'Infrastructure as code, modules, state',      '#D85A30', FALSE),
    ('System Design',  'system-design',  'Architecture, scalability, trade-offs',       '#534AB7', TRUE),
    ('DSA',            'dsa',            'Data structures and algorithms',              '#D4537E', TRUE),
    ('SQL',            'sql',            'Queries, optimization, indexing',             '#0F6E56', FALSE),
    ('Microservices',  'microservices',  'Patterns, service communication, mesh',       '#A32D2D', FALSE),
    ('Kafka',          'kafka',          'Streaming, topics, partitions, consumers',    '#444441', FALSE)
ON CONFLICT (slug) DO NOTHING;
